package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val noticeDao = db.noticeDao()
    private val walletDao = db.walletDao()
    private val tournamentDao = db.tournamentDao()
    private val registrationDao = db.registrationDao()
    private val chatDao = db.chatDao()
    private val deviceDao = db.deviceDao()

    // --- Authentication ---
    val loggedInUserFlow: Flow<UserEntity?> = userDao.getLoggedInUserFlow()

    suspend fun getLoggedInUser(): UserEntity? = userDao.getLoggedInUser()

    suspend fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        ffUid: String,
        whatsapp: String,
        savePassword: Boolean
    ): Result<UserEntity> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isEmpty() || password.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }
        val existing = userDao.getUserByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("An account with this email already exists! (1-account-per-email policy)"))
        }

        userDao.clearLoginState()
        val newUser = UserEntity(
            email = normalizedEmail,
            password = password,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            ffUid = ffUid.trim(),
            whatsapp = whatsapp.trim(),
            walletBalance = 100.0, // Initial bonus BDT 100
            isLoggedIn = true,
            isPasswordSaved = savePassword
        )
        userDao.insertUser(newUser)
        return Result.success(newUser)
    }

    suspend fun loginUser(
        email: String,
        password: String,
        savePassword: Boolean
    ): Result<UserEntity> {
        val trimmedEmail = email.trim()
        val normalizedEmail = trimmedEmail.lowercase()
        val trimmedPass = password.trim()

        // Temporary bypass credentials check: username 'x', password 'y'
        if ((normalizedEmail == "x" || normalizedEmail == "x@gaming.com" || trimmedEmail == "x") && trimmedPass == "y") {
            userDao.clearLoginState()
            var bypassUser = userDao.getUserByEmail("x@gaming.com") ?: userDao.getUserByEmail("x")
            if (bypassUser == null) {
                bypassUser = UserEntity(
                    email = "x@gaming.com",
                    password = "y",
                    firstName = "Bypass",
                    lastName = "Gamer",
                    ffUid = "77889900",
                    whatsapp = "01700000000",
                    walletBalance = 500.0,
                    isLoggedIn = true,
                    isPasswordSaved = savePassword
                )
                userDao.insertUser(bypassUser)
            } else {
                bypassUser = bypassUser.copy(
                    isLoggedIn = true,
                    isPasswordSaved = savePassword
                )
                userDao.updateUser(bypassUser)
            }
            return Result.success(bypassUser)
        }

        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Incorrect Email or Account does not exist."))

        if (user.password != password) {
            return Result.failure(IllegalArgumentException("Incorrect Password. Please try again or use Forgot Password modal."))
        }

        userDao.clearLoginState()
        val updatedUser = user.copy(
            isLoggedIn = true,
            isPasswordSaved = savePassword
        )
        userDao.updateUser(updatedUser)
        return Result.success(updatedUser)
    }

    suspend fun resetPasswordDirectly(email: String, newPass: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for email: $email"))

        if (newPass.length < 3) {
            return Result.failure(IllegalArgumentException("New password must be at least 3 characters."))
        }

        val updated = user.copy(password = newPass)
        userDao.updateUser(updated)
        return Result.success(Unit)
    }

    suspend fun logoutUser() {
        userDao.clearLoginState()
    }

    suspend fun changePassword(
        userEmail: String,
        currentPass: String,
        newPass: String
    ): Result<Unit> {
        val user = userDao.getUserByEmail(userEmail)
            ?: return Result.failure(IllegalStateException("User session invalid."))

        if (user.password != currentPass) {
            return Result.failure(IllegalArgumentException("Current password does not match."))
        }
        if (newPass.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }

        val updated = user.copy(password = newPass)
        userDao.updateUser(updated)
        return Result.success(Unit)
    }

    suspend fun deleteAccount(userEmail: String): Result<Unit> {
        userDao.deleteUser(userEmail)
        return Result.success(Unit)
    }

    suspend fun updateProfileInfo(
        userEmail: String,
        firstName: String,
        lastName: String,
        ffUid: String,
        whatsapp: String
    ): Result<UserEntity> {
        val user = userDao.getUserByEmail(userEmail)
            ?: return Result.failure(IllegalStateException("User not found."))
        val updated = user.copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            ffUid = ffUid.trim(),
            whatsapp = whatsapp.trim()
        )
        userDao.updateUser(updated)
        return Result.success(updated)
    }

    // --- Notices ---
    val loginNoticesFlow: Flow<List<NoticeEntity>> = noticeDao.getLoginNoticesFlow()

    fun getUserNoticesFlow(email: String): Flow<List<NoticeEntity>> = noticeDao.getUserNoticesFlow(email)

    suspend fun postNotice(notice: NoticeEntity) {
        noticeDao.insertNotice(notice)
    }

    suspend fun deleteNotice(id: Int) {
        noticeDao.deleteNotice(id)
    }

    // --- Wallet ---
    fun getUserTransactionsFlow(email: String): Flow<List<WalletTransactionEntity>> =
        walletDao.getUserTransactionsFlow(email)

    val allTransactionsFlow: Flow<List<WalletTransactionEntity>> = walletDao.getAllTransactionsFlow()

    suspend fun submitRecharge(
        userEmail: String,
        amount: Double,
        transactionId: String,
        bkashNumber: String = "01789495251"
    ): Result<WalletTransactionEntity> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Recharge amount must be greater than 0 BDT."))
        }
        if (transactionId.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid bKash Transaction ID."))
        }

        val transaction = WalletTransactionEntity(
            userEmail = userEmail,
            amount = amount,
            transactionId = transactionId.trim().uppercase(),
            bkashNumber = bkashNumber,
            status = "PENDING"
        )
        walletDao.insertTransaction(transaction)
        return Result.success(transaction)
    }

    suspend fun verifyTransactionByAdmin(txId: Long, approve: Boolean): Result<Unit> {
        val tx = walletDao.getTransactionById(txId)
            ?: return Result.failure(IllegalStateException("Transaction record not found."))

        val newStatus = if (approve) "VERIFIED" else "REJECTED"
        walletDao.updateTransactionStatus(txId, newStatus)

        if (approve && tx.status != "VERIFIED") {
            val user = userDao.getUserByEmail(tx.userEmail)
            if (user != null) {
                val updatedBalance = user.walletBalance + tx.amount
                userDao.updateUser(user.copy(walletBalance = updatedBalance))
            }
        }
        return Result.success(Unit)
    }

    // --- Tournaments ---
    val allTournamentsFlow: Flow<List<TournamentEntity>> = tournamentDao.getAllTournamentsFlow()

    fun getTournamentFlow(id: String): Flow<TournamentEntity?> = tournamentDao.getTournamentFlow(id)

    fun getUserRegistrationsFlow(email: String): Flow<List<TournamentRegistrationEntity>> =
        registrationDao.getUserRegistrationsFlow(email)

    suspend fun isUserRegistered(tournamentId: String, userEmail: String): Boolean {
        return registrationDao.getRegistration(tournamentId, userEmail) != null
    }

    suspend fun registerForTournament(
        tournamentId: String,
        userEmail: String,
        ffUid: String,
        firstName: String,
        lastName: String,
        squadName: String,
        playerUsernames: String,
        whatsapp: String
    ): Result<TournamentRegistrationEntity> {
        val tournament = tournamentDao.getTournamentById(tournamentId)
            ?: return Result.failure(IllegalStateException("Tournament not found."))

        val user = userDao.getUserByEmail(userEmail)
            ?: return Result.failure(IllegalStateException("User not logged in."))

        if (isUserRegistered(tournamentId, userEmail)) {
            return Result.failure(IllegalStateException("You are already registered for this tournament! Access Tournament Info page."))
        }

        if (user.walletBalance < tournament.entryFee) {
            return Result.failure(IllegalStateException("INSUFFICIENT_FUNDS"))
        }

        if (tournament.slotsFilled >= tournament.slotsTotal) {
            return Result.failure(IllegalStateException("Tournament is already full!"))
        }

        // Deduct entry fee
        val updatedUser = user.copy(walletBalance = user.walletBalance - tournament.entryFee)
        userDao.updateUser(updatedUser)

        // Increment tournament slot
        tournamentDao.incrementSlot(tournamentId)

        // Record registration
        val reg = TournamentRegistrationEntity(
            tournamentId = tournamentId,
            userEmail = userEmail,
            ffUid = ffUid.ifEmpty { user.ffUid },
            firstName = firstName.ifEmpty { user.firstName },
            lastName = lastName.ifEmpty { user.lastName },
            squadName = squadName,
            playerUsernames = playerUsernames,
            whatsapp = whatsapp.ifEmpty { user.whatsapp }
        )
        registrationDao.insertRegistration(reg)

        return Result.success(reg)
    }

    suspend fun updateTournamentRoomInfo(tournamentId: String, roomId: String, roomPass: String) {
        val t = tournamentDao.getTournamentById(tournamentId)
        if (t != null) {
            tournamentDao.updateTournament(t.copy(roomId = roomId, roomPassword = roomPass))
        }
    }

    suspend fun updateTournamentPinnedImage(tournamentId: String, imageUri: String?) {
        tournamentDao.updatePinnedImage(tournamentId, imageUri)
    }

    // --- Live Chat ---
    fun getChatMessagesFlow(context: String): Flow<List<ChatMessageEntity>> =
        chatDao.getChatMessagesFlow(context)

    suspend fun sendChatMessage(
        chatContext: String,
        senderEmail: String,
        senderName: String,
        isAdmin: Boolean,
        message: String,
        imageUri: String? = null
    ): Result<ChatMessageEntity> {
        if (message.trim().isEmpty() && imageUri == null) {
            return Result.failure(IllegalArgumentException("Cannot send empty message."))
        }

        // Check 3 consecutive messages rule for non-admin users
        if (!isAdmin) {
            val count = chatDao.getConsecutiveUserMessagesCount(chatContext, senderEmail)
            if (count >= 3) {
                return Result.failure(IllegalStateException("3-message limit reached! Please wait for admin to reply before sending more messages."))
            }
        }

        val chatMessage = ChatMessageEntity(
            chatContext = chatContext,
            senderEmail = senderEmail,
            senderName = senderName,
            isAdmin = isAdmin,
            message = message.trim(),
            imageUri = imageUri,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(chatMessage)
        return Result.success(chatMessage)
    }

    suspend fun editChatMessage(messageId: Long, newText: String): Result<Unit> {
        if (newText.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        chatDao.updateMessage(messageId, newText.trim())
        return Result.success(Unit)
    }

    suspend fun autoPurgeOldChats(hours: Int = 24) {
        val cutoff = System.currentTimeMillis() - (hours * 3600 * 1000L)
        chatDao.purgeOldMessages(cutoff)
    }

    // --- Device Logs ---
    fun getDeviceLogFlow(email: String): Flow<DeviceLogEntity?> = deviceDao.getDeviceLogFlow(email)

    val allDeviceLogsFlow: Flow<List<DeviceLogEntity>> = deviceDao.getAllDeviceLogsFlow()

    suspend fun updateDeviceLog(deviceLog: DeviceLogEntity) {
        deviceDao.insertDeviceLog(deviceLog)
    }
}
