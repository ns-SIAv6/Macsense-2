package com.macsense.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.macsense.ai.nav.MacSenseNavHost
import com.macsense.ai.ui.theme.MacSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MacSenseTheme {
                val navController = rememberNavController()
                MacSenseNavHost(navController)
            }
        }
    }
}
