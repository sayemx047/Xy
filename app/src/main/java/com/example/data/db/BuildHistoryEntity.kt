package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val buildType: String, // "debug" or "release"
    val timestamp: Long = System.currentTimeMillis(),
    val workflowRunId: Long,
    val status: String, // "SUCCESS", "FAILED", "CANCELLED", "BUILDING"
    val artifactName: String? = null,
    val apkPath: String? = null,
    val repositoryName: String? = null,
    val errorMessage: String? = null
)
