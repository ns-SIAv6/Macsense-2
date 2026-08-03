package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SoundGenome
import com.example.ui.components.GenomeTensorCard
import com.example.ui.theme.*

@Composable
fun SoundBreedingScreen(
    activeGenomes: List<SoundGenome>,
    parentA: SoundGenome?,
    parentB: SoundGenome?,
    breedWeight: Float,
    mutationFactor: Float,
    lastBredGenome: SoundGenome?,
    onSelectParentA: (SoundGenome) -> Unit,
    onSelectParentB: (SoundGenome) -> Unit,
    onBreedWeightChange: (Float) -> Unit,
    onMutationChange: (Float) -> Unit,
    onBreedClick: () -> Unit,
    onPreviewGenome: (SoundGenome) -> Unit,
    modifier: Modifier = Modifier
) {
    val heterozygosity = SoundGenome.calculateHeterozygosity(activeGenomes)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        // Title & Genetic Diversity Metric
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GENOMIC ENGINE",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "4D Tensor Stochastic Sound Breeding",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = EmeraldGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "WRIGHT'S DIVERSITY",
                        color = EmeraldGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(heterozygosity * 100).toInt()}% HETERO",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Breeding Dock (Parent A + Parent B)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Parent A Slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PARENT A",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = parentA?.name ?: "[ Select Below ]",
                            color = if (parentA != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "×",
                        color = GoldenAmber,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Parent B Slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PARENT B",
                            color = NeonMagenta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = parentB?.name ?: "[ Select Below ]",
                            color = if (parentB != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breeding Weight Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "A/B WEIGHT",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(80.dp)
                    )
                    Slider(
                        value = breedWeight,
                        onValueChange = onBreedWeightChange,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                    Text(
                        text = "${(breedWeight * 100).toInt()}:${((1f - breedWeight) * 100).toInt()}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(50.dp)
                    )
                }

                // Mutation Factor Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MUTATION",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(80.dp)
                    )
                    Slider(
                        value = mutationFactor,
                        onValueChange = onMutationChange,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonRed,
                            activeTrackColor = CrimsonRed,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                    Text(
                        text = "±${(mutationFactor * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breed Sound Action Button
                Button(
                    onClick = onBreedClick,
                    enabled = parentA != null && parentB != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPurple,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("breed_sound_button")
                ) {
                    Icon(imageVector = Icons.Default.Biotech, contentDescription = "Breed Sound")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BREED HYBRID SOUND GENOME",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Sound Genomes Pool
        Text(
            text = "ACTIVE GENOME VAULT (${activeGenomes.size})",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(activeGenomes) { genome ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { onPreviewGenome(genome) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Sound",
                            tint = genome.soundType.color
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        GenomeTensorCard(
                            genome = genome,
                            isSelected = parentA?.id == genome.id || parentB?.id == genome.id,
                            onClick = {
                                if (parentA == null) onSelectParentA(genome)
                                else if (parentB == null && parentA.id != genome.id) onSelectParentB(genome)
                                else onSelectParentA(genome)
                            }
                        )
                    }
                }
            }
        }
    }
}
