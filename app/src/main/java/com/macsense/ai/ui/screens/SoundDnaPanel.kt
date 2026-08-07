package com.macsense.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macsense.ai.audio.SoundArchive

/**
 * P5 flagship (issues #37, #61): the Sound DNA share loop — export the selected take's genome
 * as a shareable artifact, and import a friend's artifact to breed against local sounds.
 */
@Composable
fun SoundDnaPanel(
    selectedTakeId: String?,
    exportedArtifact: String?,
    importedEntry: SoundArchive.Entry?,
    onExport: (String) -> Unit,
    onImport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF171226), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("sound_dna_panel")
    ) {
        Button(
            onClick = { selectedTakeId?.let(onExport) },
            enabled = selectedTakeId != null,
            modifier = Modifier.fillMaxWidth().testTag("export_dna_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
        ) {
            Text("EXPORT SELECTED AS SOUND DNA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        exportedArtifact?.let { artifact ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = artifact.lineSequence().take(4).joinToString("\n"),
                color = Color(0xFF9AE6B4),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${artifact.length} chars ready to share",
                color = Color(0xFF8F87A8),
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("Paste Sound DNA to import", fontSize = 11.sp) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("import_dna_field")
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Button(
                onClick = { onImport(importText) },
                enabled = importText.isNotBlank(),
                modifier = Modifier.testTag("import_dna_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE))
            ) {
                Text("IMPORT & ADD TO ARCHIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            importedEntry?.let {
                Text(
                    text = "Imported ${it.takeId.take(8)}… — breed it above",
                    color = Color(0xFF9AE6B4),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
