package com.example.data.engine

import com.example.data.api.models.GitHubWorkflow
import com.example.data.api.models.WorkflowRun
import java.io.File

interface BuildEngine {
    val engineName: String

    suspend fun findWorkflows(token: String, owner: String, repo: String): Result<List<GitHubWorkflow>>

    suspend fun ensureWorkflowExists(
        token: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<GitHubWorkflow>

    suspend fun findOrTriggerRun(
        token: String,
        owner: String,
        repo: String,
        workflowFileNameOrId: String,
        headSha: String?,
        branch: String = "main",
        dispatchStartTimeMillis: Long,
        buildType: String = "debug"
    ): Result<Long>

    suspend fun pollBuildStatus(token: String, owner: String, repo: String, runId: Long): Result<WorkflowRun>

    suspend fun getBuildLogs(token: String, owner: String, repo: String, runId: Long): Result<String>

    suspend fun cancelBuild(token: String, owner: String, repo: String, runId: Long): Result<Boolean>

    suspend fun downloadApkArtifact(token: String, owner: String, repo: String, runId: Long, destDir: File): Result<File>
}
