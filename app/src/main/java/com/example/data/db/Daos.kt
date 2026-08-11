package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun clearLoginState()

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE isForLoginScreen = 1 ORDER BY id DESC")
    fun getLoginNoticesFlow(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE targetEmail IS NULL OR targetEmail = :email ORDER BY id DESC")
    fun getUserNoticesFlow(email: String): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteNotice(id: Int)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_transactions WHERE userEmail = :userEmail ORDER BY timestamp DESC")
    fun getUserTransactionsFlow(userEmail: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)

    @Query("UPDATE wallet_transactions SET status = :status WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, status: String)

    @Query("SELECT * FROM wallet_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): WalletTransactionEntity?
}

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY scheduleTime ASC")
    fun getAllTournamentsFlow(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournamentFlow(id: String): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("UPDATE tournaments SET slotsFilled = slotsFilled + 1 WHERE id = :id")
    suspend fun incrementSlot(id: String)

    @Query("UPDATE tournaments SET pinnedImageUri = :imageUri WHERE id = :id")
    suspend fun updatePinnedImage(id: String, imageUri: String?)
}

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM tournament_registrations WHERE userEmail = :userEmail ORDER BY registeredAt DESC")
    fun getUserRegistrationsFlow(userEmail: String): Flow<List<TournamentRegistrationEntity>>

    @Query("SELECT * FROM tournament_registrations WHERE tournamentId = :tournamentId AND userEmail = :userEmail LIMIT 1")
    suspend fun getRegistration(tournamentId: String, userEmail: String): TournamentRegistrationEntity?

    @Query("SELECT * FROM tournament_registrations WHERE tournamentId = :tournamentId")
    fun getTournamentRegistrationsFlow(tournamentId: String): Flow<List<TournamentRegistrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: TournamentRegistrationEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE chatContext = :chatContext ORDER BY timestamp ASC")
    fun getChatMessagesFlow(chatContext: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE chatContext = :chatContext ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMessages(chatContext: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET message = :newMessage, isEdited = 1 WHERE id = :id")
    suspend fun updateMessage(id: Long, newMessage: String)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOldMessages(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE chatContext = :chatContext AND senderEmail = :userEmail AND id > (SELECT COALESCE(MAX(id), 0) FROM chat_messages WHERE chatContext = :chatContext AND isAdmin = 1)")
    suspend fun getConsecutiveUserMessagesCount(chatContext: String, userEmail: String): Int
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device_logs WHERE userEmail = :userEmail LIMIT 1")
    fun getDeviceLogFlow(userEmail: String): Flow<DeviceLogEntity?>

    @Query("SELECT * FROM device_logs ORDER BY lastUpdated DESC")
    fun getAllDeviceLogsFlow(): Flow<List<DeviceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceLog(deviceLog: DeviceLogEntity)
}
