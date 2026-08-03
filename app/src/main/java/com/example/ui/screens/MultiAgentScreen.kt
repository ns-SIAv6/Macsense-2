package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MultiAgentMessage
import com.example.ui.theme.*

@Composable
fun MultiAgentScreen(
    ariAiResponse: String,
    isAriLoading: Boolean,
    onAskARi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var userPrompt by remember { mutableStateOf("") }

    val messages = remember(ariAiResponse) {
        listOf(
            MultiAgentMessage("m1", "ARi Breeder Thread", "Breeder", "Analyzed sound topology: 808 mass is at 92%. Recommending a high-radiance snare hybrid."),
            MultiAgentMessage("m2", "ARi Lyricist Thread", "Lyricist", "Cadence alignment fits a syncopated 16th note trap scheme in Verse 1."),
            MultiAgentMessage("m3", "ARi Ear Thread", "Engineer", "Mastering limiter target is set to -14.0 LUFS. Dynamic range is clean.")
        ) + if (ariAiResponse.isNotEmpty()) listOf(
            MultiAgentMessage("m4", "ARi Co-Producer Mind", "Master Mind", ariAiResponse)
        ) else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ARI MULTI-AGENT CO-SESSION",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Breeder • Lyricist • Engineer Collaborative Chat (Suno Parity)",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Icon(imageVector = Icons.Default.Psychology, contentDescription = "ARi Mind", tint = CyberPurple, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-Agent Chat Messages Feed
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { msg ->
                val agentColor = when (msg.avatarRole) {
                    "Breeder" -> CyberCyan
                    "Lyricist" -> NeonMagenta
                    "Engineer" -> GoldenAmber
                    else -> CyberPurple
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, agentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(agentColor, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = msg.agentName, color = agentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = msg.content, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                placeholder = { Text("Ask ARi for production, breeding, or lyric advice...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ari_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = CardBorder,
                    focusedBorderColor = CyberCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (userPrompt.isNotBlank()) {
                        onAskARi(userPrompt)
                        userPrompt = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(CyberPurple, RoundedCornerShape(12.dp))
                    .testTag("send_ari_prompt_btn")
            ) {
                if (isAriLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = TextPrimary)
                }
            }
        }
    }
}
