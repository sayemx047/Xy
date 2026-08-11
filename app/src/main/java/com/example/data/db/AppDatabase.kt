package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        NoticeEntity::class,
        WalletTransactionEntity::class,
        TournamentEntity::class,
        TournamentRegistrationEntity::class,
        ChatMessageEntity::class,
        DeviceLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noticeDao(): NoticeDao
    abstract fun walletDao(): WalletDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun chatDao(): ChatDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tournament_gaming_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial data asynchronously
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedInitialData(database)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            // Seed Notices
            db.noticeDao().insertNotice(
                NoticeEntity(
                    title = "🔥 Season 12 Grand Championship!",
                    content = "Welcome players! BDT 50,000 Prize Pool Tournament registrations are live now. bKash recharge is 100% automated with instant admin review.",
                    date = "2026-08-08",
                    isForLoginScreen = true
                )
            )
            db.noticeDao().insertNotice(
                NoticeEntity(
                    title = "⚡ bKash Recharge Update",
                    content = "Send Money to official bKash Cash In / Personal number: 01789495251 and submit your Transaction ID immediately for balance verification.",
                    date = "2026-08-08",
                    isForLoginScreen = true
                )
            )

            // Seed Tournaments
            db.tournamentDao().insertTournament(
                TournamentEntity(
                    id = "FF-M101",
                    title = "Free Fire Pro Clash - Bermuda Battle Royale",
                    gameMode = "Squad",
                    map = "Bermuda",
                    entryFee = 50.0,
                    prizePool = 2500.0,
                    perKill = 15.0,
                    scheduleTime = "Today at 09:00 PM",
                    slotsTotal = 12,
                    slotsFilled = 4,
                    status = "UPCOMING"
                )
            )
            db.tournamentDao().insertTournament(
                TournamentEntity(
                    id = "FF-M102",
                    title = "Free Fire Solo Warfare - Kalahari Cup",
                    gameMode = "Solo",
                    map = "Kalahari",
                    entryFee = 20.0,
                    prizePool = 800.0,
                    perKill = 10.0,
                    scheduleTime = "Tomorrow at 06:00 PM",
                    slotsTotal = 48,
                    slotsFilled = 18,
                    status = "UPCOMING"
                )
            )
            db.tournamentDao().insertTournament(
                TournamentEntity(
                    id = "FF-M103",
                    title = "Free Fire Duo Showdown - Purgatory Elite",
                    gameMode = "Duo",
                    map = "Purgatory",
                    entryFee = 35.0,
                    prizePool = 1500.0,
                    perKill = 12.0,
                    scheduleTime = "Tomorrow at 10:00 PM",
                    slotsTotal = 24,
                    slotsFilled = 12,
                    status = "UPCOMING"
                )
            )

            // Seed default Admin chat greeting
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    chatContext = "GENERAL",
                    senderEmail = "admin@gamingplatform.com",
                    senderName = "Tournament Admin",
                    isAdmin = true,
                    message = "Hello Gamer! Welcome to Live Support. How can we assist you today?",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
