package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.auth.AuthState
import com.example.ui.components.LogViewerModal
import com.example.ui.screens.BuildHistoryScreen
import com.example.ui.screens.GitHubConnectScreen
import com.example.ui.screens.MainBuildScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Native APK Builder",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            actions = {
                                val auth = uiState.authState
                                if (auth is AuthState.Authenticated) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        onClick = { viewModel.selectTab(2) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    IconButton(onClick = { viewModel.selectTab(2) }) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = "GitHub Account",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = uiState.currentTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Build, contentDescription = "Build") },
                                label = { Text("Build") }
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History") }
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.Link, contentDescription = "GitHub") },
                                label = { Text("GitHub") }
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (uiState.currentTab) {
                            0 -> MainBuildScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onNavigateToGitHub = { viewModel.selectTab(2) }
                            )
                            1 -> BuildHistoryScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                            2 -> GitHubConnectScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                            3 -> SettingsScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        }

                        if (uiState.showLogsModal) {
                            LogViewerModal(
                                logText = uiState.buildLogsText,
                                onDismiss = { viewModel.closeLogsModal() }
                            )
                        }
                    }
                }
            }
        }
    }
}
