package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PianoRollScreen(
    modifier: Modifier = Modifier
) {
    val notes = listOf("C5", "B4", "A4", "G4", "F4", "E4", "D4", "C4")
    var gridState by remember { mutableStateOf(Array(8) { BooleanArray(16) { it % 4 == 0 } }) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
    ) {
        Text(text = "MIDI PIANO ROLL & STEP SEQUENCER", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = "Step Sequencer Grid (FL Studio / Logic Parity)", color = TextMuted, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(17),
            modifier = Modifier.weight(1f)
        ) {
            items(17 * 8) { index ->
                val row = index / 17
                val col = index % 17

                if (col == 0) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = notes[row], color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    val stepCol = col - 1
                    val isActive = gridState[row][stepCol]

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(1.dp)
                            .background(if (isActive) CyberCyan else DarkSurface, RoundedCornerShape(4.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                val updated = gridState.map { it.clone() }.toTypedArray()
                                updated[row][stepCol] = !updated[row][stepCol]
                                gridState = updated
                            }
                            .testTag("piano_grid_${row}_${stepCol}")
                    )
                }
            }
        }
    }
}
