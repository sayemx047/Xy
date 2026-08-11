package com.example.data.project

import android.net.Uri

data class ProjectInfo(
    val projectName: String,
    val rootUri: Uri?,
    val localDirectoryPath: String? = null,
    val moduleNames: List<String> = emptyList(),
    val primaryModule: String = "app",
    val compileSdk: String = "Unknown",
    val minSdk: String = "Unknown",
    val targetSdk: String = "Unknown",
    val hasGradleWrapper: Boolean = false,
    val hasAndroidManifest: Boolean = false,
    val gradleDslType: String = "Kotlin DSL (.kts)",
    val hasDotEnv: Boolean = false,
    val hasDotEnvExample: Boolean = false,
    val hasSettingsGradle: Boolean = false,
    val hasSettingsGradleKts: Boolean = false,
    val hasAppBuildGradle: Boolean = false,
    val hasAppBuildGradleKts: Boolean = false,
    val hasWrapperJar: Boolean = false,
    val isValid: Boolean = true,
    val validationMessages: List<String> = emptyList(),
    val totalFilesCount: Int = 0,
    val totalSizeInBytes: Long = 0L
) {
    fun getSummaryText(): String {
        return """
            Project files detected: $totalFilesCount
            .env: ${if (hasDotEnv) "Found" else "Not found"}
            .env.example: ${if (hasDotEnvExample) "Found" else "Not found"}
            settings.gradle: ${if (hasSettingsGradle) "Found" else "Not found"}
            settings.gradle.kts: ${if (hasSettingsGradleKts) "Found" else "Not found"}
            gradlew: ${if (hasGradleWrapper) "Found" else "Not found"}
            gradle-wrapper.jar: ${if (hasWrapperJar) "Found" else "Not found"}
            app/build.gradle: ${if (hasAppBuildGradle) "Found" else "Not found"}
            app/build.gradle.kts: ${if (hasAppBuildGradleKts) "Found" else "Not found"}
        """.trimIndent()
    }
}

data class ProjectFileItem(
    val relativePath: String,
    val contentBytes: ByteArray,
    val isExecutable: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProjectFileItem
        if (relativePath != other.relativePath) return false
        if (!contentBytes.contentEquals(other.contentBytes)) return false
        if (isExecutable != other.isExecutable) return false
        return true
    }

    override fun hashCode(): Int {
        var result = relativePath.hashCode()
        result = 31 * result + contentBytes.contentHashCode()
        result = 31 * result + isExecutable.hashCode()
        return result
    }
}
