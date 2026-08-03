package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MasteringScreen(
    masteringLufs: Float,
    masteringStyle: String,
    onProcessMastering: (Float, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLufs by remember { mutableStateOf(masteringLufs) }
    var selectedStyle by remember { mutableStateOf(masteringStyle) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REFERENCE MASTERING CHAIN",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "1-Click AI Mastering Engine (LANDR / eMastered Parity)",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Mastering", tint = GoldenAmber, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LUFS Target Selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "TARGET LOUDNESS (LUFS)", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-14.0f to "Streaming (-14 LUFS)", -9.0f to "Club Punch (-9 LUFS)", -6.0f to "Ultra Loud (-6 LUFS)").forEach { (lufs, label) ->
                        FilterChip(
                            selected = selectedLufs == lufs,
                            onClick = { selectedLufs = lufs },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldenAmber, selectedLabelColor = ObsidianBg)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mastering Style Profile
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "SONIC DYNAMIC PROFILE", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cyber Warm Punch", "Crisp Highs", "Deep Bass Sub", "Wide Stereo").forEach { style ->
                        FilterChip(
                            selected = selectedStyle == style,
                            onClick = { selectedStyle = style },
                            label = { Text(style, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonMagenta, selectedLabelColor = TextPrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onProcessMastering(selectedLufs, selectedStyle) },
            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber, contentColor = ObsidianBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("execute_mastering_btn")
        ) {
            Icon(imageVector = Icons.Default.Speed, contentDescription = "Process Master")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EXECUTE ONE-CLICK AI MASTERING",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
