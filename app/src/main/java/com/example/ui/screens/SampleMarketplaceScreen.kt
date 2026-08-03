package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketplaceSample
import com.example.data.model.SoundType
import com.example.ui.theme.*

@Composable
fun SampleMarketplaceScreen(
    modifier: Modifier = Modifier
) {
    val samples = listOf(
        MarketplaceSample("m1", "Aether Sub 808", "CyberProducer_99", SoundType.SUB_808, 0, 0.95f, 0.40f, 0.10f, 0.85f, 1420),
        MarketplaceSample("m2", "Quantum Punch Kick", "Dr_Beat", SoundType.KICK, 0, 0.88f, 0.80f, 0.20f, 0.90f, 980),
        MarketplaceSample("m3", "Plasma Laser Snare", "SonicMind", SoundType.SNARE, 0, 0.40f, 0.95f, 0.50f, 0.60f, 2100),
        MarketplaceSample("m4", "Neon Synth Stabs", "AuraSound", SoundType.SYNTH, 0, 0.70f, 0.90f, 0.30f, 0.95f, 3200)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        Text(
            text = "GENOME SAMPLE EXCHANGE",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Cloud Sample Marketplace (Splice Parity)",
            color = TextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(samples) { sample ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .testTag("marketplace_sample_${sample.id}"),
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(sample.soundType.color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Preview", tint = sample.soundType.color)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(text = sample.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "By ${sample.creator} • ${sample.downloads} downloads", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "FREE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
