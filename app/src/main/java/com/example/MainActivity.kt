package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.TournamentGamingTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TournamentGamingTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val currentUser by viewModel.loggedInUser.collectAsState()
                val uiAlert by viewModel.uiAlert.collectAsState()
                val showRechargePrompt by viewModel.showRechargePrompt.collectAsState()
                val isAdminMode by viewModel.isAdminMode.collectAsState()

                val loginNotices by viewModel.loginNotices.collectAsState()
                val userNotices by viewModel.userNotices.collectAsState()
                val userTransactions by viewModel.userTransactions.collectAsState()
                val allTransactions by viewModel.allTransactions.collectAsState()
                val tournaments by viewModel.tournaments.collectAsState()
                val userRegistrations by viewModel.userRegistrations.collectAsState()
                val userDeviceLog by viewModel.userDeviceLog.collectAsState()
                val allDeviceLogs by viewModel.allDeviceLogs.collectAsState()

                Scaffold(
                    topBar = {
                        if (currentScreen !is Screen.Login && currentUser != null) {
                            GamerTopAppBar(
                                walletBalance = currentUser?.walletBalance ?: 0.0,
                                isAdminMode = isAdminMode,
                                onWalletClick = { viewModel.navigateTo(Screen.Wallet) },
                                onChatClick = { viewModel.navigateTo(Screen.LiveChat("GENERAL")) },
                                onAdminToggle = { viewModel.toggleAdminMode() },
                                onProfileClick = { viewModel.navigateTo(Screen.Profile) }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen !is Screen.Login && currentUser != null) {
                            CustomBottomNavBar(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> viewModel.navigateTo(screen) }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Login -> {
                                LoginScreen(
                                    viewModel = viewModel,
                                    loginNotices = loginNotices
                                )
                            }
                            is Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    tournaments = tournaments,
                                    notices = userNotices,
                                    userRegistrations = userRegistrations
                                )
                            }
                            is Screen.TournamentDetail -> {
                                TournamentDetailScreen(
                                    tournamentId = screen.tournamentId,
                                    viewModel = viewModel,
                                    currentUser = currentUser
                                )
                            }
                            is Screen.Wallet -> {
                                WalletScreen(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    transactions = userTransactions
                                )
                            }
                            is Screen.LiveChat -> {
                                LiveChatScreen(
                                    contextKey = screen.contextKey,
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    isAdminMode = isAdminMode
                                )
                            }
                            is Screen.Profile -> {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    currentUser = currentUser,
                                    deviceLog = userDeviceLog
                                )
                            }
                            is Screen.AdminPanel -> {
                                AdminPanelScreen(
                                    viewModel = viewModel,
                                    allTransactions = allTransactions,
                                    tournaments = tournaments,
                                    allDeviceLogs = allDeviceLogs
                                )
                            }
                        }

                        // Render Professional Alert Dialog
                        uiAlert?.let { alert ->
                            ProfessionalAlertCard(
                                alert = alert,
                                onDismiss = { viewModel.dismissAlert() }
                            )
                        }

                        // Render "Not enough credit. Wanna recharge?" Prompt
                        if (showRechargePrompt) {
                            InsufficientCreditDialog(
                                onDismiss = { viewModel.showRechargePrompt(false) },
                                onRechargeConfirm = { viewModel.navigateTo(Screen.Wallet) }
                            )
                        }
                    }
                }
            }
        }
    }
}
