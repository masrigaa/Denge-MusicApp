package com.asla.denge.data.repository

import android.accounts.AccountManager
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.asla.denge.data.remote.innertube.InnertubeClient
import com.asla.denge.domain.model.UserAccount
import com.asla.denge.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthRepositoryImpl(
    private val context: Context,
    private val innertubeClient: InnertubeClient,
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences("auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _authState = MutableStateFlow(!prefs.getString(KEY_AUTH_TOKEN, null).isNullOrBlank())
    private val _accountsFlow = MutableStateFlow<List<UserAccount>>(emptyList())

    init {
        loadSavedAccounts()
    }

    private fun loadSavedAccounts(): List<UserAccount> {
        val currentEmail = prefs.getString(KEY_USER_EMAIL, null)
        val rawJson = prefs.getString(KEY_SAVED_ACCOUNTS, null)
        val list = if (!rawJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<UserAccount>>(rawJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val updated = if (list.isEmpty() && !currentEmail.isNullOrBlank()) {
            val rawName = prefs.getString(KEY_USER_NAME, null)
            val name = if (rawName.isNullOrBlank() || rawName.equals("Pengguna Google", ignoreCase = true) || rawName.equals("Google User", ignoreCase = true) || rawName.equals("Akun Google Terhubung", ignoreCase = true) || rawName.equals("Connected Google Account", ignoreCase = true)) {
                com.asla.denge.util.GoogleAccountPicker.formatDisplayName(currentEmail)
            } else rawName
            val avatar = prefs.getString(KEY_USER_AVATAR, null)
            val cookie = prefs.getString(KEY_AUTH_COOKIE, null)
            listOf(
                UserAccount(
                    email = currentEmail,
                    displayName = name,
                    channelName = name,
                    handle = "@${currentEmail.substringBefore("@")}",
                    avatarUrl = avatar,
                    isActive = true,
                    cookie = cookie,
                )
            )
        } else {
            list.map {
                val cleanDisplayName = if (it.displayName.isBlank() || it.displayName.equals("Pengguna Google", ignoreCase = true) || it.displayName.equals("Google User", ignoreCase = true) || it.displayName.equals("Akun Google Terhubung", ignoreCase = true) || it.displayName.equals("Connected Google Account", ignoreCase = true)) {
                    com.asla.denge.util.GoogleAccountPicker.formatDisplayName(it.email)
                } else it.displayName
                it.copy(
                    displayName = cleanDisplayName,
                    channelName = cleanDisplayName,
                    isActive = it.email.equals(currentEmail, ignoreCase = true)
                )
            }
        }

        _accountsFlow.value = updated
        return updated
    }

    private fun persistAccounts(accounts: List<UserAccount>) {
        _accountsFlow.value = accounts
        try {
            val raw = json.encodeToString(accounts)
            prefs.edit().putString(KEY_SAVED_ACCOUNTS, raw).apply()
        } catch (_: Exception) {}
    }

    override suspend fun isAuthenticated(): Boolean {
        return !getToken().isNullOrBlank()
    }

    override suspend fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    override fun getAuthState(): Flow<Boolean> {
        return _authState.asStateFlow()
    }

    override fun getSavedAccounts(): Flow<List<UserAccount>> {
        return _accountsFlow.asStateFlow()
    }

    override suspend fun storeToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        _authState.value = true
    }

    override suspend fun signOut() {
        val currentAccounts = _accountsFlow.value.map { it.copy(isActive = false) }
        persistAccounts(currentAccounts)

        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_AVATAR)
            .remove(KEY_AUTH_COOKIE)
            .apply()
        _authState.value = false
    }

    override suspend fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    override suspend fun getUserName(): String? {
        val rawName = prefs.getString(KEY_USER_NAME, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        if (rawName.isNullOrBlank() || rawName.equals("Pengguna Google", ignoreCase = true) || rawName.equals("Google User", ignoreCase = true) || rawName.equals("Akun Google Terhubung", ignoreCase = true) || rawName.equals("Connected Google Account", ignoreCase = true) || rawName.equals("Sobat Musik", ignoreCase = true)) {
            if (!email.isNullOrBlank()) {
                val formatted = com.asla.denge.util.GoogleAccountPicker.formatDisplayName(email)
                prefs.edit().putString(KEY_USER_NAME, formatted).apply()
                return formatted
            }
            return "Music Lover"
        }
        return rawName
    }

    override fun hasCustomUserName(): Boolean {
        val name = prefs.getString(KEY_USER_NAME, null)
        return !name.isNullOrBlank() && !name.equals("Pengguna Google", ignoreCase = true) && !name.equals("Google User", ignoreCase = true) && !name.equals("Akun Google Terhubung", ignoreCase = true) && !name.equals("Connected Google Account", ignoreCase = true) && !name.equals("Sobat Musik", ignoreCase = true)
    }

    override suspend fun setUserName(name: String) {
        val clean = name.trim().ifBlank { "Music Lover" }
        prefs.edit().putString(KEY_USER_NAME, clean).apply()
    }

    override suspend fun getUserAvatar(): String? {
        return prefs.getString(KEY_USER_AVATAR, null)
    }

    override suspend fun getAuthCookie(): String? {
        return prefs.getString(KEY_AUTH_COOKIE, null)
    }

    override suspend fun storeUser(email: String, name: String, avatar: String?, cookie: String?) {
        val cleanName = if (name.isBlank() || name.equals("Pengguna Google", ignoreCase = true) || name.equals("Google User", ignoreCase = true) || name.equals("Akun Google Terhubung", ignoreCase = true) || name.equals("Connected Google Account", ignoreCase = true)) {
            com.asla.denge.util.GoogleAccountPicker.formatDisplayName(email)
        } else {
            name.trim()
        }
        val handle = "@${email.substringBefore("@")}"
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, cookie ?: "active_user_token")
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, cleanName)
            if (avatar != null) putString(KEY_USER_AVATAR, avatar) else remove(KEY_USER_AVATAR)
            if (cookie != null) putString(KEY_AUTH_COOKIE, cookie) else remove(KEY_AUTH_COOKIE)
        }.apply()
        _authState.value = true

        val currentAccounts = _accountsFlow.value.toMutableList()
        val existingIndex = currentAccounts.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        val updatedAccount = UserAccount(
            email = email,
            displayName = cleanName,
            channelName = cleanName,
            handle = handle,
            avatarUrl = avatar,
            isActive = true,
            cookie = cookie,
        )

        // Mark all others inactive
        for (i in 0 until currentAccounts.size) {
            currentAccounts[i] = currentAccounts[i].copy(isActive = false)
        }

        if (existingIndex >= 0) {
            currentAccounts[existingIndex] = updatedAccount
        } else {
            currentAccounts.add(0, updatedAccount)
        }
        persistAccounts(currentAccounts)
    }

    override suspend fun switchAccount(email: String) {
        val currentAccounts = _accountsFlow.value.toMutableList()
        val target = currentAccounts.find { it.email.equals(email, ignoreCase = true) } ?: return

        for (i in 0 until currentAccounts.size) {
            currentAccounts[i] = currentAccounts[i].copy(
                isActive = currentAccounts[i].email.equals(email, ignoreCase = true)
            )
        }
        persistAccounts(currentAccounts)

        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, target.cookie ?: "active_user_token")
            putString(KEY_USER_EMAIL, target.email)
            putString(KEY_USER_NAME, target.displayName)
            if (target.avatarUrl != null) putString(KEY_USER_AVATAR, target.avatarUrl) else remove(KEY_USER_AVATAR)
            if (target.cookie != null) putString(KEY_AUTH_COOKIE, target.cookie) else remove(KEY_AUTH_COOKIE)
        }.apply()

        _authState.value = true
    }

    override suspend fun saveAccount(account: UserAccount) {
        val currentAccounts = _accountsFlow.value.toMutableList()
        val index = currentAccounts.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (index >= 0) {
            currentAccounts[index] = account
        } else {
            currentAccounts.add(account)
        }
        persistAccounts(currentAccounts)
    }

    override suspend fun refreshDeviceAccounts() {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            val currentAccounts = _accountsFlow.value.toMutableList()
            var modified = false

            for (acc in accounts) {
                val email = acc.name
                if (currentAccounts.none { it.email.equals(email, ignoreCase = true) }) {
                    val fallbackName = email.substringBefore("@")
                        .replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    val newAcc = UserAccount(
                        email = email,
                        displayName = fallbackName,
                        channelName = fallbackName,
                        handle = "@${email.substringBefore("@")}",
                        isActive = false,
                    )
                    currentAccounts.add(newAcc)
                    modified = true
                }
            }

            if (modified) {
                persistAccounts(currentAccounts)
            }
        } catch (_: Exception) {}
    }

    override suspend fun fetchAndSyncYouTubeAccount(cookie: String): UserAccount? {
        return try {
            val profile = innertubeClient.fetchAccountProfile(cookie)
            if (profile != null) {
                storeUser(
                    email = profile.email,
                    name = profile.name,
                    avatar = profile.avatarUrl,
                    cookie = cookie
                )
                UserAccount(
                    email = profile.email,
                    displayName = profile.name,
                    channelName = profile.name,
                    handle = profile.handle ?: "@${profile.email.substringBefore("@")}",
                    avatarUrl = profile.avatarUrl,
                    isActive = true,
                    cookie = cookie,
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "jwt_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_AUTH_COOKIE = "auth_cookie"
        private const val KEY_SAVED_ACCOUNTS = "saved_accounts_json"
    }
}
