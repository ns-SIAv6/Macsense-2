package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.MacSenseViewModel
import com.example.ui.components.WhisperChipBar
import com.example.ui.screens.*
import com.example.ui.theme.MacSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MacSenseTheme {
                MacSenseApp()
            }
        }
    }
}

enum class MacSenseNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    VERTICAL_DAW("DAW", Icons.Default.GraphicEq, "nav_daw"),
    BREEDING("BREED", Icons.Default.Biotech, "nav_breed"),
    GRAVEYARD("LAZARUS", Icons.Default.Dangerous, "nav_lazarus"),
    LYRICS("LYRICS", Icons.Default.Mic, "nav_lyrics"),
    MARKETPLACE("SAMPLE", Icons.Default.Download, "nav_marketplace"),
    VERSIONS("NODES", Icons.Default.AccountTree, "nav_versions"),
    MASTERING("MASTER", Icons.Default.Equalizer, "nav_mastering"),
    ARI_CHAT("ARI CHAT", Icons.Default.Psychology, "nav_ari"),
    PIANO_ROLL("PIANO", Icons.Default.GridOn, "nav_piano"),
    TELEMETRY("SLA", Icons.Default.Speed, "nav_telemetry")
}

@Composable
fun MacSenseApp(
    viewModel: MacSenseViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(MacSenseNavTab.VERTICAL_DAW) }

    // Collect StateFlows reactively
    val activeGenomes by viewModel.activeGenomes.collectAsStateWithLifecycle()
    val extinctGenomes by viewModel.extinctGenomes.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val versionNodes by viewModel.versionNodes.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentBar by viewModel.currentBar.collectAsStateWithLifecycle()
    val bpm by viewModel.bpm.collectAsStateWithLifecycle()
    val activeSectionIndex by viewModel.activeSectionIndex.collectAsStateWithLifecycle()

    val parentA by viewModel.parentA.collectAsStateWithLifecycle()
    val parentB by viewModel.parentB.collectAsStateWithLifecycle()
    val breedWeight by viewModel.breedWeight.collectAsStateWithLifecycle()
    val mutationFactor by viewModel.mutationFactor.collectAsStateWithLifecycle()
    val lastBredGenome by viewModel.lastBredGenome.collectAsStateWithLifecycle()

    val whisperChips by viewModel.whisperChips.collectAsStateWithLifecycle()
    val ariAiResponse by viewModel.ariAiResponse.collectAsStateWithLifecycle()
    val isAriLoading by viewModel.isAriLoading.collectAsStateWithLifecycle()

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordedDurationMs by viewModel.recordedDurationMs.collectAsStateWithLifecycle()

    val vocalScanMode by viewModel.vocalScanMode.collectAsStateWithLifecycle()
    val vocalCentroidHz by viewModel.vocalCentroidHz.collectAsStateWithLifecycle()

    val identityBank by viewModel.identityBank.collectAsStateWithLifecycle()
    val savedRequests by viewModel.savedRequests.collectAsStateWithLifecycle()
    val ariActiveOriginal by viewModel.ariActiveOriginal.collectAsStateWithLifecycle()
    val ariActiveSuggestion by viewModel.ariActiveSuggestion.collectAsStateWithLifecycle()
    val ariWhyItWorks by viewModel.ariWhyItWorks.collectAsStateWithLifecycle()

    val masteringLufs by viewModel.masteringLufs.collectAsStateWithLifecycle()
    val masteringStyle by viewModel.masteringStyle.collectAsStateWithLifecycle()

    val integrityHash by viewModel.integrityHash.collectAsStateWithLifecycle()
    val mtbfHours by viewModel.mtbfHours.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                WhisperChipBar(
                    chips = whisperChips,
                    onActionClick = { chip ->
                        when (chip.sourceThread) {
                            "ARi Breeder" -> currentTab = MacSenseNavTab.BREEDING
                            "ARi Lyricist" -> currentTab = MacSenseNavTab.LYRICS
                            "ARi Ear" -> currentTab = MacSenseNavTab.MASTERING
                            else -> currentTab = MacSenseNavTab.ARI_CHAT
                        }
                    },
                    onDismiss = { viewModel.dismissWhisperChip(it) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("macsense_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                MacSenseNavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 9.sp, maxLines = 1) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MacSenseNavTab.VERTICAL_DAW -> VerticalDawScreen(
                    sections = viewModel.sections,
                    activeSectionIndex = activeSectionIndex,
                    currentBar = currentBar,
                    bpm = bpm,
                    isPlaying = isPlaying,
                    tracks = tracks,
                    isRecording = isRecording,
                    recordedDurationMs = recordedDurationMs,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSelectSection = { viewModel.selectSection(it) },
                    onBpmChange = { viewModel.setBpm(it) },
                    onToggleRecording = { viewModel.toggleRecording() }
                )

                MacSenseNavTab.BREEDING -> SoundBreedingScreen(
                    activeGenomes = activeGenomes,
                    parentA = parentA,
                    parentB = parentB,
                    breedWeight = breedWeight,
                    mutationFactor = mutationFactor,
                    lastBredGenome = lastBredGenome,
                    onSelectParentA = { viewModel.selectParentA(it) },
                    onSelectParentB = { viewModel.selectParentB(it) },
                    onBreedWeightChange = { viewModel.setBreedWeight(it) },
                    onMutationChange = { viewModel.setMutationFactor(it) },
                    onBreedClick = { viewModel.breedCurrentParents() },
                    onPreviewGenome = { viewModel.previewGenome(it) }
                )

                MacSenseNavTab.GRAVEYARD -> LazarusGraveyardScreen(
                    extinctGenomes = extinctGenomes,
                    onResurrect = { viewModel.resurrectGenome(it) }
                )

                MacSenseNavTab.LYRICS -> LyricsStudioScreen(
                    lyrics = lyrics,
                    vocalScanMode = vocalScanMode,
                    vocalCentroidHz = vocalCentroidHz,
                    identityBank = identityBank,
                    savedRequests = savedRequests,
                    ariOriginalText = ariActiveOriginal,
                    ariSuggestionText = ariActiveSuggestion,
                    ariWhyItWorks = ariWhyItWorks,
                    onRewriteSpan = { span, mode -> viewModel.rewriteLyricSpan(span, mode) },
                    onScanModeChange = { viewModel.setVocalScanMode(it) },
                    onScanClick = { viewModel.scanVocalReference() },
                    onAskAri = { viewModel.askARiPrompt(it) }
                )

                MacSenseNavTab.MARKETPLACE -> SampleMarketplaceScreen()

                MacSenseNavTab.VERSIONS -> VersionHistoryScreen(nodes = versionNodes)

                MacSenseNavTab.MASTERING -> MasteringScreen(
                    masteringLufs = masteringLufs,
                    masteringStyle = masteringStyle,
                    onProcessMastering = { lufs, style -> viewModel.processMastering(lufs, style) }
                )

                MacSenseNavTab.ARI_CHAT -> MultiAgentScreen(
                    ariAiResponse = ariAiResponse,
                    isAriLoading = isAriLoading,
                    onAskARi = { viewModel.askARiPrompt(it) }
                )

                MacSenseNavTab.PIANO_ROLL -> PianoRollScreen()

                MacSenseNavTab.TELEMETRY -> TelemetryDashboardScreen(
                    integrityHash = integrityHash,
                    mtbfHours = mtbfHours
                )
            }
        }
    }
}
