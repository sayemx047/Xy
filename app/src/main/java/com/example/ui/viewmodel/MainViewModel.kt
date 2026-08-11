package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.GitHubRepo
import com.example.data.api.models.GitHubWorkflow
import com.example.data.api.models.WorkflowRun
import com.example.data.auth.AuthState
import com.example.data.auth.GitHubAuthManager
import com.example.data.db.AppDatabase
import com.example.data.db.BuildHistoryEntity
import com.example.data.engine.GitHubActionsBuildEngine
import com.example.data.installer.ApkInstaller
import com.example.data.project.ProjectInfo
import com.example.data.project.ProjectScanner
import com.example.data.project.ProjectUploader
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

sealed class ActiveBuildState {
    object Idle : ActiveBuildState()
    data class UploadingProject(val stepMessage: String, val progress: Float) : ActiveBuildState()
    object TriggeringWorkflow : ActiveBuildState()
    data class PollingStatus(val runId: Long, val status: String, val conclusion: String?) : ActiveBuildState()
    data class DownloadingArtifact(val runId: Long) : ActiveBuildState()
    data class Success(val runId: Long, val apkFile: File, val artifactName: String) : ActiveBuildState()
    data class Failed(val runId: Long?, val errorMessage: String, val logOutput: String? = null) : ActiveBuildState()
}

