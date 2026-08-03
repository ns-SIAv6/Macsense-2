package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.VoiceCommandManager
import com.example.ui.theme.*

@Composable
fun VoiceCommandBar(
    voiceState: VoiceCommandManager.VoiceState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpokenCommandReady: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            onStartListening()
        }
    }

    // Automatically trigger Gemini command processing when voice recognition finishes
    LaunchedEffect(voiceState.lastSpokenText) {
        if (voiceState.lastSpokenText.isNotBlank()) {
            onSpokenCommandReady(voiceState.lastSpokenText)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                width = if (voiceState.isListening) 1.5.dp else 1.dp,
                color = if (voiceState.isListening) CyberCyan else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("voice_command_bar"),
        color = DarkSurfaceVariant.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Microphone Trigger Button
                    IconButton(
                        onClick = {
                            if (voiceState.isListening) {
                                onStopListening()
                            } else {
                                if (hasMicPermission) {
                                    onStartListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (voiceState.isListening) CrimsonRed else CyberCyan,
                                shape = CircleShape
                            )
                            .testTag("mic_voice_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Ari Voice Command Microphone",
                            tint = ObsidianBg,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ARI VOICE CONTROL",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (voiceState.isListening) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Listening Wave",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val statusText = when {
                            voiceState.isListening -> "Listening... Speak to Ari"
                            voiceState.isProcessing -> "Ari executing voice command..."
                            voiceState.lastSpokenText.isNotEmpty() -> "\"${voiceState.lastSpokenText}\""
                            voiceState.errorMessage != null -> voiceState.errorMessage
                            else -> "Tap mic & speak natural DAW commands"
                        }

                        Text(
                            text = statusText,
                            color = if (voiceState.isListening) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (voiceState.isListening) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }

                if (voiceState.lastExecutedAction.isNotEmpty()) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Executed",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = voiceState.lastExecutedAction,
                                color = EmeraldGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Spoken Suggestion Chips
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val suggestions = listOf(
                    "Ari, increase attack on synth track",
                    "Ari, set tempo to 128 BPM",
                    "Ari, boost volume on sub bass",
                    "Ari, breed selected sound genomes",
                    "Ari, master track at -14 LUFS"
                )
                items(suggestions) { phrase ->
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(0.5.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { onSpokenCommandReady(phrase) }
                    ) {
                        Text(
                            text = phrase,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
