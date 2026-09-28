package com.asla.denge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.asla.denge.util.toHdThumbnailUrl

/**
 * Universal Track Artwork component.
 * Guarantees that no track or video ever displays as an empty or blank box.
 * Displays a sleek Brew & Bean vinyl sleeve gradient with title initial watermark
 * as permanent fallback behind Coil's AsyncImage.
 */
@Composable
fun TrackArtwork(
    title: String,
    thumbnailUrl: String?,
    videoId: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    iconSize: Dp = 24.dp,
    showBorder: Boolean = false,
) {
    val resolvedUrl = remember(thumbnailUrl, videoId) {
        toHdThumbnailUrl(thumbnailUrl, videoId)
    }

    val initial = remember(title) {
        title.trim().take(1).uppercase().ifBlank { "♪" }
    }

    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A3728), // Warm espresso
                Color(0xFF7B5E43), // Rich caramel wood
                Color(0xFF2E1C14), // Dark roast
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientBrush)
            .then(
                if (showBorder) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Aesthetic Fallback / Placeholder (always present behind the AsyncImage)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Subtle initial letter in center
            Text(
                text = initial,
                fontSize = (iconSize.value * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD7CCC8).copy(alpha = 0.45f),
            )
            // Foreground music icon
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFFFFD180).copy(alpha = 0.75f),
                modifier = Modifier.size(iconSize),
            )
        }

        // Live artwork image
        if (resolvedUrl.isNotBlank()) {
            val isYtVideoThumbnail = remember(resolvedUrl) {
                resolvedUrl.contains("ytimg.com") || resolvedUrl.contains("youtube.com")
            }
            AsyncImage(
                model = resolvedUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isYtVideoThumbnail) {
                            Modifier.graphicsLayer {
                                scaleX = 1.34f
                                scaleY = 1.34f
                            }
                        } else Modifier
                    ),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
