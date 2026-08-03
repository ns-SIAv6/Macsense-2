package com.macsense.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsStudioScreen() {
    var lyricsText by remember { mutableStateOf(
        "Yeah, double cup spilling on the MPC\n" +
        "Beat so hard, MACSENSE setting me free\n" +
        "Riding on the wave, neon in the night\n" +
        "Spitting raw science till the morning light"
    ) }

    var selectedRhymeScheme by remember { mutableStateOf("AABB") }
    var currentWordIndex by remember { mutableStateOf(0) }
    var isPlayingSync by remember { mutableStateOf(false) }

    val wordsList = remember(lyricsText) {
        lyricsText.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    val rhymeSuggestions = remember(currentWordIndex) {
        listOf("MPC", "Free", "Key", "Degree", "G", "Legacy")
    }

    // Sync progress looping coroutine
    LaunchedEffect(isPlayingSync) {
        if (isPlayingSync) {
            while (isPlayingSync) {
                delay(400) // progress through words every 400ms
                if (wordsList.isNotEmpty()) {
                    currentWordIndex = (currentWordIndex + 1) % wordsList.size
                }
            }
        } else {
            currentWordIndex = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✍ ", color = MagentaNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MACSENSE LYRICS STUDIO", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = Modifier.testTag("lyrics_studio_screen")
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Lyrics Editor & Synced Prompt
            Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Text input editor
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1FA855F7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "LYRICS BLUEPRINT COMPOSER",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lyricsText,
                            onValueChange = { lyricsText = it },
                            modifier = Modifier.fillMaxSize(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = BackgroundDark,
                                unfocusedContainerColor = BackgroundDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }

                // Word Synced Visualizer Card
                Card(
                    modifier = Modifier.weight(0.8f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1F06B6D4)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "REAL-TIME TELEPROMPTER & SYNCHRONIZER",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { isPlayingSync = !isPlayingSync },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isPlayingSync) Color.Red else CyanNeon)
                            ) {
                                Text(
                                    text = if (isPlayingSync) "‖" else "▶",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Word flow wrap panel
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(BorderStroke(1.dp, Color(0x0FA855F7)), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            FlowTextHighlight(words = wordsList, activeIndex = currentWordIndex)
                        }
                    }
                }
            }

            // Right Column: AI Rhyme Assistant
            Column(modifier = Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0x1FA855F7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "AI RHYME ENGINE & COMPANION",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Rhyme Scheme Selector
                        Column {
                            Text("Rhyme Scheme Target", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("AABB", "ABAB", "AAAA", "ABBA").forEach { scheme ->
                                    val isSelected = selectedRhymeScheme == scheme
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PurpleNeon else SurfaceSubtle)
                                            .border(BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent))
                                            .clickable { selectedRhymeScheme = scheme }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(scheme, color = if (isSelected) Color.White else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0x1FA855F7))

                        // Selected Word Info
                        val activeWord = if (wordsList.isNotEmpty() && currentWordIndex in wordsList.indices) wordsList[currentWordIndex] else "None"
                        Column {
                            Text("Active Sync Word", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                activeWord,
                                color = CyanNeon,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Divider(color = Color(0x1FA855F7))

                        // Rhyme suggestions List
                        Text("Suggested Rhyming Words", color = TextSecondary, fontSize = 11.sp)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(rhymeSuggestions) { _, rhyme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceSubtle)
                                        .clickable {
                                            // Append suggestion to lyrics
                                            lyricsText = "$lyricsText $rhyme"
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rhyme, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Add, contentDescription = "Add Word", tint = MagentaNeon, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowTextHighlight(words: List<String>, activeIndex: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Render text flowing, highlight active index word
                var wordCounter = 0
                val chunks = words.chunked(6)
                chunks.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        chunk.forEach { word ->
                            val isCurrent = wordCounter == activeIndex
                            val infiniteTransition = rememberInfiniteTransition(label = "")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.12f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(250, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = ""
                            )
                            val wordColor = if (isCurrent) CyanNeon else TextPrimary
                            val weight = if (isCurrent) FontWeight.Black else FontWeight.Normal

                            Text(
                                text = "$word ",
                                color = wordColor,
                                fontWeight = weight,
                                fontSize = if (isCurrent) 17.sp else 14.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            wordCounter++
                        }
                    }
                }
            }
        }
    }
}
