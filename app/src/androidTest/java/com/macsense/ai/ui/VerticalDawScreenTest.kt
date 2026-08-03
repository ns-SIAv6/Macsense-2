package com.macsense.ai.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.macsense.ai.ui.screens.VerticalDawScreen
import org.junit.Rule
import org.junit.Test

class VerticalDawScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysScreen() {
        composeTestRule.setContent {
            VerticalDawScreen()
        }
        composeTestRule.onNodeWithTag("vertical_daw_screen").assertExists()
    }
}
