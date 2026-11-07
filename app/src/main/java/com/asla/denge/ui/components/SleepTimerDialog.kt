package com.asla.denge.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/**
 * Modern sleep timer modal dialog matching the Archive Tunes aesthetic.
 * Allows custom slider duration (5 to 120 minutes) and "End of track".
 */
@Composable
fun SleepTimerDialog(
    initialMinutes: Int? = null,
    onDismissRequest: () -> Unit,
    onSetTimer: (minutes: Int) -> Unit,
    onResetTimer: () -> Unit,
) {
    var isEndOfTrack by remember { mutableStateOf(initialMinutes == -1) }
    var selectedMinutes by remember {
        mutableFloatStateOf(
            if (initialMinutes != null && initialMinutes > 0) initialMinutes.toFloat() else 30f
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title
                Text(
                    text = "Timer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Current duration label
                Text(
                    text = if (isEndOfTrack) "End of track" else "${selectedMinutes.roundToInt()} minutes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Slider (5 to 120 minutes with 5-minute intervals)
                Slider(
                    value = selectedMinutes,
                    onValueChange = {
                        selectedMinutes = ((it / 5).roundToInt() * 5).coerceIn(5, 120).toFloat()
                        isEndOfTrack = false
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                    enabled = !isEndOfTrack,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "End of track" Full-Width Option Button with prominent Radio Circle
                Surface(
                    onClick = {
                        isEndOfTrack = !isEndOfTrack
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isEndOfTrack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(
                        width = if (isEndOfTrack) 1.5.dp else 1.dp,
                        color = if (isEndOfTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "End of track",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isEndOfTrack) FontWeight.Bold else FontWeight.Medium,
                            color = if (isEndOfTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )

                        // Distinct selection circle (Radio Indicator)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .then(
                                    if (isEndOfTrack) {
                                        Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isEndOfTrack) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Reset | Cancel | OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            onResetTimer()
                            onDismissRequest()
                        },
                    ) {
                        Text(
                            text = "Reset",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                if (isEndOfTrack) {
                                    onSetTimer(-1)
                                } else {
                                    onSetTimer(selectedMinutes.roundToInt())
                                }
                                onDismissRequest()
                            },
                        ) {
                            Text(
                                text = "OK",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
