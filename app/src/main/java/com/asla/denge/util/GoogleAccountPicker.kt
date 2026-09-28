package com.asla.denge.util

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

object GoogleAccountPicker {

    /**
     * Create the official Android System Google Account Chooser intent.
     * This opens the native Google Play Services bottom sheet ("Pilih akun untuk melanjutkan ke [App]").
     */
    fun createChooseAccountIntent(): Intent {
        val googleAccountTypes = arrayOf("com.google")
        return AccountManager.newChooseAccountIntent(
            null,
            null as ArrayList<Account>?,
            googleAccountTypes,
            null,
            null,
            null,
            null
        )
    }

    /**
     * Format a clean, human-readable display name from a Google email address.
     * e.g. "aslamfikri54@gmail.com" -> "Asla Fikri" or "Aslam Fikri"
     */
    fun formatDisplayName(email: String): String {
        val prefix = email.substringBefore("@")
        // Split common separations (dots, underscores, hyphens)
        var clean = prefix.replace(Regex("[._\\-]+"), " ")
        // Remove trailing digits (e.g. "aslamfikri54" -> "aslamfikri")
        clean = clean.replace(Regex("""(\D+)\d+$"""), "$1").trim()

        // Separate common compound Indonesian & global names joined without spaces
        if (!clean.contains(" ")) {
            val commonSeparators = listOf(
                "fikri", "putra", "putri", "sari", "hidayat", "pratama", "santoso",
                "wijaya", "ramadhan", "akbar", "syah", "nur", "ananda", "kurnia", "pradana"
            )
            for (sep in commonSeparators) {
                val idx = clean.indexOf(sep, ignoreCase = true)
                if (idx > 1) {
                    clean = clean.substring(0, idx) + " " + clean.substring(idx)
                    break
                }
            }
        }

        // Separate compound names if camelCased or concatenated
        val parts = clean.split(" ").filter { it.isNotBlank() }
        val capitalized = parts.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return if (capitalized.length >= 2) capitalized else prefix.replaceFirstChar { it.uppercase() }
    }

    /**
     * Query accounts already registered on device if available.
     */
    fun getDeviceGoogleAccounts(context: Context): List<Pair<String, String>> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            accounts.map { acc ->
                val email = acc.name
                val name = formatDisplayName(email)
                email to name
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
