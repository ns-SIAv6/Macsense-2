package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SectionCard
import com.example.data.model.TrackItem
import com.example.ui.components.PlayheadCanvas
import com.example.ui.components.TactileKnob
import com.example.ui.theme.*

@Composable
fun VerticalDawScreen(
    sections: List<SectionCard>,
    activeSectionIndex: Int,
    currentBar: Int,
    bpm: Int,
    isPlaying: Boolean,
    tracks: List<TrackItem>,
    isRecording: Boolean,
    recordedDurationMs: Long,
    onTogglePlay: () -> Unit,
    onSelectSection: (Int) -> Unit,
    onBpmChange: (Int) -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedTrackId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Vertical Scroll Canvas (Main Stage Playhead)
        Box(modifier = Modifier.weight(1.2f)) {
            PlayheadCanvas(
                sections = sections,
                activeSectionIndex = activeSectionIndex,
                currentBar = currentBar,
                bpm = bpm,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                onSelectSection = onSelectSection,
                onBpmChange = onBpmChange
            )
        }

        // Vertical Timeline Track Stacker & Mixer Drawer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header + Flow Capture Record Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VERTICAL TIMELINE TRACK STACKER",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${tracks.size} Layered Tracks • Tactile Automation & MIDI Inputs",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Flow Capture DSP Recording Button
                    Button(
                        onClick = onToggleRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) CrimsonRed else CyberCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("flow_capture_rec_btn")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Flow Capture Record",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "REC ${(recordedDurationMs / 1000f)}s" else "FLOW REC",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Vertical Audio Track Layer Stack
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tracks) { track ->
                        val isExpanded = expandedTrackId == track.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isExpanded) 1.5.dp else 1.dp,
                                    color = if (isExpanded) CyberCyan else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { expandedTrackId = if (isExpanded) null else track.id },
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(track.soundType.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = track.name.uppercase(),
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (track.isMuted) CrimsonRed.copy(alpha = 0.2f) else DarkSurface,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = if (track.isMuted) "MUTED" else "ACTIVE",
                                                color = if (track.isMuted) CrimsonRed else EmeraldGreen,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Piano,
                                            contentDescription = "MIDI Input",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TactileKnob(
                                        label = "VOL",
                                        value = track.volume,
                                        onValueChange = {},
                                        color = track.soundType.color
                                    )

                                    TactileKnob(
                                        label = "PAN",
                                        value = (track.pan + 1f) / 2f,
                                        onValueChange = {},
                                        color = CyberCyan
                                    )

                                    // Simulated Track Waveform Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .padding(horizontal = 12.dp)
                                            .background(ObsidianBg, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                        ) {
                                            listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 1.0f, 0.6f, 0.4f, 0.8f, 0.9f, 0.5f, 0.2f, 0.7f).forEach { h ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .fillMaxHeight(h)
                                                        .background(
                                                            if (isPlaying) track.soundType.color else TextMuted,
                                                            RoundedCornerShape(2.dp)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

