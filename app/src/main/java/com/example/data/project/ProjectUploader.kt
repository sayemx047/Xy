package com.example.data.project

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.example.data.api.GitHubApiService
import com.example.data.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectUploader(private val context: Context, private val apiService: GitHubApiService) {

    suspend fun uploadProjectToRepo(
        token: String,
        owner: String,
        repo: String,
        branch: String = "main",
        projectInfo: ProjectInfo,
        onProgress: (step: String, percent: Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $token"

        try {
            onProgress("Scanning project files...", 0.1f)
            val filesToUpload = collectProjectFiles(projectInfo).toMutableList()

            if (filesToUpload.isEmpty()) {
                return@withContext Result.failure(Exception("No buildable files found in project."))
            }

            // Ensure .github/workflows/android.yml is included if not present
            if (filesToUpload.none { it.relativePath.equals(".github/workflows/android.yml", ignoreCase = true) }) {
                val defaultWorkflowContent = getStandardAndroidWorkflowYaml(branch)
                filesToUpload.add(
                    ProjectFileItem(
                        relativePath = ".github/workflows/android.yml",
                        contentBytes = defaultWorkflowContent.toByteArray(Charsets.UTF_8),
                        isExecutable = false
                    )
                )
            }

            onProgress("Preparing commit payload (${filesToUpload.size} files)...", 0.3f)

            // Try fetching branch reference or default branch reference for parent commit
            var parentCommitSha: String? = null
            val branchRefResp = apiService.getBranchRef(authHeader, owner, repo, branch)

            if (branchRefResp.isSuccessful && branchRefResp.body() != null) {
                parentCommitSha = branchRefResp.body()!!.`gitObject`.sha
            } else {
                // Try fallback to main or master
                val mainRefResp = apiService.getBranchRef(authHeader, owner, repo, "main")
                if (mainRefResp.isSuccessful && mainRefResp.body() != null) {
                    parentCommitSha = mainRefResp.body()!!.`gitObject`.sha
                } else {
                    val masterRefResp = apiService.getBranchRef(authHeader, owner, repo, "master")
                    if (masterRefResp.isSuccessful && masterRefResp.body() != null) {
                        parentCommitSha = masterRefResp.body()!!.`gitObject`.sha
                    }
                }
            }

            // Build tree items
            val treeItems = mutableListOf<GitTreeItem>()
            var count = 0
            for (item in filesToUpload) {
                count++
                onProgress("Processing file $count/${filesToUpload.size}: ${item.relativePath}", 0.3f + 0.3f * (count.toFloat() / filesToUpload.size))

                val mode = if (item.isExecutable || item.relativePath.endsWith("gradlew")) "100755" else "100644"

                if (isBinaryFile(item.relativePath, item.contentBytes)) {
                    // Binary file: use GitHub Git Blobs API (base64)
                    val base64Str = Base64.encodeToString(item.contentBytes, Base64.NO_WRAP)
                    val blobResp = apiService.createBlob(
                        authHeader, owner, repo,
                        CreateBlobRequest(content = base64Str, encoding = "base64")
                    )

                    if (blobResp.isSuccessful && blobResp.body() != null) {
                        val blobSha = blobResp.body()!!.sha
                        treeItems.add(
                            GitTreeItem(
                                path = item.relativePath,
                                mode = mode,
                                type = "blob",
                                sha = blobSha
                            )
                        )
                    } else {
                        val err = blobResp.errorBody()?.string() ?: "Blob creation failed HTTP ${blobResp.code()}"
                        return@withContext Result.failure(Exception("Failed to upload binary file (${item.relativePath}): $err"))
                    }
                } else {
                    // Text file: send UTF-8 content directly in Tree
                    val contentStr = String(item.contentBytes, Charsets.UTF_8)
                    treeItems.add(
                        GitTreeItem(
                            path = item.relativePath,
                            mode = mode,
                            type = "blob",
                            content = contentStr
                        )
                    )
                }
            }

            onProgress("Creating Git Tree on GitHub...", 0.7f)
            // CRITICAL: baseTreeSha MUST be null so GitHub creates a clean tree without stale files from previous commits!
            val createTreeResp = apiService.createTree(
                authHeader, owner, repo,
                CreateTreeRequest(baseTreeSha = null, tree = treeItems)
            )

            if (!createTreeResp.isSuccessful || createTreeResp.body() == null) {
                val err = createTreeResp.errorBody()?.string() ?: "Tree creation failed HTTP ${createTreeResp.code()}"
                return@withContext Result.failure(Exception("GitHub Tree API Error: $err"))
            }

            val newTreeSha = createTreeResp.body()!!.sha

            onProgress("Creating Commit...", 0.85f)
            val parentShas = if (parentCommitSha != null) listOf(parentCommitSha) else emptyList()
            val commitResp = apiService.createCommit(
                authHeader, owner, repo,
                CreateCommitRequest(
                    message = "Clean build commit for ${projectInfo.projectName} (${System.currentTimeMillis()})",
                    treeSha = newTreeSha,
                    parentShas = parentShas
                )
            )

            if (!commitResp.isSuccessful || commitResp.body() == null) {
                val err = commitResp.errorBody()?.string() ?: "Commit creation failed HTTP ${commitResp.code()}"
                return@withContext Result.failure(Exception("GitHub Commit API Error: $err"))
            }

            val newCommitSha = commitResp.body()!!.sha

            onProgress("Updating repository branch reference ($branch)...", 0.95f)
            val updateRefResp = apiService.updateBranchRef(
                authHeader, owner, repo, branch,
                UpdateRefRequest(sha = newCommitSha, force = true)
            )

            if (!updateRefResp.isSuccessful) {
                // Branch might not exist yet, try creating ref
                val createRefResp = apiService.createRef(
                    authHeader, owner, repo,
                    CreateRefRequest(ref = "refs/heads/$branch", sha = newCommitSha)
                )

                if (!createRefResp.isSuccessful) {
                    val err = updateRefResp.errorBody()?.string() ?: createRefResp.errorBody()?.string() ?: "Branch update/creation failed"
                    return@withContext Result.failure(Exception("GitHub Ref Update Error: $err"))
                }
            }

            onProgress("Project successfully uploaded to GitHub!", 1.0f)
            return@withContext Result.success(newCommitSha)

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private fun collectProjectFiles(projectInfo: ProjectInfo): List<ProjectFileItem> {
        val result = mutableListOf<ProjectFileItem>()

        if (projectInfo.localDirectoryPath != null) {
            val rootDir = File(projectInfo.localDirectoryPath)
            if (rootDir.exists()) {
                collectLocalFiles(rootDir, rootDir, result)
            }
        } else if (projectInfo.rootUri != null) {
            collectSafFiles(projectInfo.rootUri, result)
        }

        return result
    }

    private fun collectLocalFiles(rootDir: File, current: File, result: MutableList<ProjectFileItem>) {
        val files = current.listFiles() ?: return
        for (f in files) {
            val name = f.name
            val relPath = f.relativeTo(rootDir).path.replace("\\", "/")
            if (isIgnoredFile(name, relPath, f.isDirectory)) continue

            if (f.isDirectory) {
                collectLocalFiles(rootDir, f, result)
            } else {
                val isExec = name == "gradlew" || f.canExecute()
                result.add(ProjectFileItem(relPath, f.readBytes(), isExec))
            }
        }
    }

    private fun collectSafFiles(rootUri: Uri, result: MutableList<ProjectFileItem>) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return
        traverseSafDocument(rootDoc, "", result)
    }

    private fun traverseSafDocument(
        dir: DocumentFile,
        currentPath: String,
        result: MutableList<ProjectFileItem>
    ) {
        val files = dir.listFiles()
        for (f in files) {
            val name = f.name ?: continue
            val relPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
            if (isIgnoredFile(name, relPath, f.isDirectory)) continue

            if (f.isDirectory) {
                traverseSafDocument(f, relPath, result)
            } else {
                val bytes = context.contentResolver.openInputStream(f.uri)?.use { it.readBytes() } ?: continue
                val isExec = name == "gradlew"
                result.add(ProjectFileItem(relPath, bytes, isExec))
            }
        }
    }

    private fun isIgnoredFile(name: String, relativePath: String, isDir: Boolean): Boolean {
        val ignoredDirs = setOf(".gradle", "build", ".idea", ".git", "captures", ".externalNativeBuild")
        val ignoredFiles = setOf("local.properties", ".DS_Store", "thumbs.db")
        val ignoredExtensions = setOf("jks", "keystore", "apk", "aab")

        if (isDir) {
            return ignoredDirs.contains(name)
        }

        // Never ignore .env, .env.example, gradle-wrapper.jar, gradlew, gradlew.bat, or gradle-wrapper.properties
        if (name == ".env" || name == ".env.example") {
            return false
        }
        if (name.equals("gradle-wrapper.jar", ignoreCase = true) || relativePath.endsWith("gradle/wrapper/gradle-wrapper.jar", ignoreCase = true)) {
            return false
        }
        if (name == "gradlew" || name == "gradlew.bat" || name == "gradle-wrapper.properties") {
            return false
        }

        if (ignoredFiles.contains(name)) return true

        val ext = name.substringAfterLast(".", "").lowercase()
        if (ignoredExtensions.contains(ext)) return true

        return false
    }

    private fun isBinaryFile(relativePath: String, bytes: ByteArray): Boolean {
        val ext = relativePath.substringAfterLast(".", "").lowercase()
        val binaryExtensions = setOf(
            "jar", "png", "jpg", "jpeg", "webp", "gif", "ico", "so", "aar", "class", 
            "zip", "gz", "tar", "ttf", "otf", "eot", "woff", "woff2", "pb", "db", "sqlite"
        )
        if (binaryExtensions.contains(ext)) return true

        // Check first 1000 bytes for null bytes
        val sampleSize = minOf(bytes.size, 1000)
        for (i in 0 until sampleSize) {
            if (bytes[i] == 0.toByte()) {
                return true
            }
        }
        return false
    }

    private fun getStandardAndroidWorkflowYaml(branch: String): String {
        return """
name: Build Android APK

on:
  push:
    branches: ["$branch"]
  workflow_dispatch:
    inputs:
      build_type:
        description: 'Build Type (debug or release)'
        required: false
        default: 'debug'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Find Gradle wrapper
        shell: bash
        run: |
          set -e

          GRADLEW="${'$'}(find . -type f -name gradlew -print -quit)"

          if [ -z "${'$'}GRADLEW" ]; then
            echo "::error::Gradle wrapper not found"
            exit 1
          fi

          chmod +x "${'$'}GRADLEW"

          PROJECT_DIR="${'$'}(dirname "${'$'}GRADLEW")"

          echo "GRADLEW=${'$'}GRADLEW" >> "${'$'}GITHUB_ENV"
          echo "PROJECT_DIR=${'$'}PROJECT_DIR" >> "${'$'}GITHUB_ENV"

      - name: Build Debug APK
        if: ${'$'}{{ github.event.inputs.build_type == 'debug' || github.event.inputs.build_type == '' || github.event_name == 'push' }}
        shell: bash
        run: |
          set -e
          echo "Using Gradle wrapper: ${'$'}GRADLEW"
          cd "${'$'}PROJECT_DIR"
          "${'$'}GRADLEW" --no-daemon --stacktrace --warning-mode all assembleDebug

      - name: Build Release APK
        if: ${'$'}{{ github.event.inputs.build_type == 'release' }}
        shell: bash
        run: |
          set -e
          echo "Using Gradle wrapper: ${'$'}GRADLEW"
          cd "${'$'}PROJECT_DIR"
          "${'$'}GRADLEW" --no-daemon --stacktrace --warning-mode all assembleRelease

      - name: Upload APKs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: android-apks
          path: |
            **/build/outputs/apk/**/*.apk
            **/build/outputs/apk/**/*.aab
          if-no-files-found: warn
""".trimIndent()
    }
}
