package com.example.data.api

import com.example.data.api.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * High-level Retrofit-backed client service abstracting the GitHub REST API.
 * Provides abstracted methods for Authentication, Repository Management,
 * Git Data commits, and Actions Workflow Dispatching.
 */
class GitHubApiClient(
    val apiService: GitHubApiService,
    val authService: GitHubAuthService
) {

    companion object {
        const val GITHUB_API_BASE_URL = "https://api.github.com/"
        const val GITHUB_AUTH_BASE_URL = "https://github.com/"

        fun create(): GitHubApiClient {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            val apiRetrofit = Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val authRetrofit = Retrofit.Builder()
                .baseUrl(GITHUB_AUTH_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return GitHubApiClient(
                apiService = apiRetrofit.create(GitHubApiService::class.java),
                authService = authRetrofit.create(GitHubAuthService::class.java)
            )
        }
    }

    private fun formatAuthHeader(token: String): String {
        val trimmed = token.trim()
        return if (trimmed.startsWith("Bearer ", ignoreCase = true) || trimmed.startsWith("token ", ignoreCase = true)) {
            trimmed
        } else {
            "Bearer $trimmed"
        }
    }

    // ============================================================================
    // 1. AUTHENTICATION & USER MANAGEMENT
    // ============================================================================

    suspend fun getAuthenticatedUser(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAuthenticatedUser(formatAuthHeader(token))
            handleResponse(response) { user -> user }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToken(token: String): Result<GitHubUser> {
        return getAuthenticatedUser(token)
    }

    suspend fun exchangeOAuthCode(
        clientId: String,
        clientSecret: String?,
        code: String,
        redirectUri: String? = null
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authService.exchangeOAuthCode(clientId, clientSecret, code, redirectUri)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestDeviceCode(
        clientId: String,
        scope: String = "repo workflow"
    ): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authService.requestDeviceCode(clientId, scope)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollDeviceToken(
        clientId: String,
        deviceCode: String
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authService.pollDeviceToken(clientId, deviceCode)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================================
    // 2. REPOSITORY MANAGEMENT & FILE OPERATIONS
    // ============================================================================

    suspend fun getUserRepositories(
        token: String,
        sort: String = "updated",
        perPage: Int = 100
    ): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserRepositories(formatAuthHeader(token), sort, perPage)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepository(
        token: String,
        owner: String,
        repo: String
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRepository(formatAuthHeader(token), owner, repo)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepository(
        token: String,
        name: String,
        description: String = "Automated Android APK build workspace created by Native APK Builder",
        isPrivate: Boolean = true,
        autoInit: Boolean = true
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val request = CreateRepoRequest(
                name = name,
                description = description,
                isPrivate = isPrivate,
                autoInit = autoInit
            )
            val response = apiService.createRepository(formatAuthHeader(token), request)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        contentBase64: String,
        commitMessage: String,
        branch: String? = null,
        sha: String? = null
    ): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val request = CreateOrUpdateFileRequest(
                message = commitMessage,
                contentBase64 = contentBase64,
                branch = branch,
                sha = sha
            )
            val response = apiService.createOrUpdateFile(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                path = path,
                request = request
            )
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitAndPushFiles(
        token: String,
        owner: String,
        repo: String,
        branch: String = "main",
        commitMessage: String,
        items: List<GitTreeItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val auth = formatAuthHeader(token)
            
            // 1. Get branch reference (head commit)
            val refResponse = apiService.getBranchRef(auth, owner, repo, branch)
            if (!refResponse.isSuccessful || refResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to get branch ref for $branch: ${refResponse.code()}"))
            }
            val parentCommitSha = refResponse.body()!!.gitObject.sha

            // 2. Create Git Tree
            val treeResponse = apiService.createTree(
                auth, owner, repo,
                CreateTreeRequest(baseTreeSha = parentCommitSha, tree = items)
            )
            if (!treeResponse.isSuccessful || treeResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to create Git tree: ${treeResponse.code()}"))
            }
            val newTreeSha = treeResponse.body()!!.sha

            // 3. Create Commit
            val commitResponse = apiService.createCommit(
                auth, owner, repo,
                CreateCommitRequest(
                    message = commitMessage,
                    treeSha = newTreeSha,
                    parentShas = listOf(parentCommitSha)
                )
            )
            if (!commitResponse.isSuccessful || commitResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to create commit: ${commitResponse.code()}"))
            }
            val newCommitSha = commitResponse.body()!!.sha

            // 4. Update Reference
            val updateRefResponse = apiService.updateBranchRef(
                auth, owner, repo, branch,
                UpdateRefRequest(sha = newCommitSha, force = true)
            )
            if (!updateRefResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to update branch ref: ${updateRefResponse.code()}"))
            }

            Result.success(newCommitSha)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================================
    // 3. WORKFLOW DISPATCHING & CI/CD ACTIONS
    // ============================================================================

    suspend fun dispatchWorkflow(
        token: String,
        owner: String,
        repo: String,
        workflowId: String,
        ref: String = "main",
        inputs: Map<String, String> = emptyMap()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = WorkflowDispatchRequest(ref = ref, inputs = inputs)
            val response = apiService.dispatchWorkflow(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                workflowId = workflowId,
                request = request
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Failed to dispatch workflow: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRuns(
        token: String,
        owner: String,
        repo: String,
        event: String? = "workflow_dispatch",
        perPage: Int = 10
    ): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWorkflowRuns(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                event = event,
                perPage = perPage
            )
            handleResponse(response) { it.workflowRuns }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRun(
        token: String,
        owner: String,
        repo: String,
        runId: Long
    ): Result<WorkflowRun> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWorkflowRun(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                runId = runId
            )
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRunArtifacts(
        token: String,
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<Artifact>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRunArtifacts(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                runId = runId
            )
            handleResponse(response) { it.artifacts }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadArtifactZip(
        token: String,
        owner: String,
        repo: String,
        artifactId: Long
    ): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadArtifactZip(
                authHeader = formatAuthHeader(token),
                owner = owner,
                repo = repo,
                artifactId = artifactId
            )
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper method to wrap Retrofit Response<T> into Result<R>
    private fun <T, R> handleResponse(response: Response<T>, transform: (T) -> R): Result<R> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(transform(body))
            } else {
                Result.failure(Exception("Empty response body (HTTP ${response.code()})"))
            }
        } else {
            val errorString = response.errorBody()?.string()
            val message = if (!errorString.isNull_or_Empty()) errorString else "HTTP Error ${response.code()}"
            Result.failure(Exception(message))
        }
    }

    private fun String?.isNull_or_Empty(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
