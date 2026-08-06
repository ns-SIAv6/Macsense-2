package com.macsense.ai.ui.writingsurface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macsense.ai.ui.theme.*

@Composable
fun CreativeStatsStrip(
    stats: LyricStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("creative_stats_strip"),
        colors = CardDefaults.cardColors(containerColor = MacsensePanelPurple),
        border = androidx.compose.foundation.BorderStroke(1.dp, MacsenseBorderPurple),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats Item 1: Word Count
            StatItem(
                label = "WORDS",
                value = stats.wordCount.toString(),
                tagSuffix = "w",
                indicatorColor = MacsenseGoldPrimary,
                modifier = Modifier.weight(1f)
            )

            VerticalDividerDivider()

            // Stats Item 2: Syllable Count
            StatItem(
                label = "SYLLABLES",
                value = stats.syllableCount.toString(),
                tagSuffix = "s",
                indicatorColor = MacsenseAccentPurpleBright,
                modifier = Modifier.weight(1f)
            )

            VerticalDividerDivider()

            // Stats Item 3: Rhyme Density
            StatItem(
                label = "RHYME DENSITY",
                value = "${stats.rhymeDensityPercent}%",
                tagSuffix = "rh",
                indicatorColor = MacsenseSuccess,
                modifier = Modifier.weight(1.2f)
            )

            VerticalDividerDivider()

            // Stats Item 4: Cadence Consistency
            StatItem(
                label = "CADENCE",
                value = "${stats.cadenceScore}/100",
                tagSuffix = "cad",
                indicatorColor = MacsenseGoldBright,
                modifier = Modifier.weight(1.2f)
            )

            VerticalDividerDivider()

            // Stats Item 5: Vocab Richness
            StatItem(
                label = "VOCAB RICHNESS",
                value = "${stats.vocabRichnessPercent}%",
                tagSuffix = "voc",
                indicatorColor = MacsenseTextPrimary,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    tagSuffix: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = MacsenseTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("stat_label_$tagSuffix")
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(indicatorColor)
            )
            Text(
                text = value,
                color = MacsenseTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("stat_value_$tagSuffix")
            )
        }
    }
}

@Composable
private fun VerticalDividerDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MacsenseBorderPurple)
    )
}
