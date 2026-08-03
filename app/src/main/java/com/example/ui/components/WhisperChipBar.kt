package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WhisperChip
import com.example.ui.theme.*

@Composable
fun WhisperChipBar(
    chips: List<WhisperChip>,
    onActionClick: (WhisperChip) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chips.isEmpty()) return

    val topChip = chips.first()
    val tierColor = when (topChip.tier) {
        WhisperChip.Tier.NOTIFY -> CyberCyan
        WhisperChip.Tier.QUESTION -> GoldenAmber
        WhisperChip.Tier.REVIEW -> CrimsonRed
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .testTag("whisper_chip_bar"),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceVariant.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(tierColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (topChip.tier) {
                                WhisperChip.Tier.NOTIFY -> Icons.Default.AutoAwesome
                                WhisperChip.Tier.QUESTION -> Icons.Default.Psychology
                                WhisperChip.Tier.REVIEW -> Icons.Default.Warning
                            },
                            contentDescription = "ARi Thread",
                            tint = tierColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ARi • ${topChip.sourceThread}",
                                color = tierColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[Tier: ${topChip.tier.name}]",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = topChip.message,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    topChip.actionText?.let { action ->
                        Button(
                            onClick = { onActionClick(topChip) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tierColor,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("whisper_action_btn")
                        ) {
                            Text(
                                text = action,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss(topChip.id) }
                            .testTag("whisper_dismiss_btn")
                    )
                }
            }
        }
    }
}
