package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.AppRepository
import com.example.utils.DeviceMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class TournamentDetail(val tournamentId: String) : Screen()
    object Wallet : Screen()
    data class LiveChat(val contextKey: String = "GENERAL") : Screen()
    object Profile : Screen()
    object AdminPanel : Screen()
}

data class UiAlert(
    val title: String,
    val message: String,
    val isSuccess: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = AppRepository(db)

    // User State
    val loggedInUser: StateFlow<UserEntity?> = repository.loggedInUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Alert Dialogs
    private val _uiAlert = MutableStateFlow<UiAlert?>(null)
    val uiAlert: StateFlow<UiAlert?> = _uiAlert.asStateFlow()

    // Insufficient Balance Dialog Prompt ("Not enough credit. Wanna recharge?")
    private val _showRechargePrompt = MutableStateFlow(false)
    val showRechargePrompt: StateFlow<Boolean> = _showRechargePrompt.asStateFlow()

    // Admin Mode Toggle for testing verification & notice broadcast
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // Data Flows
    val loginNotices: StateFlow<List<NoticeEntity>> = repository.loginNoticesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotices: StateFlow<List<NoticeEntity>> = loggedInUser
        .flatMapLatest { user ->
            if (user != null) repository.getUserNoticesFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions: StateFlow<List<WalletTransactionEntity>> = loggedInUser
        .flatMapLatest { user ->
            if (user != null) repository.getUserTransactionsFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<WalletTransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tournaments: StateFlow<List<TournamentEntity>> = repository.allTournamentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRegistrations: StateFlow<List<TournamentRegistrationEntity>> = loggedInUser
        .flatMapLatest { user ->
            if (user != null) repository.getUserRegistrationsFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userDeviceLog: StateFlow<DeviceLogEntity?> = loggedInUser
        .flatMapLatest { user ->
            if (user != null) repository.getDeviceLogFlow(user.email) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDeviceLogs: StateFlow<List<DeviceLogEntity>> = repository.allDeviceLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe logged in user & sync device monitoring details
        viewModelScope.launch {
            loggedInUser.collect { user ->
                if (user != null) {
                    if (_currentScreen.value == Screen.Login) {
                        _currentScreen.value = Screen.Home
                    }
                    val log = DeviceMonitor.captureDeviceDetails(getApplication(), user.email)
                    repository.updateDeviceLog(log)
                } else {
                    _currentScreen.value = Screen.Login
                }
            }
        }

        // Periodically purge chats older than 24h
        viewModelScope.launch {
            repository.autoPurgeOldChats(24)
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun dismissAlert() {
        _uiAlert.value = null
    }

    fun showRechargePrompt(show: Boolean) {
        _showRechargePrompt.value = show
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    // --- Auth Actions ---
    fun register(
        email: String,
        pass: String,
        fname: String,
        lname: String,
        ffUid: String,
        whatsapp: String,
        savePass: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(email, pass, fname, lname, ffUid, whatsapp, savePass)
            result.onSuccess { user ->
                _uiAlert.value = UiAlert(
                    title = "Account Created!",
                    message = "Welcome ${user.firstName}! Your account has been registered and credited with BDT 100 bonus.",
                    isSuccess = true
                )
            }.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Registration Failed",
                    message = err.message ?: "Could not register account.",
                    isSuccess = false
                )
            }
        }
    }

    fun login(email: String, pass: String, savePass: Boolean) {
        viewModelScope.launch {
            val result = repository.loginUser(email, pass, savePass)
            result.onSuccess { user ->
                _uiAlert.value = UiAlert(
                    title = "Welcome Back!",
                    message = "Successfully logged in as ${user.email}.",
                    isSuccess = true
                )
            }.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Login Failed",
                    message = err.message ?: "Incorrect Password/Email.",
                    isSuccess = false
                )
            }
        }
    }

    fun resetPasswordDirectly(email: String, newPass: String) {
        val targetEmail = email.trim()
        if (targetEmail.isEmpty() || newPass.trim().isEmpty()) {
            _uiAlert.value = UiAlert(
                title = "Input Required",
                message = "Please enter your registered email address and a new password.",
                isSuccess = false
            )
            return
        }
        viewModelScope.launch {
            val res = repository.resetPasswordDirectly(targetEmail, newPass.trim())
            res.onSuccess {
                _uiAlert.value = UiAlert(
                    title = "Password Reset Successful",
                    message = "Password for $targetEmail has been updated! Please sign in with your new password.",
                    isSuccess = true
                )
            }.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Reset Failed",
                    message = err.message ?: "Account not found or password too short.",
                    isSuccess = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
            _currentScreen.value = Screen.Login
        }
    }

    fun changePassword(currentPass: String, newPass: String) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val res = repository.changePassword(user.email, currentPass, newPass)
            res.onSuccess {
                _uiAlert.value = UiAlert(
                    title = "Password Updated",
                    message = "Your password has been changed successfully.",
                    isSuccess = true
                )
            }.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Password Change Failed",
                    message = err.message ?: "Could not change password.",
                    isSuccess = false
                )
            }
        }
    }

    fun deleteAccount() {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            repository.deleteAccount(user.email)
            _uiAlert.value = UiAlert(
                title = "Account Deleted",
                message = "Your account and data have been permanently removed.",
                isSuccess = true
            )
            _currentScreen.value = Screen.Login
        }
    }

    fun updateProfile(fname: String, lname: String, ffUid: String, whatsapp: String) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateProfileInfo(user.email, fname, lname, ffUid, whatsapp)
            res.onSuccess {
                _uiAlert.value = UiAlert(
                    title = "Profile Saved",
                    message = "Your gamer profile details have been updated.",
                    isSuccess = true
                )
            }
        }
    }

    // --- Wallet Actions ---
    fun submitRecharge(amount: Double, transactionId: String) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val res = repository.submitRecharge(user.email, amount, transactionId)
            res.onSuccess {
                _uiAlert.value = UiAlert(
                    title = "Recharge Submitted",
                    message = "Transaction ID $transactionId submitted! Admin is reviewing your bKash payment.",
                    isSuccess = true
                )
            }.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Recharge Error",
                    message = err.message ?: "Could not submit recharge.",
                    isSuccess = false
                )
            }
        }
    }

    // --- Tournament Actions ---
    fun registerTournament(
        tournamentId: String,
        ffUid: String,
        fname: String,
        lname: String,
        squadName: String,
        playerUsernames: String,
        whatsapp: String
    ) {
        val user = loggedInUser.value ?: return
        viewModelScope.launch {
            val res = repository.registerForTournament(
                tournamentId = tournamentId,
                userEmail = user.email,
                ffUid = ffUid,
                firstName = fname,
                lastName = lname,
                squadName = squadName,
                playerUsernames = playerUsernames,
                whatsapp = whatsapp
            )
            res.onSuccess {
                _uiAlert.value = UiAlert(
                    title = "Registration Successful!",
                    message = "Welcome to the tournament! You now have private access to Tournament Info & Room ID.",
                    isSuccess = true
                )
                _currentScreen.value = Screen.TournamentDetail(tournamentId)
            }.onFailure { err ->
                if (err.message == "INSUFFICIENT_FUNDS") {
                    _showRechargePrompt.value = true
                } else {
                    _uiAlert.value = UiAlert(
                        title = "Registration Failed",
                        message = err.message ?: "Could not complete registration.",
                        isSuccess = false
                    )
                }
            }
        }
    }

    // --- Chat Actions ---
    fun sendChatMessage(
        chatContext: String,
        message: String,
        imageUri: String? = null
    ) {
        val user = loggedInUser.value
        val senderEmail = user?.email ?: "guest@gaming.com"
        val senderName = if (user != null) "${user.firstName} (${user.email})" else "Guest User"
        val isAdmin = _isAdminMode.value

        viewModelScope.launch {
            val res = repository.sendChatMessage(
                chatContext = chatContext,
                senderEmail = if (isAdmin) "admin@gaming.com" else senderEmail,
                senderName = if (isAdmin) "Tournament Admin" else senderName,
                isAdmin = isAdmin,
                message = message,
                imageUri = imageUri
            )
            res.onFailure { err ->
                _uiAlert.value = UiAlert(
                    title = "Message Not Sent",
                    message = err.message ?: "Failed to send message.",
                    isSuccess = false
                )
            }
        }
    }

    fun editChatMessage(messageId: Long, newText: String) {
        viewModelScope.launch {
            repository.editChatMessage(messageId, newText)
        }
    }

    // --- Admin Actions ---
    fun adminApproveTransaction(txId: Long, approve: Boolean) {
        viewModelScope.launch {
            repository.verifyTransactionByAdmin(txId, approve)
            _uiAlert.value = UiAlert(
                title = if (approve) "Transaction Verified" else "Transaction Rejected",
                message = if (approve) "Wallet balance credited to user account." else "Transaction status set to REJECTED.",
                isSuccess = approve
            )
        }
    }

    fun adminPostNotice(title: String, content: String, isLoginScreen: Boolean, targetEmail: String? = null) {
        viewModelScope.launch {
            repository.postNotice(
                NoticeEntity(
                    title = title,
                    content = content,
                    date = "2026-08-08",
                    isForLoginScreen = isLoginScreen,
                    targetEmail = if (targetEmail.isNullOrEmpty()) null else targetEmail
                )
            )
            _uiAlert.value = UiAlert(
                title = "Notice Broadcasted",
                message = "Global notice has been published.",
                isSuccess = true
            )
        }
    }

    fun adminUpdateRoomInfo(tournamentId: String, roomId: String, roomPass: String) {
        viewModelScope.launch {
            repository.updateTournamentRoomInfo(tournamentId, roomId, roomPass)
            _uiAlert.value = UiAlert(
                title = "Room Credentials Set",
                message = "Room ID: $roomId & Pass: $roomPass are now visible to joined participants.",
                isSuccess = true
            )
        }
    }

    fun adminPinTournamentImage(tournamentId: String, imageUri: String?) {
        viewModelScope.launch {
            repository.updateTournamentPinnedImage(tournamentId, imageUri)
            _uiAlert.value = UiAlert(
                title = "Banner Pinned",
                message = "Banner image is now pinned on the Home screen for players.",
                isSuccess = true
            )
        }
    }
}
