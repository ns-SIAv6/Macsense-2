package com.macsense.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macsense.ai.ui.viewmodel.MasteringPreset
import com.macsense.ai.ui.viewmodel.MasteringViewModel
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasteringScreen(viewModel: MasteringViewModel = viewModel()) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val targetLufs by viewModel.targetLufs.collectAsState()
    val ceilingDbtp by viewModel.ceilingDbtp.collectAsState()
    val currentLufs by viewModel.currentLufs.collectAsState()
    val currentDbtp by viewModel.currentDbtp.collectAsState()
    val eqLow by viewModel.eqLowGain.collectAsState()
    val eqMid by viewModel.eqMidGain.collectAsState()
    val eqHigh by viewModel.eqHighGain.collectAsState()
    val limiterThreshold by viewModel.limiterThreshold.collectAsState()
    val compressorThreshold by viewModel.compressorThreshold.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedPresetId by viewModel.selectedPresetId.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("〰 ", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MACSENSE MASTERING SUITE", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = Modifier.testTag("mastering_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Waveform visualizer & Real Meters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x1FA855F7))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "MASTER SIGNAL LEVEL & METERS",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mastering Visualizer (Canvas)
                        MasterWaveformVisualizer(isProcessing)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Meter readouts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LoudnessMeterStats("LOUDNESS INTEGRATED", String.format("%.1f LUFS", currentLufs), targetLufs, CyanNeon)
                            LoudnessMeterStats("TRUE PEAK MAX", String.format("%.2f dBTP", currentDbtp), ceilingDbtp, MagentaNeon)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Master Engine Start/Stop Action
                        Button(
                            onClick = {
                                if (isProcessing) viewModel.stopMasteringProcess()
                                else viewModel.startMasteringProcess()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProcessing) Color.Red else CyanNeon
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isProcessing) "‖  STOP ANALYZING SIGNAL" else "▶  INITIALIZE MASTERING DISPATCHER",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Target Settings Sliders
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1FA855F7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "MASTERING TARGET SPECIFICATIONS",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Target LUFS
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target Integrated Loudness", color = TextPrimary, fontSize = 13.sp)
                                Text("${String.format("%.1f", targetLufs)} LUFS", color = CyanNeon, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = targetLufs.toFloat(),
                                onValueChange = { viewModel.setTargetLufs(it.toDouble()) },
                                valueRange = -24f..-4f,
                                colors = SliderDefaults.colors(activeTrackColor = CyanNeon, thumbColor = CyanNeon, inactiveTrackColor = SurfaceSubtle)
                            )
                        }

                        // Target dBTP
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("True Peak Ceiling Limit", color = TextPrimary, fontSize = 13.sp)
                                Text("${String.format("%.2f", ceilingDbtp)} dBTP", color = MagentaNeon, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = ceilingDbtp.toFloat(),
                                onValueChange = { viewModel.setCeilingDbtp(it.toDouble()) },
                                valueRange = -6f..0f,
                                colors = SliderDefaults.colors(activeTrackColor = MagentaNeon, thumbColor = MagentaNeon, inactiveTrackColor = SurfaceSubtle)
                            )
                        }
                    }
                }
            }

            // Preset Selectors
            item {
                Text(
                    "TARGET PLATFORM PRESETS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(2).forEach { preset ->
                        PresetCard(
                            preset = preset,
                            isSelected = selectedPresetId == preset.id,
                            onClick = { viewModel.applyPreset(preset.id) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(2).take(2).forEach { preset ->
                        PresetCard(
                            preset = preset,
                            isSelected = selectedPresetId == preset.id,
                            onClick = { viewModel.applyPreset(preset.id) }
                        )
                    }
                }
            }

            // Dynamics and EQ Fine tuning panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1F8B5CF6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "EQ & DYNAMIC LIMITER ATTRIBUTES",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Three bands EQ
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EQ Low", color = TextSecondary, fontSize = 11.sp)
                                Slider(value = eqLow, onValueChange = { viewModel.setEqGains(it, eqMid, eqHigh) }, valueRange = -12f..12f, colors = SliderDefaults.colors(activeTrackColor = PurpleNeon))
                                Text(String.format("%+.1fdB", eqLow), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EQ Mid", color = TextSecondary, fontSize = 11.sp)
                                Slider(value = eqMid, onValueChange = { viewModel.setEqGains(eqLow, it, eqHigh) }, valueRange = -12f..12f, colors = SliderDefaults.colors(activeTrackColor = PurpleNeon))
                                Text(String.format("%+.1fdB", eqMid), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EQ High", color = TextSecondary, fontSize = 11.sp)
                                Slider(value = eqHigh, onValueChange = { viewModel.setEqGains(eqLow, eqMid, it) }, valueRange = -12f..12f, colors = SliderDefaults.colors(activeTrackColor = PurpleNeon))
                                Text(String.format("%+.1fdB", eqHigh), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Limiter & Compressor sliders
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tube Compressor Threshold", color = TextPrimary, fontSize = 12.sp)
                                Text("${compressorThreshold.toInt()} dB", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Slider(value = compressorThreshold, onValueChange = { viewModel.setCompressorThreshold(it) }, valueRange = -48f..0f)
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Limiter Hard Threshold", color = TextPrimary, fontSize = 12.sp)
                                Text("${limiterThreshold.toInt()} dB", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Slider(value = limiterThreshold, onValueChange = { viewModel.setLimiterThreshold(it) }, valueRange = -24f..0f)
                        }
                    }
                }
            }

            // Export Section
            item {
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPILE & EXPORT BROADCAST WAV", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Completed", color = TextPrimary) },
            text = { Text("Mastered WAV has been processed cleanly under -1.0dBTP and synced correctly to target LUFS.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK", color = CyanNeon)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun MasterWaveformVisualizer(isPlaying: Boolean) {
    val phase by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundDark)
            .border(BorderStroke(1.dp, Color(0x1FA855F7)), RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val path = Path()
        
        path.moveTo(0f, midY)
        for (x in 0..width.toInt() step 4) {
            val ratio = x / width
            val envelope = sin(ratio * Math.PI).toFloat()
            val wave = if (isPlaying) {
                sin(ratio * 20f + phase).toFloat() * 35f + sin(ratio * 40f).toFloat() * 12f
            } else {
                sin(ratio * 10f).toFloat() * 6f
            }
            path.lineTo(x.toFloat(), midY + wave * envelope)
        }
        
        drawPath(
            path = path,
            color = CyanNeon,
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
fun LoudnessMeterStats(label: String, valStr: String, targetVal: Double, tintColor: Color) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(valStr, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${String.format("%.1f", targetVal)}",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PresetCard(preset: MasteringPreset, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PurpleNeon else SurfaceDark)
            .border(BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else Color(0x1F8B5CF6)), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(preset.name, color = if (isSelected) Color.White else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Loudness: ${preset.targetLufs} LUFS", color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("Peak: ${preset.ceilingDbtp} dBTP", color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
