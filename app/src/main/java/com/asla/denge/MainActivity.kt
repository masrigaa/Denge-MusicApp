package com.asla.denge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.asla.denge.domain.model.ThemeMode
import com.asla.denge.domain.repository.SettingsRepository
import com.asla.denge.player.PlayerManager
import com.asla.denge.ui.navigation.AdsFreeNavHost
import com.asla.denge.ui.theme.DengeTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val playerManager: PlayerManager by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled
        }

    override fun onStart() {
        super.onStart()
        playerManager.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        playerManager.setAppInForeground(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Synchronously match window background to user/system theme to eliminate cold-start white/cream flash
        val syncThemeMode = settingsRepository.getThemeModeSync()
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = when (syncThemeMode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val initialBgColor = if (initialIsDark) android.graphics.Color.parseColor("#161514") else android.graphics.Color.parseColor("#F7F2EB")
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(initialBgColor))
        window.decorView.setBackgroundColor(initialBgColor)

        requestNotificationPermissionIfNeeded()

        setContent {
            val themeMode by settingsRepository.getThemeMode()
                .collectAsState(initial = settingsRepository.getThemeModeSync())
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DengeTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AdsFreeNavHost()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
