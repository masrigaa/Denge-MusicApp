package com.asla.denge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Maps semantic DengeColors into Material 3 ColorScheme so standard
 * Material components (Dialogs, Sheets, Cards, Menus) match the brand palette.
 */
private fun dengeColorsToColorScheme(denge: DengeColors, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = denge.primary,
            onPrimary = denge.onPrimary,
            primaryContainer = Color(0xFF382C24),
            onPrimaryContainer = denge.primary,
            secondary = denge.secondary,
            onSecondary = denge.onPrimary,
            secondaryContainer = denge.surfaceMuted,
            onSecondaryContainer = denge.text,
            tertiary = denge.accent,
            onTertiary = denge.onPrimary,
            tertiaryContainer = denge.card,
            onTertiaryContainer = denge.text,
            background = denge.canvas,
            onBackground = denge.text,
            surface = denge.surface,
            onSurface = denge.text,
            surfaceVariant = denge.card,
            onSurfaceVariant = denge.textMuted,
            surfaceContainerLowest = denge.canvas,
            surfaceContainerLow = denge.surface,
            surfaceContainer = denge.card,
            surfaceContainerHigh = denge.card,
            surfaceContainerHighest = denge.surfaceMuted,
            outline = denge.border,
            outlineVariant = denge.borderSubtle,
            error = denge.danger,
            onError = Color.White,
            errorContainer = denge.dangerBg,
        )
    } else {
        lightColorScheme(
            primary = denge.primary,
            onPrimary = denge.onPrimary,
            primaryContainer = Color(0xFFEDE0D4),
            onPrimaryContainer = denge.primary,
            secondary = denge.secondary,
            onSecondary = denge.onPrimary,
            secondaryContainer = denge.surfaceMuted,
            onSecondaryContainer = denge.text,
            tertiary = denge.accent,
            onTertiary = denge.onPrimary,
            tertiaryContainer = denge.card,
            onTertiaryContainer = denge.text,
            background = denge.canvas,
            onBackground = denge.text,
            surface = denge.surface,
            onSurface = denge.text,
            surfaceVariant = denge.card,
            onSurfaceVariant = denge.textMuted,
            surfaceContainerLowest = denge.canvas,
            surfaceContainerLow = denge.card,
            surfaceContainer = denge.surfaceMuted,
            surfaceContainerHigh = denge.card,
            surfaceContainerHighest = denge.surfaceMuted,
            outline = denge.border,
            outlineVariant = denge.borderSubtle,
            error = denge.danger,
            onError = Color.White,
            errorContainer = denge.dangerBg,
        )
    }
}

/**
 * Déngé Design Theme.
 * Supports Light ("Brew & Bean" warm coffee) and Dark ("Warm Charcoal" deep coffee).
 */
@Composable
fun DengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dengeColors = if (darkTheme) DarkDengeColors else LightDengeColors
    val colorScheme = dengeColorsToColorScheme(dengeColors, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
                ?: (view.context as? android.content.ContextWrapper)?.baseContext as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDengeColors provides dengeColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AdsFreeTypography,
            shapes = AdsFreeShapes,
            content = content,
        )
    }
}

@Composable
fun AdsFreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    DengeTheme(darkTheme = darkTheme, content = content)
}


