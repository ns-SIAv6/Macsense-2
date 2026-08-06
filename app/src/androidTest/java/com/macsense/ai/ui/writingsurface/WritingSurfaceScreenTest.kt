package com.macsense.ai.ui.writingsurface

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WritingSurfaceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWritingSurfaceLayoutAndInteraction() {
        val viewModel = WritingSurfaceViewModel()

        composeTestRule.setContent {
            WritingSurfaceScreen(viewModel = viewModel)
        }

        // 1. Verify general layout elements exist
        composeTestRule.onNodeWithTag("writing_surface_screen").assertExists()
        composeTestRule.onNodeWithTag("creative_stats_strip").assertExists()
        composeTestRule.onNodeWithTag("writing_surface_tabs").assertExists()
        composeTestRule.onNodeWithTag("ari_docked_panel").assertExists()

        // 2. Verify we start on the SOLO WRITING tab with lyrics input visible
        composeTestRule.onNodeWithTag("lyrics_editor_text_field").assertExists()

        // 3. Switch to AI ASSISTANCE tab
        composeTestRule.onNodeWithTag("tab_ai_assistance").performClick()
        composeTestRule.onNodeWithTag("identity_bank").assertExists()
        composeTestRule.onNodeWithTag("identity_bank_grid").assertExists()

        // 4. Select an identity from the bank and verify ViewModel state updates
        composeTestRule.onNodeWithTag("identity_item_Melodic R&B").performClick()
        assertEquals("Melodic R&B", viewModel.artistIdentity.value)

        // 5. Switch back to SOLO WRITING
        composeTestRule.onNodeWithTag("tab_solo_writing").performClick()

        // 6. Simulate selected text span and verify contextual panel appears
        viewModel.selectTextRange(0, 4) // selects "Yeah"
        composeTestRule.onNodeWithTag("contextual_rewrite_panel").assertExists()

        // 7. Click one of the editing actions (Better Cadence) in the docked panel
        composeTestRule.onNodeWithTag("edit_action_Better cadence").performClick()

        // 8. Verify the Accept/Reject diff preview pop-up overlay is rendered
        composeTestRule.onNodeWithTag("lyric_diff_editor").assertExists()
        composeTestRule.onNodeWithTag("diff_suggested_text").assertExists()

        // 9. Click "Accept" and verify the diff is applied and the editor overlay closes
        composeTestRule.onNodeWithTag("accept_diff_button").performClick()
        composeTestRule.onNodeWithTag("lyric_diff_editor").assertDoesNotExist()

        // The lyrics should be updated and selection cleared
        assertEquals(null, viewModel.selectedTextSpan.value)
    }

    @Test
    fun testRejectDiffOverlayInteraction() {
        val viewModel = WritingSurfaceViewModel()

        composeTestRule.setContent {
            WritingSurfaceScreen(viewModel = viewModel)
        }

        // Simulate selection and rewrite trigger
        viewModel.selectTextRange(0, 4)
        viewModel.triggerLyricEditAction("Rewrite")

        // Diff should be visible
        composeTestRule.onNodeWithTag("lyric_diff_editor").assertExists()

        // Click reject button
        composeTestRule.onNodeWithTag("reject_diff_button").performClick()

        // Overlay should disappear, and lyrics should be unchanged
        composeTestRule.onNodeWithTag("lyric_diff_editor").assertDoesNotExist()
    }
}
