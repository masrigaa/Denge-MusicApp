package com.asla.denge.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asla.denge.R
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.Track
import com.asla.denge.ui.components.TrackItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val history by viewModel.history.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Liked Songs", "Downloaded", "History")

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }

    // Dialog Create Playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    text = "Create New Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for your custom playlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("e.g. Chill Beats, Acoustic Vibes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Create Playlist")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    // Modal Detail Playlist Tracks
    selectedPlaylistForDetail?.let { playlist ->
        val playlistTracks by viewModel.getPlaylistTracks(playlist.playlistId).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { selectedPlaylistForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (playlist.isEditable) {
                        IconButton(
                            onClick = {
                                viewModel.deletePlaylist(playlist.playlistId)
                                selectedPlaylistForDetail = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Playlist",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (playlistTracks.size == 1) "1 track in this playlist" else "${playlistTracks.size} tracks in this playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (playlistTracks.isEmpty()) {
                        Text(
                            text = "No tracks in this playlist yet. Add songs from Player or recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(playlistTracks, key = { idx, item -> "${playlist.playlistId}_${item.videoId}_$idx" }) { _, track ->
                                TrackItem(
                                    track = track,
                                    onClick = { viewModel.playTrack(track, playlistTracks) },
                                    onPlayNext = { viewModel.playNext(track) },
                                    onAddToQueue = { viewModel.addToQueue(track) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (playlistTracks.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.playTrack(playlistTracks.first(), playlistTracks)
                            selectedPlaylistForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlaylistForDetail = null }) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Brew & Bean header with soft curve
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
                    Text(
                        text = stringResource(R.string.nav_library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Playlists, liked tracks & history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Playlists tab
                        if (playlists.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                title = "No Playlists Yet",
                                description = "Create your first playlist using the + button below.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                                itemsIndexed(playlists, key = { idx, item -> "pl_${item.playlistId}_$idx" }) { _, playlist ->
                                    PlaylistItemView(
                                        playlist = playlist,
                                        onClick = { selectedPlaylistForDetail = playlist }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(88.dp)) }
                            }
                        }
                    }
                    1 -> {
                        // Liked songs tab
                        if (likedSongs.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Favorite,
                                title = "No Liked Songs Yet",
                                description = "Tap the heart ❤️ button in the player to add your favorite songs here.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (likedSongs.size == 1) "1 Liked Song" else "${likedSongs.size} Liked Songs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Button(
                                            onClick = { viewModel.playTrack(likedSongs.first(), likedSongs) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play All")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                itemsIndexed(likedSongs, key = { idx, item -> "liked_${item.videoId}_$idx" }) { _, track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { viewModel.playTrack(track, likedSongs) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(88.dp)) }
                            }
                        }
                    }
                    2 -> {
                        // Downloaded songs tab (Offline)
                        if (downloadedTracks.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Download,
                                title = "No Downloaded Songs",
                                description = "Download songs from the player to listen offline anytime without internet.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (downloadedTracks.size == 1) "1 Offline Track" else "${downloadedTracks.size} Offline Tracks",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Button(
                                            onClick = { viewModel.playDownloadedTrack(downloadedTracks.first(), downloadedTracks) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play All")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                itemsIndexed(downloadedTracks, key = { idx, item -> "dl_${item.videoId}_$idx" }) { _, track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { viewModel.playDownloadedTrack(track, downloadedTracks) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                        onDelete = { viewModel.deleteDownloadedTrack(track.videoId) },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(88.dp)) }
                            }
                        }
                    }
                    3 -> {
                        // History tab - 100% safe from duplicate keys
                        if (history.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.History,
                                title = "No Listening History",
                                description = "Songs you play will automatically appear here.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (history.size == 1) "1 Recently Played Song" else "${history.size} Recently Played Songs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Button(
                                            onClick = { viewModel.playTrack(history.first(), history) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play All")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                itemsIndexed(history, key = { idx, item -> "lib_hist_${item.videoId}_$idx" }) { _, track ->
                                    TrackItem(
                                        track = track,
                                        onClick = { viewModel.playTrack(track, history) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(88.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Floating button to create playlist when on tab 0 (Playlists) - perfectly placed right above navbar/miniplayer
        if (selectedTabIndex == 0) {
            FloatingActionButton(
                onClick = {
                    newPlaylistName = ""
                    showCreatePlaylistDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Playlist", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemView(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (playlist.trackCount == 1) "1 track" else "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(actionText)
            }
        }
    }
}
