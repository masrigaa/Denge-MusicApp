package com.asla.denge.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AdsFreeMusic color palette — Brew and Bean Coffee DNA.
 * Warm retail coffee aesthetic: rich espresso, warm caramel, tan accents,
 * soft cream canvas, and tactile card surfaces.
 */

// ── Primary (Espresso) ──
val EspressoBrown = Color(0xFF3E2A1E)     // Primary action & active ink
val EspressoDark = Color(0xFF2C1A10)      // Deepest roast for primary text
val OnEspresso = Color(0xFFF4EBE1)        // Steamed milk cream

// ── Secondary & Accents (Caramel & Tan) ──
val CaramelBrown = Color(0xFF73513C)      // Secondary roast
val TanAccent = Color(0xFFA88569)         // Warm accent & borders
val GoldenCaramel = Color(0xFFD49B55)     // Highlight & stars/ratings

// ── Surfaces & Canvas (Cream & Steamed Milk) ──
val CoffeeCanvas = Color(0xFFF7F2EB)      // Warm cream page background
val CoffeeSurface = Color(0xFFFFFFFF)     // Clean white/cream card surface
val CoffeeCard = Color(0xFFF5ECE2)        // Soft cream card layer
val CoffeeSurfaceMuted = Color(0xFFEADCCF) // Muted container / chip background
val CoffeeHeader = Color(0xFF3E2A1E)      // Deep espresso header or contrast banner
val CoffeeHeaderWarm = Color(0xFFF0E5D8)  // Warm light coffee header

// ── Foreground / Text ──
val TextEspresso = Color(0xFF2C1A10)      // High contrast heading & text
val TextMuted = Color(0xFF7E6958)         // Warm secondary text
val TextPlaceholder = Color(0xFFA8988B)   // Placeholder & disabled

// ── Border & Dividers ──
val CoffeeBorder = Color(0xFFE0D2C3)      // Hairline warm border
val CoffeeBorderSubtle = Color(0xFFEFE6DC)// Very subtle divider

// ── Status ──
val CoffeeDanger = Color(0xFFB84A32)      // Warm terracotta error
val CoffeeDangerBg = Color(0xFFFBECE9)
val CoffeeSuccess = Color(0xFF388E3C)
val CoffeeSuccessBg = Color(0xFFE8F5E9)

/**
 * Semantic role-based colors for Déngé design system.
 * Supports both Light (Brew & Bean) and Dark (Arang hangat) palettes.
 */
data class DengeColors(
    val canvas: Color,
    val surface: Color,
    val card: Color,
    val surfaceMuted: Color,
    val header: Color,
    val headerWarm: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val accent: Color,
    val highlight: Color,
    val text: Color,
    val textMuted: Color,
    val textPlaceholder: Color,
    val border: Color,
    val borderSubtle: Color,
    val danger: Color,
    val dangerBg: Color,
    val success: Color,
    val successBg: Color,
)

val LightDengeColors = DengeColors(
    canvas = CoffeeCanvas,
    surface = CoffeeSurface,
    card = CoffeeCard,
    surfaceMuted = CoffeeSurfaceMuted,
    header = CoffeeHeader,
    headerWarm = CoffeeHeaderWarm,
    primary = EspressoBrown,
    onPrimary = OnEspresso,
    secondary = CaramelBrown,
    accent = TanAccent,
    highlight = GoldenCaramel,
    text = TextEspresso,
    textMuted = TextMuted,
    textPlaceholder = TextPlaceholder,
    border = CoffeeBorder,
    borderSubtle = CoffeeBorderSubtle,
    danger = CoffeeDanger,
    dangerBg = CoffeeDangerBg,
    success = CoffeeSuccess,
    successBg = CoffeeSuccessBg,
)

/**
 * "Arang hangat" Dark Theme palette.
 * Warm charcoal aesthetic preserving the coffee soul without harsh pure blacks.
 */
val DarkDengeColors = DengeColors(
    canvas = Color(0xFF161514),
    surface = Color(0xFF1F1D1B),
    card = Color(0xFF282522),
    surfaceMuted = Color(0xFF34302C),
    header = CoffeeHeader, // #3E2A1E
    headerWarm = Color(0xFF2E2A26),
    primary = TanAccent, // #A88569
    onPrimary = TextEspresso, // #2C1A10
    secondary = Color(0xFFC19A78),
    accent = TanAccent,
    highlight = GoldenCaramel,
    text = OnEspresso, // #F4EBE1
    textMuted = Color(0xFFB3A99E),
    textPlaceholder = Color(0xFF857B72),
    border = Color(0xFF3A3632),
    borderSubtle = Color(0xFF2E2B28),
    danger = Color(0xFFE57C63),
    dangerBg = Color(0xFF3A2521),
    success = Color(0xFF81C784),
    successBg = Color(0xFF1F3322),
)

val LocalDengeColors = androidx.compose.runtime.staticCompositionLocalOf { LightDengeColors }


