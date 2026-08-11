package com.example.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed encrypted credential storage.
 * Encrypts GitHub tokens using AES-256 GCM prior to persisting in SharedPreferences.
 */
class SecureStorage(context: Context) {

    private val prefs = context.getSharedPreferences("secure_github_auth_prefs", Context.MODE_PRIVATE)
    private val keyAlias = "GitHubTokenMasterKeyAlias"

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // KeyStore initialization fallback
        }
    }

    fun saveToken(token: String) {
        val encrypted = encrypt(token)
        prefs.edit().putString("enc_github_token", encrypted).apply()
    }

    fun getStoredToken(): String? {
        val stored = prefs.getString("enc_github_token", null) ?: return null
        return try {
            decrypt(stored)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserLogin(login: String, avatarUrl: String?) {
        prefs.edit()
            .putString("user_login", login)
            .putString("user_avatar", avatarUrl)
            .apply()
    }

    fun getUserLogin(): String? = prefs.getString("user_login", null)
    fun getUserAvatar(): String? = prefs.getString("user_avatar", null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(data: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, combined, 0, 12)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decryptedBytes = cipher.doFinal(combined, 12, combined.size - 12)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
