package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthState
import com.example.ui.viewmodel.ActiveBuildState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBuildScreen(
    viewModel: MainViewModel,
    uiState: com.example.ui.viewmodel.UiState,
    onNavigateToGitHub: () -> Unit
) {
    val context = LocalContext.current

    // SAF Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanFolder(uri)
        }
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanZip(uri)
        }
    }

    var showNotConnectedDialog by remember { mutableStateOf(false) }

    if (showNotConnectedDialog) {
        AlertDialog(
            onDismissRequest = { showNotConnectedDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("GitHub Not Connected") },
            text = { Text("GitHub account is not connected. Connect your account to use remote GitHub Actions build runners.") },
            confirmButton = {
                Button(onClick = {
                    showNotConnectedDialog = false
                    onNavigateToGitHub()
                }) {
                    Text("Connect GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotConnectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. GITHUB CONNECTION STATUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GitHub Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    when (val auth = uiState.authState) {
                        is AuthState.Authenticated -> {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${auth.user.login}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Not Connected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.authState is AuthState.Authenticated) {
                    val repo = uiState.selectedRepository
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Build Repository",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = repo?.fullName ?: "No repository selected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = onNavigateToGitHub) {
                            Text("Change")
                        }
                    }
                } else {
                    Button(
                        onClick = onNavigateToGitHub,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect GitHub Account")
                    }
                }
            }
        }

        // 2. PROJECT IMPORT CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Android Project Source",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select an existing project folder or ZIP file generated by Google AI Studio or Android Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use This Folder")
                    }

                    OutlinedButton(
                        onClick = { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import ZIP")
                    }
                }

                if (uiState.isScanningProject) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Scanning and validating project files...")
                    }
                }

                // Selected Project Summary
                val project = uiState.selectedProject
                if (project != null && !uiState.isScanningProject) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (project.isValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = project.projectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (project.isValid) "Project Detected ✓" else "Validation Warning ⚠",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (project.isValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Gradle DSL:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(project.gradleDslType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Primary Module:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(project.primaryModule, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("compileSdk / targetSdk:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${project.compileSdk} / ${project.targetSdk}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Gradle Wrapper:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (project.hasGradleWrapper) "✓ Found" else "⚠ Missing (Runner fallback)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (project.validationMessages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        project.validationMessages.forEach { msg ->
                                            Text("• $msg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. BUILD CONFIGURATION & ACTION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Build Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.discoveredWorkflows.isNotEmpty()) {
                    var workflowExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = "Workflow File:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = workflowExpanded,
                        onExpandedChange = { workflowExpanded = !workflowExpanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedWorkflow?.name ?: "Default Workflow",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workflowExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = workflowExpanded,
                            onDismissRequest = { workflowExpanded = false }
                        ) {
                            uiState.discoveredWorkflows.forEach { wf ->
                                DropdownMenuItem(
                                    text = { Text("${wf.name} (${wf.path})") },
                                    onClick = {
                                        viewModel.selectWorkflow(wf)
                                        workflowExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = uiState.buildType == "debug",
                        onClick = { viewModel.setBuildType("debug") },
                        label = { Text("Debug APK") },
                        leadingIcon = {
                            if (uiState.buildType == "debug") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.buildType == "release",
                        onClick = { viewModel.setBuildType("release") },
                        label = { Text("Release APK") },
                        leadingIcon = {
                            if (uiState.buildType == "release") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isBuildInProgress = uiState.activeBuildState !is ActiveBuildState.Idle &&
                        uiState.activeBuildState !is ActiveBuildState.Success &&
                        uiState.activeBuildState !is ActiveBuildState.Failed

                Button(
                    onClick = {
                        if (uiState.authState !is AuthState.Authenticated) {
                            showNotConnectedDialog = true
                        } else {
                            viewModel.startBuildProcess()
                        }
                    },
                    enabled = !isBuildInProgress && uiState.selectedProject != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBuildInProgress) "BUILDING APK..." else "BUILD APK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. ACTIVE BUILD / STATUS CARD
        when (val activeState = uiState.activeBuildState) {
            is ActiveBuildState.UploadingProject -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Uploading Project to GitHub...", fontWeight = FontWeight.Bold)
                                Text(activeState.stepMessage, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { activeState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            is ActiveBuildState.TriggeringWorkflow -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Triggering GitHub Actions workflow...", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is ActiveBuildState.PollingStatus -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Building on GitHub Runner...", fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = activeState.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.openLogsModal(activeState.runId) }) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Logs")
                            }

                            TextButton(
                                onClick = { viewModel.cancelBuild() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Build")
                            }
                        }
                    }
                }
            }

            is ActiveBuildState.DownloadingArtifact -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Build finished! Downloading APK artifact...", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is ActiveBuildState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "BUILD SUCCESSFUL ✓",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeState.artifactName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.installApk(activeState.apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("INSTALL")
                            }

                            OutlinedButton(
                                onClick = { viewModel.saveApkToDownloads(activeState.apkFile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SAVE")
                            }

                            OutlinedButton(
                                onClick = { viewModel.shareApk(activeState.apkFile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SHARE")
                            }
                        }
                    }
                }
            }

            is ActiveBuildState.Failed -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Build Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (activeState.runId != null) {
                                    Text(
                                        text = "Run #${activeState.runId}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!activeState.logOutput.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activeState.logOutput,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 6
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeState.runId != null) {
                                OutlinedButton(
                                    onClick = { viewModel.openLogsModal(activeState.runId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("VIEW LOGS")
                                }
                            }

                            val repo = uiState.selectedRepository
                            if (repo != null && activeState.runId != null) {
                                OutlinedButton(
                                    onClick = {
                                        val url = "https://github.com/${repo.fullName}/actions/runs/${activeState.runId}"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("OPEN GITHUB")
                                }
                            }

                            Button(
                                onClick = { viewModel.startBuildProcess() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RETRY")
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
