package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val password: String,
    val firstName: String = "",
    val lastName: String = "",
    val ffUid: String = "",
    val whatsapp: String = "",
    val walletBalance: Double = 0.0,
    val isLoggedIn: Boolean = false,
    val isPasswordSaved: Boolean = false
)

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetEmail: String? = null, // null means global notice
    val title: String,
    val content: String,
    val date: String,
    val isRead: Boolean = false,
    val isForLoginScreen: Boolean = true
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val amount: Double,
    val transactionId: String,
    val bkashNumber: String = "01789495251",
    val status: String = "PENDING", // PENDING, VERIFIED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val gameMode: String, // Solo, Duo, Squad
    val map: String, // Bermuda, Kalahari, Purgatory, Alpine
    val entryFee: Double,
    val prizePool: Double,
    val perKill: Double,
    val scheduleTime: String,
    val slotsTotal: Int,
    val slotsFilled: Int,
    val roomId: String = "TBA",
    val roomPassword: String = "TBA",
    val status: String = "UPCOMING", // UPCOMING, LIVE, COMPLETED
    val pinnedImageUri: String? = null
)

@Entity(tableName = "tournament_registrations")
data class TournamentRegistrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: String,
    val userEmail: String,
    val ffUid: String,
    val firstName: String,
    val lastName: String,
    val squadName: String = "",
    val playerUsernames: String = "",
    val whatsapp: String = "",
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatContext: String = "GENERAL", // GENERAL, FORGOT_PASSWORD, TOURNAMENT_<ID>
    val senderEmail: String,
    val senderName: String,
    val isAdmin: Boolean = false,
    val message: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false
)

@Entity(tableName = "device_logs")
data class DeviceLogEntity(
    @PrimaryKey val userEmail: String,
    val model: String,
    val batteryLevel: Int,
    val networkType: String,
    val ipAddress: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
