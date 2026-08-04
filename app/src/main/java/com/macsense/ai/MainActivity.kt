package com.macsense.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.macsense.ai.di.AppContainer
import com.macsense.ai.nav.MacSenseNavHost
import com.macsense.ai.ui.theme.MacSenseTheme

class MainActivity : ComponentActivity() {
    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The capture screen observes permission state when starting a take. */ }

    private val appContainer: AppContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            MacSenseTheme {
                val navController = rememberNavController()
                // Wires the real MacSenseRepository into DawViewModel so takes captured on the
                // DAW screen are actually persisted (genome extraction, breeding, resurrection)
                // instead of only updating in-memory StateFlows.
                MacSenseNavHost(navController, repository = appContainer.repository)
            }
        }
    }
}
