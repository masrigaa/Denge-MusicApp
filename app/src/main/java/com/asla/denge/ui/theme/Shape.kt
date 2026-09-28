package com.asla.denge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens from DNA radius scale.
 *
 * DNA spec:
 * - sheet: 40dp (header curve, bottom sheets)
 * - hero: 24dp (album art cards, full player card)
 * - card: 20dp (standard cards)
 * - tile: 16dp (inputs, small tiles)
 * - key: 12dp (icon buttons)
 * - chip: full pill (chips, dots, avatars, toggles)
 */
val AdsFreeShapes = Shapes(
    // key: 12dp — icon tiles, small elements
    extraSmall = RoundedCornerShape(12.dp),
    // tile: 16dp — inputs, small tiles
    small = RoundedCornerShape(16.dp),
    // card: 20dp — standard cards, playlist cards
    medium = RoundedCornerShape(20.dp),
    // hero: 24dp — album art, full player
    large = RoundedCornerShape(24.dp),
    // sheet: 40dp — bottom sheets, header curve
    extraLarge = RoundedCornerShape(40.dp),
)

/** Full pill radius for chips, badges, toggle knobs */
val PillShape = RoundedCornerShape(percent = 50)
