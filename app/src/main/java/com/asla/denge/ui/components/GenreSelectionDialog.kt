package com.asla.denge.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.asla.denge.domain.model.MusicGenre

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreSelectionDialog(
    genres: List<MusicGenre>,
    onToggleGenre: (String) -> Unit,
    onAddCustomGenre: (String, String) -> Unit,
    onDeleteCustomGenre: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isFirstTimeOnboarding: Boolean = false,
) {
    val context = LocalContext.current
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customGenreName by remember { mutableStateOf("") }
    var customSearchQuery by remember { mutableStateOf("") }

    val selectedCount = genres.count { it.isSelected }

    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = {
                Text(
                    text = "Tambah Genre Kustom ✍️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ketik nama genre atau artis/tema favorit Anda (contoh: Hololive, Ado, Vocaloid, Synthwave)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customGenreName,
                        onValueChange = { customGenreName = it },
                        label = { Text("Nama Genre") },
                        placeholder = { Text("Contoh: Hololive / Ado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customSearchQuery,
                        onValueChange = { customSearchQuery = it },
                        label = { Text("Kata Kunci Pencarian (Opsional)") },
                        placeholder = { Text("Contoh: hololive original songs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customGenreName.isNotBlank()) {
                            onAddCustomGenre(
                                customGenreName.trim(),
                                customSearchQuery.trim().ifBlank { customGenreName.trim() }
                            )
                            Toast.makeText(context, "Genre '$customGenreName' berhasil ditambahkan! ✨", Toast.LENGTH_SHORT).show()
                            customGenreName = ""
                            customSearchQuery = ""
                            showAddCustomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (!isFirstTimeOnboarding || selectedCount >= 1) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isFirstTimeOnboarding || selectedCount >= 1,
            dismissOnClickOutside = !isFirstTimeOnboarding || selectedCount >= 1,
            usePlatformDefaultWidth = false,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFirstTimeOnboarding) "Pilih Genre Favoritmu 🎵" else "Kelola Genre Beranda ⚙️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pilih 1 - 5 genre musik untuk Beranda ($selectedCount/5 terpilih)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!isFirstTimeOnboarding) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    // Tombol Tambah Genre Kustom
                    Button(
                        onClick = { showAddCustomDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Tambah Genre Kustom (Ketik Sendiri)",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                item {
                    Text(
                        text = "PILIHAN GENRE TERSEDIA (MAKS. 5)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        genres.forEach { genre ->
                            val isSelected = genre.isSelected
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .clickable {
                                        if (!isSelected && selectedCount >= 5) {
                                            Toast.makeText(context, "Maksimal memilih 5 genre musik! 🎵", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onToggleGenre(genre.id)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = genre.icon,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = genre.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    if (genre.isCustom && onDeleteCustomGenre != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus",
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteCustomGenre(genre.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCount == 0) {
                        Toast.makeText(context, "Pilih minimal 1 genre musik! 🎵", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (isFirstTimeOnboarding) "Mulai Mendengarkan 🚀" else "Simpan Pilihan ✨")
            }
        },
        dismissButton = {
            if (!isFirstTimeOnboarding) {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    )
}
