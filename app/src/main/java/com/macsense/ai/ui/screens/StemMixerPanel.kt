package com.macsense.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macsense.ai.audio.StemMixer
import com.macsense.ai.audio.StemTrack

/**
 * Phase 4 (issue #39): stem tracks as first-class objects — a horizontal strip of typed stem
 * channel strips with per-stem gain fader, mute and solo. Mix semantics live in [StemMixer];
 * this panel only renders state and forwards intents.
 */
@Composable
fun StemMixerPanel(
    stems: List<StemTrack>,
    onGainChange: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleSolo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anySolo = stems.any { it.soloed }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF14101E))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState())
            .testTag("stem_mixer_panel")
    ) {
        stems.forEach { stem ->
            val audible = StemMixer.isAudible(stem, stems)
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .padding(end = 8.dp)
                    .background(
                        if (audible) Color(0xFF1E1830) else Color(0xFF171320),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(6.dp)
                    .testTag("stem_strip_${stem.id}")
            ) {
                Text(
                    text = stem.name,
                    color = if (audible) Color(0xFFE7E3F4) else Color(0xFF6E6884),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%.1f dB".format(stem.gainDb),
                    color = Color(0xFF8F87A8),
                    fontSize = 9.sp
                )
                Slider(
                    value = stem.gainDb,
                    onValueChange = { onGainChange(stem.id, it) },
                    valueRange = StemMixer.MIN_GAIN_DB..StemMixer.MAX_GAIN_DB,
                    modifier = Modifier.height(28.dp)
                )
                Row {
                    TextButton(
                        onClick = { onToggleMute(stem.id) },
                        modifier = Modifier.testTag("stem_mute_${stem.id}")
                    ) {
                        Text(
                            "M",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stem.muted) Color(0xFFFF5470) else Color(0xFF6E6884)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    TextButton(
                        onClick = { onToggleSolo(stem.id) },
                        modifier = Modifier.testTag("stem_solo_${stem.id}")
                    ) {
                        Text(
                            "S",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stem.soloed -> Color(0xFFFFD447)
                                anySolo -> Color(0xFF4A4560)
                                else -> Color(0xFF6E6884)
                            }
                        )
                    }
                }
            }
        }
    }
}
