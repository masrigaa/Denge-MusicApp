package com.asla.denge.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asla.denge.R
import com.asla.denge.ui.components.TrackItem
import org.koin.androidx.compose.koinViewModel

private data class GenreShortcut(
    val name: String,
    val query: String,
)

/**
 * Modern Search Screen (v2.0.1).
 * Layout matches previous classic header ("Search" & "Explore music, artists & albums"),
 * with horizontal scrolling pills for genres and moods (no emojis), and zero history pollution on shortcut tap.
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    val popularGenres = remember {
        listOf(
            GenreShortcut("Pop Hits", "Pop Music Hits 2026"),
            GenreShortcut("Rock", "Classic & Modern Rock Anthems"),
            GenreShortcut("Hip-Hop", "Hip Hop Rap Hits"),
            GenreShortcut("Indie", "Indie Alternative"),
            GenreShortcut("Lo-Fi Beats", "Lofi Hip Hop Chill"),
            GenreShortcut("R&B Soul", "RnB Soul Smooth"),
            GenreShortcut("Acoustic", "Acoustic Guitar Covers"),
            GenreShortcut("Electronic", "Electronic EDM Dance"),
            GenreShortcut("Jazz", "Smooth Jazz Cafe"),
            GenreShortcut("Classical", "Classical Piano Calm"),
            GenreShortcut("Metal", "Heavy Metal Anthems"),
            GenreShortcut("K-Pop", "K-Pop Viral Hits"),
            GenreShortcut("J-Pop", "J-Pop Music Hits"),
            GenreShortcut("Reggae", "Reggae Roots Chill"),
        )
    }

    val moodVibes = remember {
        listOf(
            GenreShortcut("Focus & Study", "Study Focus Deep Ambient"),
            GenreShortcut("Night Drive", "Night Drive Synthwave"),
            GenreShortcut("Workout Energy", "Workout Motivation Music"),
            GenreShortcut("Deep Sleep", "Sleep Ambient Calm Rain"),
            GenreShortcut("Coffee Chill", "Warm Coffee Chill Acoustic"),
            GenreShortcut("Gaming", "Gaming Music Bass Boosted"),
            GenreShortcut("Rainy Mood", "Rainy Day Melancholy"),
            GenreShortcut("Feel Good", "Happy Feel Good Pop"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 1. Classic Brew & Bean Curved Header (Restored layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_search),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore music, artists & albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pill Search Bar
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it, saveToHistory = true) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search songs, artists, playlists...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("", saveToHistory = false) }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        // 2. Body: Shortcuts & Results
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                }

                // Initial State: Horizontal Scrolling Genre & Mood pills (no emojis)
                uiState.query.isBlank() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        // Section A: Explore Genres (Horizontal scroll)
                        item {
                            Column {
                                Text(
                                    text = "Explore Genres",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp),
                                ) {
                                    items(popularGenres) { item ->
                                        CleanGenrePill(
                                            name = item.name,
                                            onClick = {
                                                viewModel.onQueryChange(item.query, saveToHistory = false)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section B: Moods & Activities (Horizontal scroll)
                        item {
                            Column {
                                Text(
                                    text = "Moods & Activities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp),
                                ) {
                                    items(moodVibes) { item ->
                                        CleanGenrePill(
                                            name = item.name,
                                            onClick = {
                                                viewModel.onQueryChange(item.query, saveToHistory = false)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section C: Recent Searches (if present)
                        if (recentSearches.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(onClick = { viewModel.clearAllHistory() }) {
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            items(recentSearches, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.selectHistoryQuery(item.query) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.query,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteHistoryItem(item.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Empty Results State
                uiState.results.songs.isEmpty() -> {
                    Text(
                        text = "No results found for \"${uiState.query}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 64.dp),
                    )
                }

                // Active Search Results List (Virtualized & Fast)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.results.songs,
                            key = { idx, track -> if (track.videoId.isNotBlank()) track.videoId else "search_$idx" }
                        ) { _, track ->
                            TrackItem(
                                track = track,
                                onClick = { viewModel.playTrack(track) },
                                onPlayNext = { viewModel.playNext(track) },
                                onAddToQueue = { viewModel.addToQueue(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanGenrePill(
    name: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}
