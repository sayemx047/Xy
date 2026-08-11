package com.example.data.auth

import android.content.Context
import com.example.data.api.GitHubApiService
import com.example.data.api.GitHubAuthService
import com.example.data.api.models.DeviceCodeResponse
import com.example.data.api.models.GitHubUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class DeviceFlowPending(val deviceCodeResponse: DeviceCodeResponse) : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: GitHubUser, val token: String) : AuthState()
    data class Error(val message: String, val details: String? = null) : AuthState()
}

class GitHubAuthManager(private val context: Context) {

    private val secureStorage = SecureStorage(context)
    private var devicePollJob: Job? = null

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GitHubApiService = retrofit.create(GitHubApiService::class.java)

    private val authRetrofit = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val authService: GitHubAuthService = authRetrofit.create(GitHubAuthService::class.java)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkStoredToken()
    }

    fun checkStoredToken() {
        val storedToken = secureStorage.getStoredToken()
        if (!storedToken.isNullOrEmpty()) {
            _authState.value = AuthState.Authenticating
            CoroutineScope(Dispatchers.IO).launch {
                verifyToken(storedToken)
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val formattedHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
            val response = apiService.getAuthenticatedUser(formattedHeader)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                secureStorage.saveToken(token)
                secureStorage.saveUserLogin(user.login, user.avatarUrl)
                _authState.value = AuthState.Authenticated(user, token)
                true
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "GitHub token is invalid or expired."
                    403 -> "GitHub API access forbidden or rate limit exceeded."
                    else -> "GitHub user verification failed (HTTP ${response.code()})"
                }
                clearStoredData()
                _authState.value = AuthState.Error(errorMsg)
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Network error checking GitHub connection: ${e.localizedMessage}")
            false
        }
    }

    fun startDeviceFlow() {
        devicePollJob?.cancel()
        _authState.value = AuthState.Authenticating

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = authService.requestDeviceCode(GITHUB_CLIENT_ID, DEFAULT_SCOPE)
                if (response.isSuccessful && response.body() != null) {
                    val deviceResp = response.body()!!
                    _authState.value = AuthState.DeviceFlowPending(deviceResp)
                    startPollingDeviceToken(deviceResp)
                } else {
                    val errBody = response.errorBody()?.string()
                    _authState.value = AuthState.Error("Failed to initiate GitHub Device Flow (HTTP ${response.code()} ${errBody ?: ""})")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network error connecting to GitHub: ${e.localizedMessage}")
            }
        }
    }

    private fun startPollingDeviceToken(deviceResp: DeviceCodeResponse) {
        devicePollJob?.cancel()
        devicePollJob = CoroutineScope(Dispatchers.IO).launch {
            var currentIntervalSec = if (deviceResp.interval > 0) deviceResp.interval else 5
            val expiresAt = System.currentTimeMillis() + (deviceResp.expiresIn * 1000L)

            while (isActive && _authState.value is AuthState.DeviceFlowPending) {
                delay(currentIntervalSec * 1000L)

                if (System.currentTimeMillis() > expiresAt) {
                    _authState.value = AuthState.Error("GitHub authorization expired. Please try again.")
                    break
                }

                try {
                    val response = authService.pollDeviceToken(
                        clientId = GITHUB_CLIENT_ID,
                        deviceCode = deviceResp.deviceCode
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val tokenResp = response.body()!!
                        val token = tokenResp.accessToken

                        if (!token.isNullOrEmpty()) {
                            // Verify account via /user API before showing Connected
                            val verified = verifyToken(token)
                            if (verified) {
                                break
                            }
                        }

                        when (tokenResp.error) {
                            "authorization_pending" -> {
                                // Waiting for user to complete auth on github.com
                            }
                            "slow_down" -> {
                                currentIntervalSec += 5
                            }
                            "expired_token" -> {
                                _authState.value = AuthState.Error("GitHub authorization expired. Please try again.")
                                break
                            }
                            "access_denied" -> {
                                _authState.value = AuthState.Error("GitHub authorization was cancelled.")
                                break
                            }
                            else -> {
                                if (!tokenResp.error.isNullOrEmpty()) {
                                    val errorMsg = tokenResp.errorDescription ?: "GitHub login failed: ${tokenResp.error}"
                                    _authState.value = AuthState.Error(errorMsg)
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors during poll and retry next interval
                }
            }
        }
    }

    fun cancelDeviceAuthFlow() {
        devicePollJob?.cancel()
        devicePollJob = null
        _authState.value = AuthState.Unauthenticated
    }

    fun disconnect() {
        devicePollJob?.cancel()
        devicePollJob = null
        clearStoredData()
        _authState.value = AuthState.Unauthenticated
    }

    private fun clearStoredData() {
        secureStorage.clear()
    }

    companion object {
        const val GITHUB_CLIENT_ID = "Ov23liKyNoz8fPQN9jZP"
        const val DEFAULT_SCOPE = "repo workflow read:user user:email"
    }
}
