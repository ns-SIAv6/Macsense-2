package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricSpan
import com.example.ui.theme.*

@Composable
fun LyricsStudioScreen(
    lyrics: List<LyricSpan>,
    vocalScanMode: String,
    vocalCentroidHz: Float,
    identityBank: List<String>,
    savedRequests: List<String>,
    ariOriginalText: String,
    ariSuggestionText: String,
    ariWhyItWorks: List<String>,
    onRewriteSpan: (LyricSpan, String) -> Unit,
    onScanModeChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onAskAri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var topTabState by remember { mutableStateOf(0) } // 0 = SOLO WRITING, 1 = AI ASSISTANCE, 2 = SAVED REQUESTS
    var selectedLyricSpanId by remember { mutableStateOf<String?>(null) }
    var userPromptInput by remember { mutableStateOf("") }
    var askAriInput by remember { mutableStateOf("") }
    var selectedActionChip by remember { mutableStateOf("Strengthen") }

    val mainScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(12.dp)
            .verticalScroll(mainScroll)
    ) {
        // --- TOP WRITING CANVAS HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = CyberPurple,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("WM", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "Vinnie Mac",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Tabs: 1 SOLO WRITING | 2 AI ASSISTANCE | 3 SAVED REQUESTS
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "1 SOLO WRITING\nYour Canvas" to 0,
                    "2 AI ASSISTANCE\nAri is with you" to 1,
                    "3 SAVED REQUESTS\nYour AI Toolkit" to 2
                ).forEach { (label, index) ->
                    val isSelected = topTabState == index
                    Surface(
                        color = if (isSelected) GoldenAmber else DarkSurface,
                        contentColor = if (isSelected) ObsidianBg else TextPrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { topTabState = index }
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Level Badge
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldenAmber)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("LEVEL 23", color = GoldenAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("4,280 / 6,000 XP", color = TextMuted, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- MAIN WORKSPACE GRID ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: Song Lyrics Editor Canvas (60% Width)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Editor Title & BPM Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Untitled Song",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("BPM 94", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("KEY A minor", color = GoldenAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud Sync",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Lyric Editor Area with Line Numbers & Sections
                LazyColumn(
                    modifier = Modifier
                        .height(380.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(lyrics) { lyric ->
                        val isSelected = selectedLyricSpanId == lyric.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) CyberPurple.copy(alpha = 0.25f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedLyricSpanId = lyric.id }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lyric.lineIndex + 1}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(28.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                if (lyric.lineIndex == 0 || lyric.lineIndex == 4 || lyric.lineIndex == 12 || lyric.lineIndex == 20) {
                                    val sectionTag = when (lyric.lineIndex) {
                                        0 -> "[HOOK]"
                                        4 -> "[VERSE 1]"
                                        12 -> "[VERSE 2]"
                                        else -> "[OUTRO]"
                                    }
                                    Text(
                                        text = sectionTag,
                                        color = GoldenAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Text(
                                    text = lyric.text,
                                    color = if (isSelected) CyberCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Formatting Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Undo, "Undo", tint = TextMuted, modifier = Modifier.size(16.dp))
                        Icon(Icons.Default.Redo, "Redo", tint = TextMuted, modifier = Modifier.size(16.dp))
                        Icon(Icons.Default.ContentCut, "Cut", tint = TextMuted, modifier = Modifier.size(16.dp))
                        Icon(Icons.Default.ContentCopy, "Copy", tint = TextMuted, modifier = Modifier.size(16.dp))
                        Icon(Icons.Default.ContentPaste, "Paste", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("B", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("I", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("U", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Floating "SELECT & ASK ARI (HII IYEE DESTINEE)" Toolbar
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SELECT & ASK ARI (HII IYEE DESTINEE)",
                            color = CyberPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Highlight any text and ask Ari to make it stronger.",
                            color = TextMuted,
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val chips = listOf(
                                "Strengthen" to Icons.Default.AutoAwesome,
                                "Rewrite" to Icons.Default.Autorenew,
                                "Change Cadence" to Icons.Default.Equalizer,
                                "Punch Up" to Icons.Default.FlashOn,
                                "Simplify" to Icons.Default.Compress
                            )
                            items(chips) { (title, icon) ->
                                val isSelected = selectedActionChip == title
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedActionChip = title
                                        val selectedSpan = lyrics.firstOrNull { it.id == selectedLyricSpanId } ?: lyrics.first()
                                        onRewriteSpan(selectedSpan, title)
                                    },
                                    label = { Text(title, fontSize = 9.sp) },
                                    leadingIcon = { Icon(icon, title, modifier = Modifier.size(12.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberPurple,
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Input Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userPromptInput,
                                onValueChange = { userPromptInput = it },
                                placeholder = {
                                    Text(
                                        "Ari, I feel like this is a weak point of my lyrics. Can you make this stronger?",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = TextPrimary),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (userPromptInput.isNotBlank()) {
                                        onAskAri(userPromptInput)
                                        userPromptInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(CyberPurple, RoundedCornerShape(8.dp))
                                    .size(44.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = TextPrimary)
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Ari AI Producer / Assistant & Identity Bank & Saved Requests (40% Width)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ARI AI PRODUCER / ASSISTANT BOX
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Ari",
                                    color = CyberPurple,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "AI PRODUCER / ASSISTANT",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ari is ON", color = CyberPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = true,
                                    onCheckedChange = {},
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "✨ Yo Vinnie Mac! 🔥 You're in a creative pocket. Keep riding. Here's some live suggestions for you:",
                            color = TextPrimary,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("🎵 VIBE / CADENCE", color = CyberPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("West Coast Bounce / Hyphy", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Laid back talk rap with a confident flex", color = TextMuted, fontSize = 8.sp)
                                }
                            }

                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("⚡ BPM SUGGESTION", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("90 – 100 BPM", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("You're sitting right in the pocket.", color = TextMuted, fontSize = 8.sp)
                                }
                            }

                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("💡 TIP", color = GoldenAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("Bar 9–12 is strong.", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Try building more punch on bar 13.", color = TextMuted, fontSize = 8.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ask Ari input
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = askAriInput,
                                onValueChange = { askAriInput = it },
                                placeholder = { Text("Ask Ari anything...", fontSize = 10.sp, color = TextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = TextPrimary),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (askAriInput.isNotBlank()) {
                                        onAskAri(askAriInput)
                                        askAriInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(CyberPurple, RoundedCornerShape(6.dp))
                                    .size(40.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // 2. ARI OUTPUT / RESULTS PANEL
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👁 Hii iyee destinee Results", color = CyberPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("What changed? ⓘ", color = TextMuted, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ORIGINAL", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = ariOriginalText,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianBg, RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ARI SUGGESTION ", color = CyberPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Surface(color = EmeraldGreen, shape = RoundedCornerShape(4.dp)) {
                                Text("+ Stronger", color = ObsidianBg, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ariSuggestionText,
                            color = CyberPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianBg, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Why this works:", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        ariWhyItWorks.forEach { reason ->
                            Text("✔ $reason", color = TextMuted, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Apply", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Try Another", fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                // 3. MY IDENTITY BANK & SAVED REQUESTS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Identity Bank Card
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MY IDENTITY BANK", color = GoldenAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.BookmarkBorder, "Bookmark", tint = GoldenAmber, modifier = Modifier.size(12.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier.height(110.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(identityBank) { line ->
                                    Text(
                                        text = "• $line",
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Saved Requests Card
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SAVED REQUESTS", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.BookmarkBorder, "Bookmark", tint = CyberCyan, modifier = Modifier.size(12.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier.height(110.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(savedRequests) { req ->
                                    Surface(
                                        color = DarkSurfaceVariant,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAskAri(req) }
                                    ) {
                                        Text(
                                            text = req,
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- BOTTOM CREATIVE STATS BAR ---
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "YOUR CREATIVE STATS   Private 🔒",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Creator Ratio Ring Gauge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = CyberPurple,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("78%", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text("78% CREATOR RATIO", color = GoldenAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("More you create than you ask Ari.", color = TextMuted, fontSize = 8.sp)
                        }
                    }

                    // Stat Blocks
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("THIS SESSION", color = TextMuted, fontSize = 8.sp)
                            Text("+320 XP", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Great writing!", color = TextMuted, fontSize = 8.sp)
                        }

                        Column {
                            Text("WORDS WRITTEN", color = TextMuted, fontSize = 8.sp)
                            Text("642", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("▲ 18%", color = EmeraldGreen, fontSize = 8.sp)
                        }

                        Column {
                            Text("AI ASSIST USE", color = TextMuted, fontSize = 8.sp)
                            Text("142", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("▼ 12%", color = CrimsonRed, fontSize = 8.sp)
                        }

                        Column {
                            Text("CREATIVE HITS", color = TextMuted, fontSize = 8.sp)
                            Text("9", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("💎 Double Entendre", color = GoldenAmber, fontSize = 8.sp)
                        }

                        Column {
                            Text("CADENCE CHANGES", color = TextMuted, fontSize = 8.sp)
                            Text("6", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("🎵 Nice flow switches!", color = CyberCyan, fontSize = 8.sp)
                        }

                        Column {
                            Text("BEST LINE", color = TextMuted, fontSize = 8.sp)
                            Text("\"I do this, I knew this, the Mac never departed.\"", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("ALL TIME", color = TextMuted, fontSize = 8.sp)
                            Text("LEVEL 23", color = GoldenAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("4,280 / 6,000 XP", color = TextMuted, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}
