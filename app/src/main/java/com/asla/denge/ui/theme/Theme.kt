package com.asla.denge.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Brew and Bean Coffee DNA — Light Warm Coffee Theme.
 * Anchored by deep espresso, caramel warmth, cream canvas, and soft tactile surfaces.
 */
private val CoffeeColorScheme = lightColorScheme(
    primary = EspressoBrown,
    onPrimary = OnEspresso,
    secondary = CaramelBrown,
    onSecondary = OnEspresso,
    tertiary = TanAccent,
    background = CoffeeCanvas,
    onBackground = TextEspresso,
    surface = CoffeeSurface,
    onSurface = TextEspresso,
    surfaceVariant = CoffeeCard,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = CoffeeCanvas,
    surfaceContainerLow = CoffeeCard,
    surfaceContainer = CoffeeSurfaceMuted,
    surfaceContainerHigh = CoffeeSurfaceMuted,
    surfaceContainerHighest = CoffeeSurfaceMuted,
    outline = CoffeeBorder,
    outlineVariant = CoffeeBorderSubtle,
    error = CoffeeDanger,
    onError = Color.White,
    errorContainer = CoffeeDangerBg,
)

@Composable
fun AdsFreeTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = CoffeeColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: (view.context as? android.content.ContextWrapper)?.baseContext as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AdsFreeTypography,
        shapes = AdsFreeShapes,
        content = content,
    )
}

