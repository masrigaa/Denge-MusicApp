package com.asla.denge.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asla.denge.ui.components.TrackItem
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.asla.denge.ui.components.GenreSelectionDialog

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.recentHistory.collectAsState()
    val mostPlayed by viewModel.mostPlayedTracks.collectAsState()
    val recommendedFromLiked by viewModel.recommendedFromLiked.collectAsState()
    val userName by viewModel.userName.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        viewModel.loadGenreFeed()
    }

    val greeting = rememberGreeting()

    // 1. Onboarding Dialogs (Hanya muncul pertama kali saat baru install)
    if (uiState.showOnboarding) {
        when (uiState.onboardingStep) {
            OnboardingStep.NAME_INPUT -> {
                com.asla.denge.ui.components.NameInputDialog(
                    initialName = "",
                    onConfirm = { name ->
                        viewModel.submitOnboardingName(name)
                    },
                    onSkip = {
                        viewModel.submitOnboardingName("Sobat Musik")
                    }
                )
            }
            OnboardingStep.GENRE_SELECTION -> {
                GenreSelectionDialog(
                    genres = uiState.availableGenres,
                    onToggleGenre = { viewModel.toggleGenreSelection(it) },
                    onAddCustomGenre = { name, query -> viewModel.addCustomGenre(name, query) },
                    onDismiss = { viewModel.completeOnboarding() },
                    onConfirm = { viewModel.completeOnboarding() },
                    isFirstTimeOnboarding = true,
                )
            }
        }
    }

    AnimatedContent(
        targetState = uiState.isLoading && uiState.genreSections.isEmpty(),
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
        },
        label = "homeLoadingOrContent",
        modifier = modifier.fillMaxSize(),
    ) { isLoading ->
        if (isLoading) {
            HomeWaveLoadingBox()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
        // Brew & Bean warm header with soft curve
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (userName.isNotBlank()) "Halo, $userName 🎵" else "$greeting 🎵",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Déngé • Musik Tanpa Batas & Bebas Iklan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // User Avatar Circle with Initial
                    val initial = userName.trim().take(1).uppercase().ifBlank { "M" }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        // Body content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.genreSections.isEmpty() && uiState.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.error ?: "Gagal memuat rekomendasi musik",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadGenreFeed() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Coba Lagi")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Baru Saja Diputar (1-3 lagu terakhir)
                    if (history.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Baru Saja Diputar",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Lanjutkan mendengarkan lagu favorit Anda",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                history.take(3).forEach { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { viewModel.playTrack(track, history) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                    )
                                }
                            }
                        }
                    }

                    // 2. Sering Kamu Putar (Most Played)
                    if (mostPlayed.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Sering Kamu Putar 🔥",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Lagu yang paling sering menemani harimu",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.playTrack(mostPlayed.first(), mostPlayed) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    mostPlayed.take(2).forEach { track ->
                                        TrackItem(
                                            track = track,
                                            onClick = { viewModel.playTrack(track, mostPlayed) },
                                            onPlayNext = { viewModel.playNext(track) },
                                            onAddToQueue = { viewModel.addToQueue(track) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Karena Kamu Menyukai... (Rekomendasi Berdasarkan Lagu Favorit)
                    val recPair = recommendedFromLiked
                    if (recPair != null && recPair.second.isNotEmpty()) {
                        val (likedTrack, recTracks) = recPair
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Karena Menyukai \"${likedTrack.title}\"",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "Rekomendasi mirip selera favoritmu 💖",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.playTrack(recTracks.first(), recTracks) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recTracks.take(4).forEach { track ->
                                        TrackItem(
                                            track = track,
                                            onClick = { viewModel.playTrack(track, recTracks) },
                                            onPlayNext = { viewModel.playNext(track) },
                                            onAddToQueue = { viewModel.addToQueue(track) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Genre sections (pilihan pengguna)
                    itemsIndexed(uiState.genreSections, key = { idx, section -> "${section.id}_$idx" }) { _, section ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = section.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                if (section.tracks.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.playTrack(section.tracks.first(), section.tracks) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                section.tracks.forEach { track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { viewModel.playTrack(track, section.tracks) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..11 -> "Selamat pagi"
        in 12..16 -> "Selamat siang"
        in 17..20 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

/**
 * Centered wave loading card with top and bottom accent lines forming an elegant framed box,
 * displaying "Déngé" and wave animated "Please wait...".
 */
@Composable
private fun HomeWaveLoadingBox(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "waveTransition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // Seamless large Déngé branding directly on background
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Garis atas (lebih tebal dan proporsional)
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Tulisan Déngé (lebih besar, mantap, dan tegas)
                Text(
                    text = "Déngé",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 4.sp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Garis bawah (lebih tebal dan proporsional)
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Teks animasi gelombang per huruf "Please wait..." (lebih jelas & tebal)
            val text = "Please wait..."
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                text.forEachIndexed { index, char ->
                    val charOffset = kotlin.math.sin(phase + index * 0.42f) * 5f
                    val charAlpha = (0.60f + kotlin.math.sin(phase + index * 0.42f) * 0.40f).coerceIn(0.45f, 1f)

                    Text(
                        text = char.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = charAlpha),
                        modifier = Modifier.graphicsLayer {
                            translationY = charOffset
                        },
                    )
                }
            }
        }
    }
}

