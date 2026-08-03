package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MidiControllerManager
import com.example.data.local.BreedingHistoryEntity
import com.example.data.model.SoundGenome
import com.example.ui.components.GenomeTensorCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SoundBreedingScreen(
    activeGenomes: List<SoundGenome>,
    parentA: SoundGenome?,
    parentB: SoundGenome?,
    breedWeight: Float,
    mutationFactor: Float,
    lastBredGenome: SoundGenome?,
    breedingHistory: List<BreedingHistoryEntity> = emptyList(),
    midiState: MidiControllerManager.MidiState = MidiControllerManager.MidiState(),
    onSelectParentA: (SoundGenome) -> Unit,
    onSelectParentB: (SoundGenome) -> Unit,
    onBreedWeightChange: (Float) -> Unit,
    onMutationChange: (Float) -> Unit,
    onBreedClick: () -> Unit,
    onPreviewGenome: (SoundGenome) -> Unit,
    onVirtualCcChange: (Int, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val heterozygosity = SoundGenome.calculateHeterozygosity(activeGenomes)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Vault, 1: Breeding History

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
                    text = "4D Tensor Stochastic Sound Breeding + MIDI Mapping",
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

        Spacer(modifier = Modifier.height(12.dp))

        // --- MIDI Controller Live Layer Status Overlay ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (midiState.isConnected) EmeraldGreen else CrimsonRed,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MIDI CONTROLLER: ${midiState.deviceName}",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (midiState.lastNotePressed != null) {
                            Text(
                                text = "ACTIVE NOTE: ${midiState.lastNoteName} (${midiState.lastNotePressed}) | VEL: ${midiState.lastVelocity}",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else if (midiState.lastCcNumber != null) {
                            Text(
                                text = "ACTIVE CC: #${midiState.lastCcNumber} -> ${midiState.activeCcTarget} (${midiState.lastCcValue})",
                                color = GoldenAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "Mapped: CC1(Rad) CC7(Wt) CC16(Mass) CC17(Mut) CC71(Ent)",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LIVE CC ENGINE",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

                // Breeding Weight Slider (CC 7)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "A/B WEIGHT (CC7)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(105.dp)
                    )
                    Slider(
                        value = breedWeight,
                        onValueChange = {
                            onBreedWeightChange(it)
                            onVirtualCcChange(7, it)
                        },
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
                        modifier = Modifier.width(45.dp)
                    )
                }

                // Mutation Factor Slider (CC 17)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MUTATION (CC17)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(105.dp)
                    )
                    Slider(
                        value = mutationFactor,
                        onValueChange = {
                            onMutationChange(it)
                            onVirtualCcChange(17, it)
                        },
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
                        modifier = Modifier.width(45.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Active Vault vs Breeding History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("ACTIVE VAULT (${activeGenomes.size})", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan,
                    selectedLabelColor = ObsidianBg
                )
            )

            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("BREEDING HISTORY (${breedingHistory.size})", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldenAmber,
                    selectedLabelColor = ObsidianBg
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
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
        } else {
            // Breeding History timeline saved in Room Database
            if (breedingHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No breeding history records in Room Database yet.\nBreed parents to persist lineage history across sessions.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(breedingHistory) { history ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                            color = DarkSurface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OFFSPRING: ${history.childName} (GEN ${history.generation})",
                                        color = GoldenAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${history.parentAName} × ${history.parentBName}",
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Weight: ${(history.breedWeight * 100).toInt()}% | Mutation: ±${(history.mutationFactor * 100).toInt()}%",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(history.timestamp)),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

