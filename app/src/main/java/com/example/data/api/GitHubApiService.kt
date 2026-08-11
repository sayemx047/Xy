package com.example.data.api

import com.example.data.api.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepositories(
        @Header("Authorization") authHeader: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Response<List<GitHubRepo>>

    @POST("user/repos")
    suspend fun createRepository(
        @Header("Authorization") authHeader: String,
        @Body request: CreateRepoRequest
    ): Response<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepo>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: CreateOrUpdateFileRequest
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun getWorkflows(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<WorkflowsResponse>

    @GET("repos/{owner}/{repo}/git/commits/{commit_sha}")
    suspend fun getCommitDetails(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("commit_sha") commitSha: String
    ): Response<GitCommitDetails>

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body request: WorkflowDispatchRequest
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("branch") branch: String? = null,
        @Query("event") event: String? = null,
        @Query("head_sha") headSha: String? = null,
        @Query("per_page") perPage: Int = 10
    ): Response<WorkflowRunsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<WorkflowRun>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getRunArtifacts(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<ArtifactsResponse>

    @Streaming
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
    suspend fun downloadArtifactZip(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): Response<ResponseBody>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelWorkflowRun(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): Response<GitHubContentResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getRunJobs(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<JobsResponse>

    @Streaming
    @GET("repos/{owner}/{repo}/actions/jobs/{job_id}/logs")
    suspend fun downloadJobLogs(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("job_id") jobId: Long
    ): Response<ResponseBody>

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateBlobRequest
    ): Response<CreateBlobResponse>

    // Git Data API for fast commit & batch file uploads
    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<GitReference>

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateTreeRequest
    ): Response<GitTreeResponse>

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateCommitRequest
    ): Response<CreateCommitResponse>

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateBranchRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: UpdateRefRequest
    ): Response<GitReference>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateRefRequest
    ): Response<GitReference>
}

interface GitHubAuthService {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("https://github.com/login/oauth/access_token")
    suspend fun exchangeOAuthCode(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String?,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String?
    ): Response<OAuthTokenResponse>

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("https://github.com/login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "repo workflow"
    ): Response<DeviceCodeResponse>

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("https://github.com/login/oauth/access_token")
    suspend fun pollDeviceToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<OAuthTokenResponse>
}
