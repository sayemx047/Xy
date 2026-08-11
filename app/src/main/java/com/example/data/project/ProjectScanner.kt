package com.example.data.project

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ProjectScanner(private val context: Context) {

    private val ignoredPaths = setOf(
        ".gradle", "build", ".idea", "local.properties", ".git", ".DS_Store"
    )

    private val ignoredExtensions = setOf(
        "jks", "keystore", "apk", "aab"
    )

    suspend fun scanFolder(treeUri: Uri): ProjectInfo = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ProjectInfo(
                projectName = "Invalid Folder",
                rootUri = treeUri,
                isValid = false,
                validationMessages = listOf("Unable to access directory via Storage Access Framework.")
            )

        val projectName = rootDoc.name ?: "AndroidProject"

        val fileList = mutableListOf<DocumentFileWrapper>()
        traverseDocumentFile(rootDoc, "", fileList)

        processProjectFiles(projectName, treeUri, null, fileList)
    }

    suspend fun scanZip(zipUri: Uri): ProjectInfo = withContext(Dispatchers.IO) {
        val zipName = getFileName(zipUri) ?: "ImportedProject.zip"
        val cleanName = zipName.replace(".zip", "", ignoreCase = true)

        val extractDir = File(context.cacheDir, "extracted_projects/${cleanName}_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        val extractedRoot = extractZipFile(zipUri, extractDir)
            ?: return@withContext ProjectInfo(
                projectName = cleanName,
                rootUri = zipUri,
                isValid = false,
                validationMessages = listOf("Failed to extract or read ZIP archive.")
            )

        // Find actual Android project root inside extracted directory
        val actualProjectRoot = findProjectRootInFile(extractedRoot)

        val fileList = mutableListOf<DocumentFileWrapper>()
        traverseFileDirectory(actualProjectRoot, "", fileList)

        processProjectFiles(actualProjectRoot.name, zipUri, actualProjectRoot.absolutePath, fileList)
    }

    private fun processProjectFiles(
        projectName: String,
        uri: Uri,
        localPath: String?,
        files: List<DocumentFileWrapper>
    ): ProjectInfo {
        var hasSettingsGradle = false
        var hasSettingsGradleKts = false
        var hasAppBuildGradle = false
        var hasAppBuildGradleKts = false
        var hasDotEnv = false
        var hasDotEnvExample = false
        var hasBuildGradle = false
        var hasGradlew = false
        var hasWrapperJar = false
        var hasWrapperProps = false
        var hasAndroidManifest = false
        var gradleDslType = "Kotlin DSL (.kts)"
        var compileSdk = "35"
        var minSdk = "24"
        var targetSdk = "35"

        val moduleNames = mutableSetOf<String>()
        val messages = mutableListOf<String>()

        var totalCount = 0
        var totalSize = 0L

        for (wrapper in files) {
            val relPath = wrapper.relativePath.replace("\\", "/")
            val fileName = wrapper.name

            if (isIgnored(relPath, fileName)) continue

            totalCount++
            totalSize += wrapper.length

            if (fileName == ".env") {
                hasDotEnv = true
            }
            if (fileName == ".env.example") {
                hasDotEnvExample = true
            }

            if (fileName == "settings.gradle.kts") {
                hasSettingsGradleKts = true
                gradleDslType = "Kotlin DSL (.kts)"
            } else if (fileName == "settings.gradle") {
                hasSettingsGradle = true
                gradleDslType = "Groovy DSL (.gradle)"
            }

            if (fileName == "build.gradle.kts" || fileName == "build.gradle") {
                hasBuildGradle = true
                if (fileName == "build.gradle.kts" && (relPath.startsWith("app/") || relPath == "build.gradle.kts")) {
                    hasAppBuildGradleKts = true
                }
                if (fileName == "build.gradle" && (relPath.startsWith("app/") || relPath == "build.gradle")) {
                    hasAppBuildGradle = true
                }

                // Check if this is inside a module folder like app/build.gradle.kts
                val parts = relPath.split("/")
                if (parts.size >= 2) {
                    val candidateModule = parts[0]
                    if (candidateModule != ".github" && candidateModule != "build" && candidateModule != "gradle") {
                        moduleNames.add(candidateModule)
                    }
                }

                // Parse SDK versions from build.gradle content if possible
                val content = wrapper.readText(context)
                if (content.contains("compileSdk")) {
                    compileSdk = extractVersionValue(content, "compileSdk") ?: compileSdk
                }
                if (content.contains("minSdk")) {
                    minSdk = extractVersionValue(content, "minSdk") ?: minSdk
                }
                if (content.contains("targetSdk")) {
                    targetSdk = extractVersionValue(content, "targetSdk") ?: targetSdk
                }
            }

            if (fileName == "gradlew") {
                hasGradlew = true
            }

            if (fileName.equals("gradle-wrapper.jar", ignoreCase = true) || relPath.endsWith("gradle/wrapper/gradle-wrapper.jar", ignoreCase = true)) {
                hasWrapperJar = true
            }

            if (fileName.equals("gradle-wrapper.properties", ignoreCase = true) || relPath.endsWith("gradle/wrapper/gradle-wrapper.properties", ignoreCase = true)) {
                hasWrapperProps = true
            }

            if (fileName == "AndroidManifest.xml") {
                hasAndroidManifest = true
            }
        }

        if (moduleNames.isEmpty()) {
            moduleNames.add("app")
        }

        val primaryModule = if (moduleNames.contains("app")) "app" else moduleNames.first()

        val isValid = (hasSettingsGradle || hasSettingsGradleKts || hasBuildGradle) && files.isNotEmpty() && hasAndroidManifest

        if (!hasSettingsGradle && !hasSettingsGradleKts && !hasBuildGradle) {
            messages.add("No settings.gradle or build.gradle found. Selected folder may not be a valid Gradle project.")
        }

        if (!hasGradlew || !hasWrapperJar || !hasWrapperProps) {
            messages.add("Gradle Wrapper missing — remote Gradle fallback will be used.")
        }

        if (!hasAndroidManifest) {
            messages.add("AndroidManifest.xml not found in primary module.")
        }

        return ProjectInfo(
            projectName = projectName,
            rootUri = uri,
            localDirectoryPath = localPath,
            moduleNames = moduleNames.toList(),
            primaryModule = primaryModule,
            compileSdk = compileSdk,
            minSdk = minSdk,
            targetSdk = targetSdk,
            hasGradleWrapper = hasGradlew,
            hasAndroidManifest = hasAndroidManifest,
            gradleDslType = gradleDslType,
            hasDotEnv = hasDotEnv,
            hasDotEnvExample = hasDotEnvExample,
            hasSettingsGradle = hasSettingsGradle,
            hasSettingsGradleKts = hasSettingsGradleKts,
            hasAppBuildGradle = hasAppBuildGradle,
            hasAppBuildGradleKts = hasAppBuildGradleKts,
            hasWrapperJar = hasWrapperJar,
            isValid = isValid,
            validationMessages = messages,
            totalFilesCount = totalCount,
            totalSizeInBytes = totalSize
        )
    }

    private fun extractVersionValue(content: String, key: String): String? {
        val regex = Regex("""$key\s*=?\s*([0-9]+)""")
        return regex.find(content)?.groupValues?.get(1)
    }

    private fun isIgnored(relPath: String, fileName: String): Boolean {
        if (fileName.equals("gradle-wrapper.jar", ignoreCase = true) || relPath.endsWith("gradle/wrapper/gradle-wrapper.jar", ignoreCase = true)) {
            return false
        }
        val parts = relPath.split("/")
        for (part in parts) {
            if (ignoredPaths.contains(part)) return true
        }
        val ext = fileName.substringAfterLast(".", "").lowercase()
        if (ignoredExtensions.contains(ext)) return true
        return false
    }

    private fun traverseDocumentFile(
        dir: DocumentFile,
        currentPath: String,
        result: MutableList<DocumentFileWrapper>
    ) {
        val files = dir.listFiles()
        for (f in files) {
            val name = f.name ?: continue
            val relPath = if (currentPath.isEmpty()) name else "$currentPath/$name"

            if (f.isDirectory) {
                if (!ignoredPaths.contains(name)) {
                    traverseDocumentFile(f, relPath, result)
                }
            } else {
                result.add(DocumentFileWrapper.FromDocument(f, relPath, name))
            }
        }
    }

    private fun traverseFileDirectory(
        dir: File,
        currentPath: String,
        result: MutableList<DocumentFileWrapper>
    ) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            val name = f.name
            val relPath = if (currentPath.isEmpty()) name else "$currentPath/$name"

            if (f.isDirectory) {
                if (!ignoredPaths.contains(name)) {
                    traverseFileDirectory(f, relPath, result)
                }
            } else {
                result.add(DocumentFileWrapper.FromLocalFile(f, relPath, name))
            }
        }
    }

    private fun findProjectRootInFile(dir: File): File {
        if (File(dir, "settings.gradle.kts").exists() || File(dir, "settings.gradle").exists() || File(dir, "build.gradle.kts").exists()) {
            return dir
        }
        val subDirs = dir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        for (sub in subDirs) {
            if (File(sub, "settings.gradle.kts").exists() || File(sub, "settings.gradle").exists() || File(sub, "build.gradle.kts").exists()) {
                return sub
            }
        }
        return dir
    }

    private fun extractZipFile(zipUri: Uri, destDir: File): File? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(zipUri) ?: return null
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                val newFile = File(destDir, entry.name)
                val canonicalDest = destDir.canonicalPath
                val canonicalFile = newFile.canonicalPath
                if (!canonicalFile.startsWith(canonicalDest + File.separator) && canonicalFile != canonicalDest) {
                    throw SecurityException("Zip Slip vulnerability attempt blocked: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zipInputStream.copyTo(fos)
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            destDir
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex("_display_name")
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    sealed class DocumentFileWrapper {
        abstract val relativePath: String
        abstract val name: String
        abstract val length: Long
        abstract fun readBytes(context: Context): ByteArray
        abstract fun readText(context: Context): String

        data class FromDocument(
            val doc: DocumentFile,
            override val relativePath: String,
            override val name: String
        ) : DocumentFileWrapper() {
            override val length: Long get() = doc.length()
            override fun readBytes(context: Context): ByteArray {
                return context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() } ?: ByteArray(0)
            }
            override fun readText(context: Context): String {
                return readBytes(context).toString(Charsets.UTF_8)
            }
        }

        data class FromLocalFile(
            val file: File,
            override val relativePath: String,
            override val name: String
        ) : DocumentFileWrapper() {
            override val length: Long get() = file.length()
            override fun readBytes(context: Context): ByteArray {
                return file.readBytes()
            }
            override fun readText(context: Context): String {
                return file.readText(Charsets.UTF_8)
            }
        }
    }
}
