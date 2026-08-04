package com.macsense.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.macsense.ai.nav.MacSenseNavHost
import com.macsense.ai.ui.theme.MacSenseTheme

class MainActivity : ComponentActivity() {
    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The capture screen observes permission state when starting a take. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        // First real production wiring of AppContainer.repository into the DAW screen: without
        // this, DawViewModel falls back to repository = null and breed_sounds/resurrect_sound
        // Ari commands silently no-op instead of persisting through Room.
        val repository = (application as MacSenseApplication).container.repository
        setContent {
            MacSenseTheme {
                val navController = rememberNavController()
                MacSenseNavHost(navController, repository = repository)
            }
        }
    }
}
