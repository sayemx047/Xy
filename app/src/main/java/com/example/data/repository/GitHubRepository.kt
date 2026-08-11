package com.example.data.repository

import com.example.data.api.GitHubApiService
import com.example.data.api.models.CreateRepoRequest
import com.example.data.api.models.GitHubRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubRepository(private val apiService: GitHubApiService) {

    suspend fun listUserRepositories(token: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getUserRepositories("Bearer $token")
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch repositories: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPrivateBuildRepository(token: String, repoName: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val req = CreateRepoRequest(
                name = repoName,
                description = "Automated Android APK build repository for Native APK Builder",
                isPrivate = true,
                autoInit = true
            )
            val resp = apiService.createRepository("Bearer $token", req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception("Failed to create build repository: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepository(token: String, owner: String, repo: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.getRepository("Bearer $token", owner, repo)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Repository not found or access denied: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
