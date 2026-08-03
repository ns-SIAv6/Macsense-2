package com.macsense.ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun VerticalDawScreen() {
    Box(modifier = Modifier.fillMaxSize().testTag("vertical_daw_screen")) {
        Text("Vertical DAW")
    }
}
