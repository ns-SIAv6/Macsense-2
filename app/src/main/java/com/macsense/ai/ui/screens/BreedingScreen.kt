package com.macsense.ai.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.ui.viewmodel.DawViewModel

/**
 * Surfaces the Phase 5 sound-genetics pipeline (breeding, resurrection, and lineage) that was
 * previously only reachable via Ari chat commands. Lets a user pick two LIVING/REBORN takes to
 * breed, pick a DORMANT/any take to resurrect, and see [DawViewModel.lastBredEntry] /
 * [DawViewModel.lastResurrectedEntry] / the [DawViewModel.soundLineage] ancestry chain rendered
 * directly, closing the UI gap called out after #28/#29.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingScreen(viewModel: DawViewModel = viewModel()) {
    val archiveEntries by viewModel.archiveEntries.collectAsState()
    val lastBredEntry by viewModel.lastBredEntry.collectAsState()
    val lastResurrectedEntry by viewModel.lastResurrectedEntry.collectAsState()

    var selectedParentA by remember { mutableStateOf<String?>(null) }
    var selectedParentB by remember { mutableStateOf<String?>(null) }
    var traitBias by remember { mutableStateOf(0.5f) }
    var selectedResurrectionTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshArchiveEntries() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83E\uDDEC ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SOUND BREEDING LAB", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = Modifier.testTag("breeding_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "BREED TWO TAKES",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (archiveEntries.isEmpty()) {
                item {
                    EmptyArchiveNotice()
                }
            } else {
                item {
                    Text("Parent A", color = TextSecondary, fontSize = 12.sp)
                }
                items(archiveEntries, key = { "a-" + it.takeId }) { entry ->
                    ArchiveEntryRow(
                        entry = entry,
                        selected = selectedParentA == entry.takeId,
                        onClick = { selectedParentA = entry.takeId }
                    )
                }

                item {
                    Text("Parent B", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                items(archiveEntries, key = { "b-" + it.takeId }) { entry ->
                    ArchiveEntryRow(
                        entry = entry,
                        selected = selectedParentB == entry.takeId,
                        onClick = { selectedParentB = entry.takeId }
                    )
                }

                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Trait Bias (toward Parent B)", color = TextSecondary, fontSize = 12.sp)
                            Text("${(traitBias * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = traitBias,
                            onValueChange = { traitBias = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(activeTrackColor = PurpleNeon, thumbColor = PurpleNeon, inactiveTrackColor = SurfaceSubtle)
                        )
                    }
                }

                item {
                    val canBreed = selectedParentA != null && selectedParentB != null && selectedParentA != selectedParentB
                    Button(
                        onClick = {
                            val a = selectedParentA
                            val b = selectedParentB
                            if (a != null && b != null) {
                                viewModel.breedSoundsFromUi(a, b, traitBias.toDouble())
                            }
                        },
                        enabled = canBreed,
                        modifier = Modifier.fillMaxWidth().testTag("breed_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BREED SELECTED TAKES", fontWeight = FontWeight.Bold)
                    }
                }

                lastBredEntry?.let { bred ->
                    item {
                        ResultCard(
                            title = "OFFSPRING CREATED",
                            entry = bred,
                            tint = PurpleNeon
                        )
                    }
                }

                item {
                    Text(
                        text = "RESURRECT A TAKE",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(archiveEntries.filter { it.state != SoundArchive.State.LIVING }.ifEmpty { archiveEntries }, key = { "r-" + it.takeId }) { entry ->
                    ArchiveEntryRow(
                        entry = entry,
                        selected = selectedResurrectionTarget == entry.takeId,
                        onClick = { selectedResurrectionTarget = entry.takeId }
                    )
                }

                item {
                    Button(
                        onClick = {
                            selectedResurrectionTarget?.let { viewModel.resurrectSoundFromUi(it, setOf("revived")) }
                        },
                        enabled = selectedResurrectionTarget != null,
                        modifier = Modifier.fillMaxWidth().testTag("resurrect_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESURRECT SELECTED TAKE", fontWeight = FontWeight.Bold)
                    }
                }

                lastResurrectedEntry?.let { revived ->
                    item {
                        ResultCard(
                            title = "TAKE RESURRECTED",
                            entry = revived,
                            tint = CyanNeon
                        )
                    }
                }

                item {
                    Text(
                        text = "LINEAGE",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                val focusTakeId = lastBredEntry?.takeId ?: lastResurrectedEntry?.takeId
                if (focusTakeId != null) {
                    item {
                        LineageCard(viewModel = viewModel, takeId = focusTakeId)
                    }
                } else {
                    item {
                        Text(
                            "Breed or resurrect a take above to see its ancestry chain here.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchiveNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x1F8B5CF6))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No archived takes yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Record a take in the DAW first — it's automatically archived with a genome, then it can be bred or resurrected here.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ArchiveEntryRow(entry: SoundArchive.Entry, selected: Boolean, onClick: () -> Unit) {
    val stateTint = when (entry.state) {
        SoundArchive.State.LIVING -> GreenActive
        SoundArchive.State.DORMANT -> TextSecondary
        SoundArchive.State.REBORN -> PurpleNeon
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PurpleNeon.copy(alpha = 0.2f) else SurfaceDark)
            .border(BorderStroke(1.dp, if (selected) PurpleNeon else Color(0x1F8B5CF6)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(entry.takeId.take(8), color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(entry.tags.joinToString(", ").ifEmpty { "no tags" }, color = TextSecondary, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(stateTint.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(entry.state.name, color = stateTint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultCard(title: String, entry: SoundArchive.Entry, tint: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Take: ${entry.takeId}", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            entry.originTakeId?.let {
                Text("Origin: $it", color = TextSecondary, fontSize = 11.sp)
            }
            if (entry.tags.isNotEmpty()) {
                Text("Tags: ${entry.tags.joinToString(", ")}", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LineageCard(viewModel: DawViewModel, takeId: String) {
    val lineage = viewModel.soundLineage
    val ancestors = remember(takeId, lineage) { lineage.ancestors(takeId) }
    val depth = remember(takeId, lineage) { lineage.generationDepth(takeId) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x1F8B5CF6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Focus take: ${takeId.take(8)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("Resurrection depth: $depth", color = TextSecondary, fontSize = 11.sp)
            if (ancestors.isEmpty()) {
                Text("No known ancestors in this snapshot — this is a root take.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Ancestors:", color = TextSecondary, fontSize = 11.sp)
                ancestors.forEach { ancestor ->
                    Text("  \u2022 ${ancestor.takeId.take(8)} (${ancestor.state.name})", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
