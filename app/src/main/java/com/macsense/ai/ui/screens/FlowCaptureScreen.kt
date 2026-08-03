package com.macsense.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macsense.ai.ui.viewmodel.FlowCaptureViewModel
import com.macsense.ai.ui.viewmodel.RecordSession
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowCaptureScreen(
    viewModel: FlowCaptureViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                FlowCaptureViewModel(LocalContext.current) as T
        }
    )
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val autoBpm by viewModel.autoBpm.collectAsState()
    val recordedTakes by viewModel.recordedTakes.collectAsState()
    val cadenceStyle by viewModel.cadenceStyle.collectAsState()
    val quantizeFeel by viewModel.quantizeFeel.collectAsState()
    val performanceStyle by viewModel.performanceStyle.collectAsState()
    val autoAlignToBeat by viewModel.autoAlignEnabled.collectAsState()

    // Simulate input level during recording
    val liveInputLevel = if (isRecording) {
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val phase by infiniteTransition.animateFloat(
            initialValue = -50f,
            targetValue = -3f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )
        phase
    } else {
        -60f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱ ", color = PurpleNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FLOW CAPTURE STUDIO", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = Modifier.testTag("flow_capture_screen")
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Recording controls and Visualizer
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recording visualizer / stopwatch panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x1F8B5CF6))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "FLOW STATE ENGINE",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Big Clock
                        val secondsInt = elapsedSeconds.toInt()
                        val minutes = secondsInt / 60
                        val displaySecs = secondsInt % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, displaySecs),
                            color = if (isRecording) Color.Red else TextPrimary,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )

                        // Signal Input Level Bar
                        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Signal Input Monitor", color = TextSecondary, fontSize = 11.sp)
                                Text("${String.format("%.1f", liveInputLevel)} dB", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val fillRatio = ((liveInputLevel + 60f) / 60f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(SurfaceSubtle)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillRatio)
                                        .background(if (liveInputLevel > -3.0f) Color.Red else CyanNeon)
                                )
                            }
                        }

                        // Capture Waves Animation
                        LiveCaptureWaveform(isRecording)

                        // Large Rec Button
                        Button(
                            onClick = { viewModel.toggleRecording() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red else PurpleNeon
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(52.dp)
                        ) {
                            Text(
                                text = if (isRecording) "■  STOP CAPTURE" else "●  TAP TO CAPTURE FLOW",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (autoBpm > 0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⏱ ", color = CyanNeon, fontSize = 14.sp)
                                Text(
                                    text = "Auto-Detected BPM: ${String.format("%.1f", autoBpm)}",
                                    color = CyanNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // AI Realtime Style Tuning Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1F8B5CF6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "AUTO-ALIGN & ASSISTANT TUNING",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Align To Project BPM", color = TextPrimary, fontSize = 13.sp)
                                Text("Keeps natural performance timing synced perfectly", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = autoAlignToBeat,
                                onCheckedChange = { viewModel.setAutoAlignEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.3f))
                            )
                        }

                        Divider(color = Color(0x1FA855F7))

                        Column {
                            Text("Flow Cadence Accent Style", color = TextSecondary, fontSize = 11.sp)
                            FlowStyleSelector(
                                items = listOf("Trap / Triplets", "Boom Bap 16ths", "Drill Syncopated", "Ambient / Loose"),
                                selectedItem = cadenceStyle,
                                onSelected = { viewModel.setCadenceStyle(it) }
                            )
                        }

                        Divider(color = Color(0x1FA855F7))

                        Column {
                            Text("Quantization Metric Snap", color = TextSecondary, fontSize = 11.sp)
                            FlowStyleSelector(
                                items = listOf("Straight 16ths", "Triplet 8ths", "Loose Swing", "No Quantize"),
                                selectedItem = quantizeFeel,
                                onSelected = { viewModel.setQuantizeFeel(it) }
                            )
                        }
                    }
                }
            }

            // Right Column: Captured Takes Shelf List
            Column(
                modifier = Modifier.weight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1FA855F7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "CAPTURED VOCAL TAKES SHELF",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (recordedTakes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No takes recorded yet.\nTap Record above to begin.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(recordedTakes) { index, take ->
                                    TakeItemRow(take) {
                                        viewModel.deleteTake(take.id)
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

@Composable
fun LiveCaptureWaveform(isRecording: Boolean) {
    val phase = if (isRecording) {
        rememberInfiniteTransition(label = "capture-waveform").animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "capture-phase"
        ).value
    } else {
        0f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(BorderStroke(1.dp, Color(0x1FA855F7)), RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val path = Path()

        path.moveTo(0f, midY)
        for (x in 0..width.toInt() step 4) {
            val ratio = x / width
            val envelope = sin(ratio * Math.PI).toFloat()
            val wave = if (isRecording) {
                sin(ratio * 25f + phase).toFloat() * 30f + sin(ratio * 50f).toFloat() * 12f
            } else {
                sin(ratio * 12f).toFloat() * 3f
            }
            path.lineTo(x.toFloat(), midY + wave * envelope)
        }

        drawPath(
            path = path,
            color = PurpleNeon,
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
fun FlowStyleSelector(items: List<String>, selectedItem: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PurpleNeon else SurfaceSubtle)
                    .clickable { onSelected(item) }
                    .semantics {
                        // Expose segmented choices as radio buttons to TalkBack and keyboard users.
                        role = Role.RadioButton
                        selected = isSelected
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TakeItemRow(take: RecordSession, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSubtle)
            .border(BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♫ ", color = CyanNeon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(take.id.uppercase(), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Duration: ${String.format("%.1f", take.durationSeconds)}s | BPM: ${String.format("%.1f", take.autoBpm)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Take", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}
