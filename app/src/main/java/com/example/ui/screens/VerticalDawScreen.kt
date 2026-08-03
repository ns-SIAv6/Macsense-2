package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Vertical Scroll Canvas (Main Stage)
        Box(modifier = Modifier.weight(1f)) {
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

        // Instrument Map & Flow Capture Control Drawer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
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
                            text = "INSTRUMENT MAP & AUTOMATION",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "5 Active Tracks • Tactile Automation",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Track Strip Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tracks) { track ->
                        Surface(
                            modifier = Modifier
                                .width(160.dp)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = track.name,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(track.soundType.color, CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