data class UiState(
    val authState: AuthState = AuthState.Unauthenticated,
    val selectedProject: ProjectInfo? = null,
    val isScanningProject: Boolean = false,
    val userRepositories: List<GitHubRepo> = emptyList(),
    val isLoadingRepos: Boolean = false,
    val selectedRepository: GitHubRepo? = null,
    val discoveredWorkflows: List<GitHubWorkflow> = emptyList(),
    val selectedWorkflow: GitHubWorkflow? = null,
    val isLoadingWorkflows: Boolean = false,
    val workflowErrorMessage: String? = null,
    val buildType: String = "debug", // "debug" or "release"
    val activeBuildState: ActiveBuildState = ActiveBuildState.Idle,
    val buildLogsText: String? = null,
    val showLogsModal: Boolean = false,
    val buildHistory: List<BuildHistoryEntity> = emptyList(),
    val currentTab: Int = 0, // 0 = Build, 1 = History, 2 = GitHub, 3 = Settings
    val repoErrorMessage: String? = null,
    val isCreatingRepo: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = GitHubAuthManager(application)
    val gitHubRepo = GitHubRepository(authManager.apiService)
    val projectScanner = ProjectScanner(application)
    val projectUploader = ProjectUploader(application, authManager.apiService)
    val buildEngine = GitHubActionsBuildEngine(authManager.apiService)
    val apkInstaller = ApkInstaller(application)
    val database = AppDatabase.getDatabase(application)
    val buildHistoryDao = database.buildHistoryDao()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Observe auth state
        viewModelScope.launch {
            authManager.authState.collectLatest { auth ->
                _uiState.value = _uiState.value.copy(authState = auth)
                if (auth is AuthState.Authenticated) {
                    loadUserRepositories(auth.token)
                }
            }
        }

        // Observe build history
        viewModelScope.launch {
            buildHistoryDao.getAllHistory().collectLatest { history ->
                _uiState.value = _uiState.value.copy(buildHistory = history)
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
    }

    fun startDeviceAuthFlow() {
        authManager.startDeviceFlow()
    }

    fun cancelDeviceAuthFlow() {
        authManager.cancelDeviceAuthFlow()
    }

    fun disconnectGitHub() {
        authManager.disconnect()
        _uiState.value = _uiState.value.copy(
            userRepositories = emptyList(),
            selectedRepository = null
        )
    }

    fun loadUserRepositories(tokenOverride: String? = null) {
        val token = tokenOverride ?: (authManager.authState.value as? AuthState.Authenticated)?.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRepos = true, repoErrorMessage = null)
            val res = gitHubRepo.listUserRepositories(token)
            if (res.isSuccess) {
                val repos = res.getOrDefault(emptyList())
                val defaultRepo = repos.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    userRepositories = repos,
                    selectedRepository = _uiState.value.selectedRepository ?: defaultRepo,
                    isLoadingRepos = false
                )
                if (_uiState.value.selectedRepository != null) {
                    loadWorkflowsForSelectedRepo()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingRepos = false,
                    repoErrorMessage = res.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun selectRepository(repo: GitHubRepo) {
        _uiState.value = _uiState.value.copy(selectedRepository = repo)
        loadWorkflowsForSelectedRepo(repoOverride = repo)
    }

    fun loadWorkflowsForSelectedRepo(repoOverride: GitHubRepo? = null) {
        val auth = authManager.authState.value as? AuthState.Authenticated ?: return
        val repo = repoOverride ?: _uiState.value.selectedRepository ?: return
        val owner = repo.owner?.login ?: auth.user.login

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingWorkflows = true,
                workflowErrorMessage = null
            )
            val res = buildEngine.findWorkflows(auth.token, owner, repo.name)
            if (res.isSuccess) {
                var workflows: List<GitHubWorkflow> = res.getOrDefault(emptyList())
                if (workflows.isEmpty()) {
                    val ensureRes = buildEngine.ensureWorkflowExists(
                        auth.token, owner, repo.name, repo.defaultBranch ?: "main"
                    )
                    if (ensureRes.isSuccess) {
                        workflows = listOf(ensureRes.getOrThrow())
                    }
                }

                val preferred = workflows.find { wf ->
                    wf.path.contains("android", ignoreCase = true) || 
                    wf.name.contains("Android", ignoreCase = true) || 
                    wf.name.contains("APK", ignoreCase = true)
                } ?: workflows.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    discoveredWorkflows = workflows,
                    selectedWorkflow = preferred,
                    isLoadingWorkflows = false,
                    workflowErrorMessage = if (workflows.isEmpty()) "No build workflow found in repository." else null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    discoveredWorkflows = emptyList(),
                    selectedWorkflow = null,
                    isLoadingWorkflows = false,
                    workflowErrorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to list workflows."
                )
            }
        }
    }

    fun selectWorkflow(workflow: GitHubWorkflow) {
        _uiState.value = _uiState.value.copy(selectedWorkflow = workflow)
    }

    fun createPrivateBuildRepository(repoName: String = "my-android-app") {
        val token = (authManager.authState.value as? AuthState.Authenticated)?.token ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingRepo = true, repoErrorMessage = null)
            val res = gitHubRepo.createPrivateBuildRepository(token, repoName)
            if (res.isSuccess) {
                val newRepo = res.getOrThrow()
                val updatedRepos = _uiState.value.userRepositories + newRepo
                _uiState.value = _uiState.value.copy(
                    userRepositories = updatedRepos,
                    selectedRepository = newRepo,
                    isCreatingRepo = false
                )
                loadWorkflowsForSelectedRepo(repoOverride = newRepo)
            } else {
                _uiState.value = _uiState.value.copy(
                    isCreatingRepo = false,
                    repoErrorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to create repository."
                )
            }
        }
    }

    fun scanFolder(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningProject = true)
            val info = projectScanner.scanFolder(treeUri)
            _uiState.value = _uiState.value.copy(
                selectedProject = info,
                isScanningProject = false
            )
        }
    }

    fun scanZip(zipUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningProject = true)
            val info = projectScanner.scanZip(zipUri)
            _uiState.value = _uiState.value.copy(
                selectedProject = info,
                isScanningProject = false
            )
        }
    }

    fun setBuildType(type: String) {
        _uiState.value = _uiState.value.copy(buildType = type)
    }

    fun startBuildProcess() {
        val auth = authManager.authState.value as? AuthState.Authenticated
        if (auth == null) {
            _uiState.value = _uiState.value.copy(
                activeBuildState = ActiveBuildState.Failed(null, "GitHub account is not connected. Please connect your GitHub account first.")
            )
            return
        }

        val repo = _uiState.value.selectedRepository
        if (repo == null) {
            _uiState.value = _uiState.value.copy(
                activeBuildState = ActiveBuildState.Failed(null, "No build repository selected. Please create or select a repository.")
            )
            return
        }

        val project = _uiState.value.selectedProject
        if (project == null || !project.isValid) {
            _uiState.value = _uiState.value.copy(
                activeBuildState = ActiveBuildState.Failed(null, "No valid Android project selected. Please pick a valid project folder.")
            )
            return
        }

        val dispatchStartTimeMillis = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                // Step 1: Upload project
                _uiState.value = _uiState.value.copy(
                    activeBuildState = ActiveBuildState.UploadingProject("Preparing project files...", 0.05f)
                )

                val uploadResult = projectUploader.uploadProjectToRepo(
                    token = auth.token,
                    owner = repo.owner?.login ?: auth.user.login,
                    repo = repo.name,
                    branch = repo.defaultBranch ?: "main",
                    projectInfo = project,
                    onProgress = { step, percent ->
                        _uiState.value = _uiState.value.copy(
                            activeBuildState = ActiveBuildState.UploadingProject(step, percent)
                        )
                    }
                )

                if (uploadResult.isFailure) {
                    val err = uploadResult.exceptionOrNull()?.localizedMessage ?: "Project upload failed."
                    _uiState.value = _uiState.value.copy(
                        activeBuildState = ActiveBuildState.Failed(null, "Upload failed: $err")
                    )
                    return@launch
                }

                val newCommitSha = uploadResult.getOrNull()

                // Step 2: Detect or trigger GitHub Actions workflow run
                _uiState.value = _uiState.value.copy(activeBuildState = ActiveBuildState.TriggeringWorkflow)

                var selectedWf = _uiState.value.selectedWorkflow
                if (selectedWf == null) {
                    val ensureRes = buildEngine.ensureWorkflowExists(
                        auth.token, repo.owner?.login ?: auth.user.login, repo.name, repo.defaultBranch ?: "main"
                    )
                    selectedWf = ensureRes.getOrNull()
                }

                val workflowFileNameOrId = selectedWf?.path?.substringAfterLast("/")
                    ?: selectedWf?.id?.toString()
                    ?: "android.yml"

                val triggerResult = buildEngine.findOrTriggerRun(
                    token = auth.token,
                    owner = repo.owner?.login ?: auth.user.login,
                    repo = repo.name,
                    workflowFileNameOrId = workflowFileNameOrId,
                    headSha = newCommitSha,
                    branch = repo.defaultBranch ?: "main",
                    dispatchStartTimeMillis = dispatchStartTimeMillis,
                    buildType = _uiState.value.buildType
                )

                if (triggerResult.isFailure) {
                    val err = triggerResult.exceptionOrNull()?.localizedMessage ?: "Failed to find or trigger workflow run."
                    _uiState.value = _uiState.value.copy(
                        activeBuildState = ActiveBuildState.Failed(null, "Workflow run error: $err")
                    )
                    return@launch
                }

                val runId = triggerResult.getOrThrow()

                // Insert into DB
                val historyId = buildHistoryDao.insertBuild(
                    BuildHistoryEntity(
                        projectName = project.projectName,
                        buildType = _uiState.value.buildType,
                        workflowRunId = runId,
                        status = "BUILDING",
                        repositoryName = repo.fullName
                    )
                )

                // Step 3: Start polling
                pollWorkflowStatus(auth.token, repo.owner?.login ?: auth.user.login, repo.name, runId, historyId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    activeBuildState = ActiveBuildState.Failed(null, "Unexpected build error: ${e.localizedMessage}")
                )
            }
        }
    }

    private fun pollWorkflowStatus(token: String, owner: String, repo: String, runId: Long, historyId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 120 // max 10 mins

            while (attempts < maxAttempts) {
                val statusResult = buildEngine.pollBuildStatus(token, owner, repo, runId)

                if (statusResult.isSuccess) {
                    val run = statusResult.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        activeBuildState = ActiveBuildState.PollingStatus(runId, run.status, run.conclusion)
                    )

                    if (run.status == "completed") {
                        if (run.conclusion == "success") {
                            // Download artifact
                            _uiState.value = _uiState.value.copy(
                                activeBuildState = ActiveBuildState.DownloadingArtifact(runId)
                            )

                            val outputDir = File(getApplication<Application>().cacheDir, "apks/$runId")
                            val downloadRes = buildEngine.downloadApkArtifact(token, owner, repo, runId, outputDir)

                            if (downloadRes.isSuccess) {
                                val apkFile = downloadRes.getOrThrow()
                                val artifactName = apkFile.name

                                buildHistoryDao.insertBuild(
                                    BuildHistoryEntity(
                                        id = historyId,
                                        projectName = _uiState.value.selectedProject?.projectName ?: "Android Project",
                                        buildType = _uiState.value.buildType,
                                        workflowRunId = runId,
                                        status = "SUCCESS",
                                        artifactName = artifactName,
                                        apkPath = apkFile.absolutePath,
                                        repositoryName = "$owner/$repo"
                                    )
                                )

                                _uiState.value = _uiState.value.copy(
                                    activeBuildState = ActiveBuildState.Success(runId, apkFile, artifactName)
                                )
                            } else {
                                val err = downloadRes.exceptionOrNull()?.localizedMessage ?: "Failed to download APK artifact."
                                buildHistoryDao.insertBuild(
                                    BuildHistoryEntity(
                                        id = historyId,
                                        projectName = _uiState.value.selectedProject?.projectName ?: "Android Project",
                                        buildType = _uiState.value.buildType,
                                        workflowRunId = runId,
                                        status = "FAILED",
                                        errorMessage = err,
                                        repositoryName = "$owner/$repo"
                                    )
                                )
                                _uiState.value = _uiState.value.copy(
                                    activeBuildState = ActiveBuildState.Failed(runId, err)
                                )
                            }
                        } else {
                            // Workflow failed
                            val logRes = buildEngine.getBuildLogs(token, owner, repo, runId)
                            val logs = logRes.getOrNull() ?: "Workflow execution failed on GitHub Actions."

                            buildHistoryDao.insertBuild(
                                BuildHistoryEntity(
                                    id = historyId,
                                    projectName = _uiState.value.selectedProject?.projectName ?: "Android Project",
                                    buildType = _uiState.value.buildType,
                                    workflowRunId = runId,
                                    status = "FAILED",
                                    errorMessage = "Workflow failed with conclusion: ${run.conclusion}",
                                    repositoryName = "$owner/$repo"
                                )
                            )

                            _uiState.value = _uiState.value.copy(
                                activeBuildState = ActiveBuildState.Failed(runId, "GitHub Actions build failed during Gradle execution.", logs)
                            )
                        }
                        break
                    }
                }

                delay(5000) // Poll every 5s
                attempts++
            }
        }
    }

    fun openLogsModal(runId: Long?, repositoryName: String? = null) {
        val auth = authManager.authState.value as? AuthState.Authenticated
        var targetOwner = _uiState.value.selectedRepository?.owner?.login ?: auth?.user?.login
        var targetRepo = _uiState.value.selectedRepository?.name

        if (!repositoryName.isNullOrEmpty() && repositoryName.contains("/")) {
            targetOwner = repositoryName.substringBefore("/")
            targetRepo = repositoryName.substringAfter("/")
        }

        if (auth != null && targetOwner != null && targetRepo != null && runId != null) {
            viewModelScope.launch {
                val logsRes = buildEngine.getBuildLogs(auth.token, targetOwner, targetRepo, runId)
                val text = logsRes.getOrNull() ?: "Unable to fetch logs for run #$runId"
                _uiState.value = _uiState.value.copy(buildLogsText = text, showLogsModal = true)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                buildLogsText = _uiState.value.buildLogsText ?: "No logs available for this build run.",
                showLogsModal = true
            )
        }
    }

    fun closeLogsModal() {
        _uiState.value = _uiState.value.copy(showLogsModal = false)
    }

    fun installApk(file: File) {
        apkInstaller.installApk(file)
    }

    fun shareApk(file: File) {
        apkInstaller.shareApk(file)
    }

    fun saveApkToDownloads(file: File) {
        apkInstaller.saveToDownloads(file)
    }

    fun cancelBuild() {
        pollingJob?.cancel()
        val auth = authManager.authState.value as? AuthState.Authenticated
        val repo = _uiState.value.selectedRepository
        val active = _uiState.value.activeBuildState

        val runId = when (active) {
            is ActiveBuildState.PollingStatus -> active.runId
            is ActiveBuildState.DownloadingArtifact -> active.runId
            else -> null
        }

        if (auth != null && repo != null && runId != null) {
            viewModelScope.launch {
                val owner = repo.owner?.login ?: auth.user.login
                buildEngine.cancelBuild(auth.token, owner, repo.name, runId)
                _uiState.value = _uiState.value.copy(
                    activeBuildState = ActiveBuildState.Failed(runId, "Build Cancelled by User")
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                activeBuildState = ActiveBuildState.Failed(null, "Build Cancelled by User")
            )
        }
    }

    fun resetActiveBuildState() {
        _uiState.value = _uiState.value.copy(activeBuildState = ActiveBuildState.Idle)
    }

    fun clearBuildHistory() {
        viewModelScope.launch {
            buildHistoryDao.clearAll()
        }
    }
}
