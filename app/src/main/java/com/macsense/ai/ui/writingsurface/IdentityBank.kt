package com.macsense.ai.ui.writingsurface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class ArtistIdentityProfile(
    val id: String,
    val name: String,
    val description: String,
    val styleTags: List<String>,
    val color: Color
)

val ArtistProfiles = listOf(
    ArtistIdentityProfile(
        id = "Aggressive Trap",
        name = "Aggressive Trap",
        description = "heavy 808s, raw street slang, rapid-fire hi-hat cadences",
        styleTags = listOf("808", "STREET", "FAST"),
        color = MacsenseError
    ),
    ArtistIdentityProfile(
        id = "Melodic R&B",
        name = "Melodic R&B",
        description = "smooth vocal flows, neon late-night vibes, rich emotional space",
        styleTags = listOf("NEON", "SMOOTH", "SOUL"),
        color = MacsenseAccentPurpleBright
    ),
    ArtistIdentityProfile(
        id = "Poetic Folk",
        name = "Poetic Folk",
        description = "intimate acoustic style, storytelling focus, organic rhythm flow",
        styleTags = listOf("ORGANIC", "POETIC", "DEEP"),
        color = MacsenseGoldPrimary
    ),
    ArtistIdentityProfile(
        id = "Grimy Boom-Bap",
        name = "Grimy Boom-Bap",
        description = "classic dusty vinyl beats, dense multi-syllabic rhymes, raw delivery",
        styleTags = listOf("VINYL", "RAW", "LOFI"),
        color = MacsenseSuccess
    )
)

@Composable
fun IdentityBank(
    selectedIdentityId: String,
    onIdentitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("identity_bank"),
        colors = CardDefaults.cardColors(containerColor = MacsensePanelPurple),
        border = androidx.compose.foundation.BorderStroke(1.dp, MacsenseBorderPurple),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARTIST IDENTITY BANK",
                    color = MacsenseTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Presets: ${ArtistProfiles.size}",
                    color = MacsenseTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("identity_bank_grid")
            ) {
                items(ArtistProfiles) { profile ->
                    val isSelected = profile.id == selectedIdentityId
                    val borderColor = if (isSelected) MacsenseGoldPrimary else MacsenseBorderPurple
                    val borderThickness = if (isSelected) 2.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MacsenseCardPurple else MacsenseVoidBlack)
                            .border(borderThickness, borderColor, RoundedCornerShape(8.dp))
                            .clickable { onIdentitySelected(profile.id) }
                            .padding(10.dp)
                            .testTag("identity_item_${profile.id}")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.name.uppercase(),
                                    color = if (isSelected) MacsenseGoldPrimary else MacsenseTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(profile.color)
                                )
                            }
                            Text(
                                text = profile.description,
                                color = MacsenseTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                profile.styleTags.forEach { tag ->
                                    Text(
                                        text = tag,
                                        color = MacsenseTextMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .background(MacsenseBorderPurple.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
