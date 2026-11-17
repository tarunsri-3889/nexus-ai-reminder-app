package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches NEXUS AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("NEXUS AI", context.getString(R.string.app_name))
    }

    @Test
    fun `dashboard shows today overview timeline and creates assignment reminder`() {
        composeTestRule.waitForIdle()

        // No permission dialogs on launch; Today Overview renders immediately
        composeTestRule.onNodeWithText("NEXUS AI").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("today_overview_card"))
        composeTestRule.onNodeWithTag("today_overview_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("today_progress_text").assertIsDisplayed()

        // Create a reminder scheduled for today (+30 min) so it enters Today Timeline
        composeTestRule.onNodeWithTag("new_reminder_fab").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("composer_input")
            .performTextInput("In 30 minutes finish my mathematics assignment")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("NEXUS Parsed Preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("schedule_reminder_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule
                .onAllNodesWithText("Finish my mathematics assignment")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Not Now").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Not Now").performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("today_timeline_section"))
        composeTestRule.onNodeWithTag("today_timeline_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("timeline_status_legend").assertIsDisplayed()
        composeTestRule.onNodeWithTag("timeline_indicator_1_upcoming").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("pending_assignments_section"))
        composeTestRule.onNodeWithTag("pending_assignments_section").assertIsDisplayed()

        // Complete Task 1 directly from Today's Timeline and verify 100% progress + completed indicator
        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("today_timeline_section"))
        composeTestRule.onNode(
            hasText("Complete").and(hasAnyAncestor(hasTestTag("today_timeline_section")))
        ).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule
                .onAllNodesWithText("Completed")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("timeline_indicator_1_completed"))
        composeTestRule.onNodeWithTag("timeline_indicator_1_completed").assertIsDisplayed()

        composeTestRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("today_overview_card"))
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithText("1/1 completed").assertIsDisplayed()
    }
}
