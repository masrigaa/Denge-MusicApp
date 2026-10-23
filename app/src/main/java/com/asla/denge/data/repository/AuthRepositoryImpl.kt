package com.asla.denge.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.asla.denge.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val context: Context,
) : AuthRepository {

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

    override fun hasCustomUserName(): Boolean {
        val name = prefs.getString(KEY_USER_NAME, null)
        return !name.isNullOrBlank() && !name.equals("Sobat Musik", ignoreCase = true)
    }

    override suspend fun getUserName(): String? {
        val rawName = prefs.getString(KEY_USER_NAME, null)
        if (rawName.isNullOrBlank() || rawName.equals("Sobat Musik", ignoreCase = true)) {
            return "Music Lover"
        }
        return rawName
    }

    override suspend fun setUserName(name: String) {
        val clean = name.trim().ifBlank { "Music Lover" }
        prefs.edit().putString(KEY_USER_NAME, clean).apply()
    }

    override suspend fun getAuthCookie(): String? {
        return prefs.getString(KEY_AUTH_COOKIE, null)
    }

    override suspend fun signOut() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_AUTH_COOKIE)
            .apply()
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AUTH_COOKIE = "auth_cookie"
    }
}
