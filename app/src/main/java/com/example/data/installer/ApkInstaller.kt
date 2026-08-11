package com.example.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ApkInstaller(private val context: Context) {

    companion object {
        const val FILE_PROVIDER_AUTHORITY = "com.aistudio.nativeapkbuilder.app.fileprovider"
    }

    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK file not found.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                Toast.makeText(context, "Please allow 'Install unknown apps' permission to install the APK.", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return false
            }
        }

        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting APK installation: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    fun shareApk(apkFile: File) {
        if (!apkFile.exists()) return

        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                apkFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share APK File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing APK: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveToDownloads(apkFile: File): File? {
        if (!apkFile.exists()) return null

        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val targetFile = File(downloadsDir, apkFile.name)
            apkFile.copyTo(targetFile, overwrite = true)
            Toast.makeText(context, "Saved APK to Downloads: ${targetFile.name}", Toast.LENGTH_LONG).show()
            targetFile
        } catch (e: Exception) {
            Toast.makeText(context, "Saved APK to app storage directory.", Toast.LENGTH_SHORT).show()
            apkFile
        }
    }
}
