package com.asla.denge.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import com.asla.denge.util.smoothScrollToTop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.Track
import com.asla.denge.player.RepeatMode
import com.asla.denge.ui.components.TrackArtwork
import com.asla.denge.ui.components.TrackItem
import com.asla.denge.util.getFallbackThumbnailUrl
import com.asla.denge.util.toHdThumbnailUrl
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val playerState by viewModel.playerState.collectAsState()
    val isLiked by viewModel.isCurrentTrackLiked.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val track = playerState.currentTrack ?: return
    val durationMs = playerState.durationMs
    val queue = playerState.queue
    val currentIndex = playerState.currentIndex

    val sleepTimerRemaining by viewModel.sleepTimerRemainingSeconds.collectAsState()
    val activeSleepTimerOption by viewModel.activeSleepTimerOption.collectAsState()

    var showHdPreview by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreatingNewPlaylist by remember { mutableStateOf(false) }

    val hdThumbnail = remember(track.thumbnailUrl, track.videoId) {
        toHdThumbnailUrl(track.thumbnailUrl, track.videoId).ifBlank {
            getFallbackThumbnailUrl(track.videoId)
        }
    }

    // HD Artwork Preview Dialog
    if (showHdPreview) {
        Dialog(onDismissRequest = { showHdPreview = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TrackArtwork(
                        title = track.title,
                        thumbnailUrl = track.thumbnailUrl,
                        videoId = track.videoId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(16.dp),
                        iconSize = 64.dp,
                        showBorder = true,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Share Button: copy track link directly to clipboard
                    Button(
                        onClick = {
                            val shareUrl = "https://music.youtube.com/watch?v=${track.videoId}"
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Music Link", shareUrl)
                            clipboard?.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = { showHdPreview = false },
                        ) {
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showPlaylistDialog = false
                isCreatingNewPlaylist = false
            },
            title = {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isCreatingNewPlaylist) {
                        Text(
                            text = "Enter a name for the new playlist:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            placeholder = { Text("e.g. Bedtime Tracks, Chill Beats") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Text(
                            text = "Select target playlist:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val customPlaylists = userPlaylists.filter { it.isEditable }
                        if (customPlaylists.isEmpty()) {
                            Text(
                                text = "No custom playlists yet. Tap the button below to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(customPlaylists, key = { idx, p -> "${p.playlistId}_$idx" }) { _, playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                viewModel.addCurrentTrackToPlaylist(playlist.playlistId)
                                                showPlaylistDialog = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isCreatingNewPlaylist) {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylistAndAddCurrent(newPlaylistName.trim())
                                newPlaylistName = ""
                                isCreatingNewPlaylist = false
                                showPlaylistDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Save")
                    }
                } else {
                    Button(
                        onClick = { isCreatingNewPlaylist = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create New Playlist")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isCreatingNewPlaylist) {
                            isCreatingNewPlaylist = false
                        } else {
                            showPlaylistDialog = false
                        }
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    // Sleep Timer Dialog (Modern slider matching Archive Tunes aesthetic)
    if (showSleepTimerDialog) {
        com.asla.denge.ui.components.SleepTimerDialog(
            initialMinutes = activeSleepTimerOption,
            onDismissRequest = { showSleepTimerDialog = false },
            onSetTimer = { mins -> viewModel.setSleepTimer(mins) },
            onResetTimer = { viewModel.cancelSleepTimer() },
        )
    }

    var showQueueSheet by remember { mutableStateOf(false) }

    // Intercept system back button while PlayerScreen is active:
    // Close queue sheet first if opened; otherwise collapse Now Playing without popping background destination
    BackHandler(enabled = true) {
        if (showQueueSheet) {
            showQueueSheet = false
        } else {
            onDismiss()
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close Player",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Keep center title aligned with balanced spacer
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Large Album Art with HD Preview on Click
                TrackArtwork(
                    title = track.title,
                    thumbnailUrl = track.thumbnailUrl,
                    videoId = track.videoId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { showHdPreview = true },
                    shape = RoundedCornerShape(26.dp),
                    iconSize = 96.dp,
                    showBorder = true,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Title, Artist, and Action Icons (Like & Add to Playlist)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track.artistName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Like Song Button
                        IconButton(onClick = {
                            val willLike = !isLiked
                            viewModel.toggleLikeCurrentTrack()
                            android.widget.Toast.makeText(
                                context,
                                if (willLike) "Added to Favorites" else "Removed from Favorites",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        // Add to Playlist Button
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Progress Slider
                PlayerProgressSection(
                    viewModel = viewModel,
                    durationMs = durationMs,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Playback Controls (Shuffle, Prev, Play/Pause FAB, Next, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playerState.isShuffleEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (playerState.isShuffleEnabled) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }

                    // Previous
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Play / Pause FAB
                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }

                    // Next
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        val isRepeatActive = playerState.repeatMode != RepeatMode.OFF
                        val icon = if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Repeat",
                                tint = if (isRepeatActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isRepeatActive) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }

            // 6. Bottom Action Bar: Queue elongated pill + Circular Timer button
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val bottomBarContainerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val bottomBarBorderColor = if (isDark) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pill Button: [ Queue (Count) ] elongated close to timer
                Surface(
                    onClick = { showQueueSheet = true },
                    shape = RoundedCornerShape(24.dp),
                    color = bottomBarContainerColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        bottomBarBorderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Open Queue",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (queue.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "${queue.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Circular Stopwatch/Timer Button with active underline pill
                val isTimerActive = sleepTimerRemaining != null
                Surface(
                    onClick = { showSleepTimerDialog = true },
                    shape = CircleShape,
                    color = if (isTimerActive) MaterialTheme.colorScheme.primaryContainer else bottomBarContainerColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTimerActive) MaterialTheme.colorScheme.primary else bottomBarBorderColor
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Timer",
                            modifier = Modifier.size(22.dp),
                            tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isTimerActive) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // 7. Modal Bottom Sheet for Queue
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "${queue.size} Tracks",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )

                    val queueListState = rememberLazyListState()
                    val showQueueScrollToTop by remember {
                        derivedStateOf { queueListState.firstVisibleItemIndex >= 5 }
                    }

                    androidx.compose.runtime.LaunchedEffect(currentIndex) {
                        if (currentIndex in queue.indices) {
                            queueListState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        LazyColumn(
                            state = queueListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        ) {
                            itemsIndexed(
                                items = queue,
                                key = { idx, item -> "sheet_queue_${idx}_${item.videoId}" }
                            ) { idx, qTrack ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.removeFromQueue(idx)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                                        val isStartToEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                                        val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSwiping) MaterialTheme.colorScheme.errorContainer
                                                    else Color.Transparent
                                                )
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = alignment,
                                        ) {
                                            if (isSwiping) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove from Queue",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                    },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                ) {
                                    TrackItem(
                                        track = qTrack,
                                        isPlaying = idx == currentIndex,
                                        onClick = {
                                            viewModel.playTrackAt(idx)
                                        },
                                        onPlayNext = {
                                            viewModel.playNextInQueue(qTrack)
                                        },
                                        onAddToQueue = {
                                            viewModel.addToQueue(qTrack)
                                        },
                                        onRemoveFromQueue = {
                                            viewModel.removeFromQueue(idx)
                                        },
                                        onMoveUp = if (idx > 0) {
                                            { viewModel.moveQueueItem(idx, idx - 1) }
                                        } else null,
                                        onMoveDown = if (idx < queue.size - 1) {
                                            { viewModel.moveQueueItem(idx, idx + 1) }
                                        } else null,
                                        dragHandle = {
                                            var accumulatedY by remember { mutableFloatStateOf(0f) }
                                            val density = LocalDensity.current
                                            val stepPx = remember(density) { with(density) { 56.dp.toPx() } }
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Reorder track",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .padding(6.dp)
                                                    .pointerInput(qTrack.videoId, idx, queue.size) {
                                                        detectDragGestures(
                                                            onDragStart = { accumulatedY = 0f },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                accumulatedY += dragAmount.y
                                                                if (accumulatedY > stepPx && idx < queue.size - 1) {
                                                                    viewModel.moveQueueItem(idx, idx + 1)
                                                                    accumulatedY = 0f
                                                                } else if (accumulatedY < -stepPx && idx > 0) {
                                                                    viewModel.moveQueueItem(idx, idx - 1)
                                                                    accumulatedY = 0f
                                                                }
                                                            },
                                                            onDragEnd = { accumulatedY = 0f },
                                                            onDragCancel = { accumulatedY = 0f }
                                                        )
                                                    }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }

                            if (playerState.isLoadingRadio) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading similar tracks...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else if (currentIndex >= queue.size - 1) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.fetchRadioTracks() },
                                        ) {
                                            Text("Load Similar Tracks")
                                        }
                                    }
                                }
                            }
                        }

                        // Scroll to top FAB for long queue lists
                        if (showQueueScrollToTop) {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        queueListState.smoothScrollToTop()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 24.dp, end = 12.dp)
                                    .size(46.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Scroll to top of queue",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerProgressSection(
    viewModel: PlayerViewModel,
    durationMs: Long,
) {
    val currentMs by viewModel.currentPositionMs.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (durationMs > 0L) {
        currentMs.toFloat() / durationMs.toFloat()
    } else {
        0f
    }
    val safeProgress = if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
    val seekRatio = if (durationMs > 0L) (seekPosition / durationMs.toFloat()) else 0f
    val safeSeekProgress = if (seekRatio.isNaN() || seekRatio.isInfinite()) 0f else seekRatio.coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (isSeeking) safeSeekProgress else safeProgress,
            onValueChange = { frac ->
                isSeeking = true
                seekPosition = frac * durationMs
            },
            onValueChangeFinished = {
                viewModel.seekTo(seekPosition.toLong())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(if (isSeeking) seekPosition.toLong() else currentMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
