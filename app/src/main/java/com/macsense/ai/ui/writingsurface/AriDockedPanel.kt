package com.macsense.ai.ui.writingsurface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macsense.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriDockedPanel(
    chatLog: List<ChatMessage>,
    isAriTyping: Boolean,
    onSendMessage: (String) -> Unit,
    selectedTextRange: TextSelection?,
    onTriggerLyricEdit: (String) -> Unit,
    isGeneratingEdit: Boolean,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxHeight()
            .testTag("ari_docked_panel"),
        colors = CardDefaults.cardColors(containerColor = MacsensePanelPurple),
        border = androidx.compose.foundation.BorderStroke(1.dp, MacsenseBorderPurple),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MacsenseGoldPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ARI CO-PRODUCER CHAT",
                    color = MacsenseTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // High-priority selection panel (Contextual floating edit actions)
            if (selectedTextRange != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MacsenseCardPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, MacsenseGoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .testTag("contextual_rewrite_panel"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "SELECTED: \"${selectedTextRange.text.take(30)}${if (selectedTextRange.text.length > 30) "..." else ""}\"",
                            color = MacsenseGoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isGeneratingEdit) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MacsenseGoldPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val actions = listOf("Rewrite", "Make more aggressive", "Improve rhyme", "Better cadence", "Change flow")
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                ) {
                                    items(actions) { action ->
                                        Button(
                                            onClick = { onTriggerLyricEdit(action) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MacsenseBorderPurple),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .testTag("edit_action_$action"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = action.uppercase(),
                                                color = MacsenseTextPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
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

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("ari_chat_messages"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(chatLog) { message ->
                    val isAri = message.role == "assistant"
                    val bg = if (isAri) MacsenseCardPurple else MacsenseBorderPurple
                    val alignment = if (isAri) Alignment.Start else Alignment.End
                    val textColor = if (isAri) MacsenseTextPrimary else MacsenseGoldPrimary

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = if (isAri) 0.dp else 8.dp,
                                        bottomEnd = if (isAri) 8.dp else 0.dp
                                    )
                                )
                                .background(bg)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isAri) "ARI" else "YOU",
                                    color = textColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = message.text,
                                    color = MacsenseTextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (isAriTyping) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MacsenseCardPurple)
                                .padding(8.dp)
                                .testTag("ari_typing_indicator")
                        ) {
                            Text(
                                text = "ari is typing...",
                                color = MacsenseTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Quick Chips (Preset chat helpers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Critique", "Suggest Vibe", "Help Me").forEach { chip ->
                    Box(
                        modifier = Modifier
                            .background(MacsenseVoidBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, MacsenseBorderPurple, RoundedCornerShape(12.dp))
                            .clickable { onSendMessage(chip) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("ari_chip_$chip")
                    ) {
                        Text(
                            text = chip.uppercase(),
                            color = MacsenseTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Ari...", color = MacsenseTextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ari_chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MacsenseVoidBlack,
                        unfocusedContainerColor = MacsenseVoidBlack,
                        focusedBorderColor = MacsenseGoldPrimary,
                        unfocusedBorderColor = MacsenseBorderPurple,
                        focusedTextColor = MacsenseTextPrimary,
                        unfocusedTextColor = MacsenseTextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MacsenseGoldPrimary, RoundedCornerShape(8.dp))
                        .testTag("ari_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = MacsenseVoidBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
