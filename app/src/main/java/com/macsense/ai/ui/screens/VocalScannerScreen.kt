package com.macsense.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocalScannerScreen() {
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var isScanComplete by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Fit My Voice") } // Match Closely, Fit My Voice, Blend Styles

    // Plugin values state
    var autoTuneSpeed by remember { mutableStateOf(15f) }
    var eqLow by remember { mutableStateOf(-2f) }
    var eqMid by remember { mutableStateOf(1.5f) }
    var eqHigh by remember { mutableStateOf(4f) }
    var compThreshold by remember { mutableStateOf(-16f) }
    var compRatio by remember { mutableStateOf(4f) }
    var reverbMix by remember { mutableStateOf(25f) }
    var delayFeedback by remember { mutableStateOf(30f) }

    val scope = rememberCoroutineScope()

    val infiniteRotation = rememberInfiniteTransition(label = "Scan Loader")
    val rotationAngle by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Rotation"
    )

    fun runVocalScan() {
        isScanning = true
        isScanComplete = false
        scanProgress = 0f
        scope.launch {
            for (i in 1..100) {
                delay(30)
                scanProgress = i / 100f
            }
            isScanning = false
            isScanComplete = true
            // Load preset parameters depending on tuning mode
            when (selectedMode) {
                "Match Closely" -> {
                    autoTuneSpeed = 3f
                    eqLow = -4f
                    eqMid = 0.5f
                    eqHigh = 6f
                    compThreshold = -22f
                    compRatio = 6f
                    reverbMix = 35f
                    delayFeedback = 45f
                }
                "Fit My Voice" -> {
                    autoTuneSpeed = 18f
                    eqLow = -1f
                    eqMid = 2f
                    eqHigh = 3.5f
                    compThreshold = -14f
                    compRatio = 3.5f
                    reverbMix = 15f
                    delayFeedback = 20f
                }
                "Blend Styles" -> {
                    autoTuneSpeed = 10f
                    eqLow = -2.5f
                    eqMid = 1f
                    eqHigh = 4.5f
                    compThreshold = -18f
                    compRatio = 4.5f
                    reverbMix = 25f
                    delayFeedback = 35f
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙 ", color = PurpleNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VOCAL PRESET SCANNER", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = Modifier.testTag("vocal_scanner_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drop Reference Song Zone
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x1FA855F7))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isScanning) runVocalScan() }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isScanning) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scanning",
                                tint = CyanNeon,
                                modifier = Modifier
                                    .size(48.dp)
                                    .rotate(rotationAngle)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("ANALYZING REFERENCE VOCAL PRINT...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanNeon,
                                trackColor = SurfaceSubtle,
                            )
                        } else {
                            Text(
                                "⇪",
                                color = PurpleNeon,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isScanComplete) "REFERENCE ANALYSIS COMPLETE! TAP TO RESCAN" else "DRAG & DROP REFERENCE SONG HERE",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Analyze EQ, Dynamics, Tuning speed, and Reverb space from any MP3/WAV reference.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Mode Selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("Match Closely", "Fit My Voice", "Blend Styles")
                    modes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PurpleNeon else SurfaceDark)
                                .border(BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.3f) else Color(0x1F8B5CF6)), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedMode = mode
                                    if (isScanComplete) {
                                        // Re-apply configurations
                                        runVocalScan()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Plugin Chain Blocks
            item {
                Text(
                    text = "SUGGESTED PLUGIN CHAIN SETTINGS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            // Auto-tune Block
            item {
                PluginControlCard(
                    title = "Vocal Pitch correction (Auto-Tune)",
                    active = true,
                    tint = CyanNeon
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Retune Speed", color = TextSecondary, fontSize = 12.sp)
                            Text("${autoTuneSpeed.toInt()} ms", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = autoTuneSpeed,
                            onValueChange = { autoTuneSpeed = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(activeTrackColor = CyanNeon, thumbColor = CyanNeon, inactiveTrackColor = SurfaceSubtle)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Correction Scale: Chromatic", color = TextSecondary, fontSize = 11.sp)
                            Text("Correction Humanize: 45%", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // EQ Block
            item {
                PluginControlCard(
                    title = "Suggested EQ Blueprint",
                    active = true,
                    tint = PurpleNeon
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BlueprintSlider("Low Pass/Shelf", eqLow, -12f, 12f) { eqLow = it }
                        BlueprintSlider("Presence Mid Boost", eqMid, -12f, 12f) { eqMid = it }
                        BlueprintSlider("Air High Shelf", eqHigh, -12f, 12f) { eqHigh = it }
                    }
                }
            }

            // Dynamic Compressor Block
            item {
                PluginControlCard(
                    title = "Opto Tube Compressor",
                    active = true,
                    tint = MagentaNeon
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Threshold", color = TextSecondary, fontSize = 12.sp)
                            Text("${compThreshold.toInt()} dB", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = compThreshold,
                            onValueChange = { compThreshold = it },
                            valueRange = -48f..0f,
                            colors = SliderDefaults.colors(activeTrackColor = MagentaNeon, thumbColor = MagentaNeon, inactiveTrackColor = SurfaceSubtle)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ratio", color = TextSecondary, fontSize = 12.sp)
                            Text("${String.format("%.1f", compRatio)}:1", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = compRatio,
                            onValueChange = { compRatio = it },
                            valueRange = 1f..10f,
                            colors = SliderDefaults.colors(activeTrackColor = MagentaNeon, thumbColor = MagentaNeon, inactiveTrackColor = SurfaceSubtle)
                        )
                    }
                }
            }

            // Spatial FX Block
            item {
                PluginControlCard(
                    title = "Vocal Space (Plate Reverb & Delay)",
                    active = true,
                    tint = GreenActive
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reverb Send Mix", color = TextSecondary, fontSize = 12.sp)
                            Text("${reverbMix.toInt()}%", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = reverbMix,
                            onValueChange = { reverbMix = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(activeTrackColor = GreenActive, thumbColor = GreenActive, inactiveTrackColor = SurfaceSubtle)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delay Feedback", color = TextSecondary, fontSize = 12.sp)
                            Text("${delayFeedback.toInt()}%", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = delayFeedback,
                            onValueChange = { delayFeedback = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(activeTrackColor = GreenActive, thumbColor = GreenActive, inactiveTrackColor = SurfaceSubtle)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PluginControlCard(
    title: String,
    active: Boolean,
    tint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color(0x1F8B5CF6)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(tint))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = active,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(checkedThumbColor = tint, checkedTrackColor = tint.copy(alpha = 0.3f))
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BlueprintSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(120.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(activeTrackColor = PurpleNeon, thumbColor = PurpleNeon, inactiveTrackColor = SurfaceSubtle),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%+.1fdB", value),
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
    }
}
