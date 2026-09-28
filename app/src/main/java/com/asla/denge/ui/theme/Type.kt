package com.asla.denge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography scale following DNA voice specification.
 * Uses system default font (can be replaced with Inter via Google Fonts dependency).
 *
 * DNA roles mapped to Material3 slots:
 * - display → displaySmall
 * - heading1 → headlineMedium
 * - heading2 → headlineSmall
 * - heading3 → titleMedium
 * - body → bodyLarge
 * - bodySmall → bodyMedium
 * - caption → bodySmall
 * - micro → labelSmall
 */
val AdsFreeTypography = Typography(
    // display: 20sp weight 900 line 1.25 — Now Playing track title
    displaySmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    // heading1: 17sp weight 900 line 1.35 — Screen hero headlines
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    // heading2: 17sp weight 700 tracking 0.025em — Dark header titles
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.43.sp,
    ),
    // heading3: 14sp weight 900 — Card/section titles
    titleMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // body: 13sp weight 500-600 line 1.6 — Descriptions, artist names
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 21.sp,
    ),
    // bodySmall: 11sp weight 600 — Meta info, durations
    bodyMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 17.sp,
    ),
    // caption: 10sp weight 600 — Counts, labels
    bodySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
    // micro: 10sp weight 900 uppercase tracking 0.05em — Status labels
    labelSmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
    ),
)
