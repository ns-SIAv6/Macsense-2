package com.macsense.ai.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.macsense.ai.data.repository.MacSenseRepository
import com.macsense.ai.ui.screens.*
import com.macsense.ai.ui.viewmodel.DawViewModel
import com.macsense.ai.ui.viewmodel.DawViewModelFactory

/**
 * [repository] is optional so existing callers/tests (e.g. [NavigationReachabilityTest]) that
 * construct this host without a real [MacSenseRepository] keep working unchanged — [DawViewModel]
 * simply falls back to its `repository = null` default and the sound-genetics Ari commands
 * become no-ops, exactly as before. Production call sites (see [com.macsense.ai.MainActivity])
 * should always pass the real repository from `AppContainer` so `breed_sounds`/`resurrect_sound`
 * actually persist. The same factory-or-default pattern is reused for the [Routes.BREEDING]
 * destination so [BreedingScreen] shares the exact same [DawViewModel] instance/backing store
 * conventions as the DAW screen.
 */
@Composable
fun MacSenseNavHost(navController: NavHostController, repository: MacSenseRepository? = null) {
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Routes.DAW) {
        composable(Routes.DAW) {
            val dawViewModel: DawViewModel = if (repository != null) {
                viewModel(factory = DawViewModelFactory(repository, context = context))
            } else {
                viewModel()
            }
            VerticalDawScreen(viewModel = dawViewModel)
        }
        composable(Routes.FLOW_CAPTURE) { FlowCaptureScreen() }
        composable(Routes.VOCAL_SCANNER) { VocalScannerScreen() }
        composable(Routes.MASTERING) { MasteringScreen() }
        composable(Routes.LYRICS_STUDIO) { LyricsStudioScreen() }
        composable(Routes.BREEDING) {
            val breedingViewModel: DawViewModel = if (repository != null) {
                viewModel(factory = DawViewModelFactory(repository, context = context))
            } else {
                viewModel()
            }
            BreedingScreen(viewModel = breedingViewModel)
        }
    }
}
