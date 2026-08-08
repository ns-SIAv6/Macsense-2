package com.macsense.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macsense.ai.ui.components.WaveformView
import com.macsense.ai.ui.viewmodel.RecordSession

/** Reusable plugin control card used in VocalScannerScreen. */
@Composable
fun PluginControlCard(
    title: String,
    active: Boolean = true,
    tint: Color = PurpleNeon,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) tint.copy(alpha = 0.4f) else Color(0x1F8B5CF6))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) tint else TextSecondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

/** Labeled slider for VocalScanner plugin blueprint params. */
@Composable
fun BlueprintSlider(
    label: String,
    value: Float,
    min: Float = -12f,
    max: Float = 12f,
    unit: String = "dB",
    tint: Color = PurpleNeon,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text("${String.format("%.1f", value)} $unit", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = min..max,
            colors = SliderDefaults.colors(activeTrackColor = tint, thumbColor = tint, inactiveTrackColor = SurfaceSubtle)
        )
    }
}

/** Horizontal chip-row for mode selection (FlowCapture, VocalScanner). */
@Composable
fun FlowStyleSelector(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val sel = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) PurpleNeon else SurfaceDark)
                    .border(BorderStroke(1.dp, if (sel) PurpleNeon.copy(0.6f) else Color(0x1F8B5CF6)), RoundedCornerShape(8.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option, color = if (sel) Color.White else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * A single recorded take row shown in the takes shelf panel.
 */
@Composable
fun TakeItemRow(take: RecordSession, onDelete: () -> Unit) {
    val durationLabel = take.durationSeconds.let {
        val s = it.toInt()
        "%d:%02d".format(s / 60, s % 60)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x1F8B5CF6))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(take.id.take(8), color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("$durationLabel  •  ${String.format("%.1f", take.bpm)} BPM  •  ${(take.flowConfidence * 100).toInt()}% flow", color = TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete take", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            if (take.amplitudeEnvelope.isNotEmpty()) {
                WaveformView(
                    amplitudes = take.amplitudeEnvelope,
                    height = 40.dp,
                    accentColor = CyanNeon
                )
            }
        }
    }
}

/**
 * Lineage tree card — renders the sound ancestry chain as a scrollable column of
 * parent→child arrows. Shown in BreedingScreen.
 */
@Composable
fun LineageCard(
    viewModel: com.macsense.ai.ui.viewmodel.DawViewModel,
    takeId: String
) {
    val entries by viewModel.archiveEntries.collectAsState()
    val chain = remember(takeId, entries) {
        buildLineageChain(takeId, entries)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color(0x1F8B5CF6)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ANCESTRY CHAIN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            if (chain.isEmpty()) {
                Text("No ancestry data for this take.", color = TextSecondary, fontSize = 12.sp)
            } else {
                chain.forEachIndexed { index, entry ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (index > 0) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            entry.takeId.take(12),
                            color = when (entry.state) {
                                com.macsense.ai.audio.SoundArchive.State.LIVING -> GreenActive
                                com.macsense.ai.audio.SoundArchive.State.DORMANT -> TextSecondary
                                com.macsense.ai.audio.SoundArchive.State.REBORN -> PurpleNeon
                            },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(entry.state.name, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun buildLineageChain(
    takeId: String,
    all: List<com.macsense.ai.audio.SoundArchive.Entry>
): List<com.macsense.ai.audio.SoundArchive.Entry> {
    val byId = all.associateBy { it.takeId }
    val chain = mutableListOf<com.macsense.ai.audio.SoundArchive.Entry>()
    var current = byId[takeId]
    var depth = 0
    while (current != null && depth < 20) {
        chain.add(0, current) // prepend so oldest ancestor is first
        current = current.originTakeId?.let { byId[it] }
        depth++
    }
    return chain
}
