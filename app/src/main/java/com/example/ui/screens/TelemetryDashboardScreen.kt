package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DuaneSlaBar
import com.example.ui.theme.*

@Composable
fun TelemetryDashboardScreen(
    integrityHash: Float,
    mtbfHours: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        Text(text = "TELEMETRY & INTEGRITY MATRIX", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = "Duane SLA MTBF Growth & System Reliability", color = TextMuted, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        DuaneSlaBar(integrityHash = integrityHash, mtbfHours = mtbfHours)

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .testTag("telemetry_card"),
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "STRESS-HARDENED SUBSYSTEM METRICS", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))

                TelemetryMetricRow("Cross-Pollination Heterozygosity", "97.2%", EmeraldGreen)
                TelemetryMetricRow("Rate-Limited Taste Delta", "Δ ≤ 0.05 / Session", GoldenAmber)
                TelemetryMetricRow("Entropy Auto-Recalibration", "Dynamic 4.23 Bits", CyberPurple)
                TelemetryMetricRow("Duane MTBF Growth", "4.0h → 12.2h", EmeraldGreen)
                TelemetryMetricRow("Concurrency Autoscaler", "6x Peak Headroom", CyberCyan)
            }
        }
    }
}

@Composable
private fun TelemetryMetricRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
