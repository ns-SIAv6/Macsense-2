package com.macsense.ai.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onChildren
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.macsense.ai.nav.MacSenseNavHost
import com.macsense.ai.nav.Routes
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationReachabilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyAllRoutesAreReachable() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            MacSenseNavHost(navController = navController)
        }
        
        val routes = listOf(
            Routes.DAW,
            Routes.FLOW_CAPTURE,
            Routes.VOCAL_SCANNER,
            Routes.MASTERING,
            Routes.LYRICS_STUDIO
        )

        for (route in routes) {
            composeTestRule.runOnUiThread {
                navController.navigate(route) {
                    popUpTo(0)
                }
            }
            composeTestRule.waitForIdle()
            
            val children = composeTestRule.onRoot().onChildren()
            val count = children.fetchSemanticsNodes().size
            assertTrue("Route $route rendered an empty node (0 children)", count > 0)
        }
    }
}
