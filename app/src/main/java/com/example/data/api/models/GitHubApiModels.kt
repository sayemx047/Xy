package com.example.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    @Json(name = "login") val login: String,
    @Json(name = "id") val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "private") val isPrivate: Boolean,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "default_branch") val defaultBranch: String? = "main",
    @Json(name = "description") val description: String? = null,
    @Json(name = "owner") val owner: GitHubUser? = null
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "Automated Android APK build workspace created by Native APK Builder",
    @Json(name = "private") val isPrivate: Boolean = true,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateOrUpdateFileRequest(
    @Json(name = "message") val message: String,
    @Json(name = "content") val contentBase64: String,
    @Json(name = "branch") val branch: String? = null,
    @Json(name = "sha") val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequest(
    @Json(name = "ref") val ref: String,
    @Json(name = "inputs") val inputs: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class WorkflowsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflows") val workflows: List<GitHubWorkflow>
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflow(
    @Json(name = "id") val id: Long,
    @Json(name = "node_id") val nodeId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "state") val state: String,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "badge_url") val badgeUrl: String? = null
) {
    val fileName: String get() = path.substringAfterLast("/")
}

@JsonClass(generateAdapter = true)
data class GitTreeReference(
    @Json(name = "sha") val sha: String,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitCommitDetails(
    @Json(name = "sha") val sha: String,
    @Json(name = "tree") val tree: GitTreeReference
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun>
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    @Json(name = "event") val event: String? = null,
    @Json(name = "workflow_id") val workflowId: Long? = null,
    @Json(name = "status") val status: String, // "queued", "in_progress", "completed"
    @Json(name = "conclusion") val conclusion: String? = null, // "success", "failure", "cancelled", "timed_out"
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "logs_url") val logsUrl: String? = null,
    @Json(name = "artifacts_url") val artifactsUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "artifacts") val artifacts: List<Artifact>
)

@JsonClass(generateAdapter = true)
data class Artifact(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    @Json(name = "expired") val expired: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OAuthTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class GitReference(
    @Json(name = "ref") val ref: String,
    @Json(name = "object") val gitObject: GitObject
)

@JsonClass(generateAdapter = true)
data class GitObject(
    @Json(name = "sha") val sha: String,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    @Json(name = "sha") val sha: String,
    @Json(name = "tree") val tree: List<GitTreeItem>
)

@JsonClass(generateAdapter = true)
data class GitTreeItem(
    @Json(name = "path") val path: String,
    @Json(name = "mode") val mode: String = "100644", // "100644" for file, "100755" for executable
    @Json(name = "type") val type: String = "blob",
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    @Json(name = "base_tree") val baseTreeSha: String? = null,
    @Json(name = "tree") val tree: List<GitTreeItem>
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    @Json(name = "message") val message: String,
    @Json(name = "tree") val treeSha: String,
    @Json(name = "parents") val parentShas: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
    @Json(name = "sha") val sha: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    @Json(name = "sha") val sha: String,
    @Json(name = "force") val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    @Json(name = "ref") val ref: String,
    @Json(name = "sha") val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
    @Json(name = "content") val content: String,
    @Json(name = "encoding") val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
    @Json(name = "sha") val sha: String,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubContentResponse(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "sha") val sha: String,
    @Json(name = "size") val size: Long,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class JobsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "jobs") val jobs: List<WorkflowJob>
)

@JsonClass(generateAdapter = true)
data class WorkflowJob(
    @Json(name = "id") val id: Long,
    @Json(name = "run_id") val runId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String,
    @Json(name = "conclusion") val conclusion: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "steps") val steps: List<JobStep>? = null
)

@JsonClass(generateAdapter = true)
data class JobStep(
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String,
    @Json(name = "conclusion") val conclusion: String? = null,
    @Json(name = "number") val number: Int
)

@JsonClass(generateAdapter = true)
data class DeviceCodeResponse(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_uri") val verificationUri: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "interval") val interval: Int
)
