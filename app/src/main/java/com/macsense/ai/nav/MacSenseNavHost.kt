package com.macsense.ai.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.macsense.ai.ui.screens.*

@Composable
fun MacSenseNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DAW) {
        composable(Routes.DAW) { VerticalDawScreen() }
        composable(Routes.FLOW_CAPTURE) { FlowCaptureScreen() }
        composable(Routes.VOCAL_SCANNER) { VocalScannerScreen() }
        composable(Routes.MASTERING) { MasteringScreen() }
        composable(Routes.LYRICS_STUDIO) { LyricsStudioScreen() }
    }
}
