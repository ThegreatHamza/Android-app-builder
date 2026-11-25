package com.hamzaapps.hifzpath

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the real app through every screen on a simulated device (Robolectric). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) = rule.waitUntilAtLeastOneExists(hasText(text, substring = true), 10_000)
    private fun waitForTag(tag: String) = rule.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)

    @Test
    fun fullFlow() {
        // Setup: empty name is rejected, then a student is created.
        waitForTag("setup_start")
        rule.onNodeWithTag("setup_start").performClick()
        rule.onNodeWithText("Please enter a name.").assertIsDisplayed()
        rule.onNodeWithTag("setup_name").performTextInput("Abdullah")
        rule.onNodeWithTag("setup_start").performClick()

        // Today: default plan starts at An-Nas (page 604).
        waitForTag("do_SABAQ_604")
        rule.onNodeWithText("Today · Abdullah").assertIsDisplayed()
        rule.onNodeWithTag("do_SABAQ_604").performClick()
        rule.onNodeWithText("Strong").performClick()
        waitFor("Undo")
        // Daily target is ½ page, so after one page today no further new page is offered.
        rule.onNodeWithText("Today: 1 of 1 done").assertIsDisplayed()

        // Mushaf: page grid for juz 30, open a page and mark it as learning.
        rule.onNodeWithText("Mushaf").performClick()
        waitForTag("page_grid")
        rule.onNodeWithTag("page_grid").performScrollToNode(hasTestTag("page_590"))
        rule.onNodeWithTag("page_590").performClick()
        waitForTag("status_LEARNING")
        rule.onNodeWithTag("status_LEARNING").performClick()
        waitFor("Juz 30: 1 of 23 pages memorized")

        // Surahs: search by number.
        rule.onNodeWithText("Surahs").performClick()
        waitForTag("surah_search")
        rule.onNodeWithTag("surah_search").performTextInput("36")
        waitFor("83 ayahs")

        // Progress.
        rule.onNodeWithText("Progress").performClick()
        waitForTag("progress_screen")
        rule.onNodeWithText("Juz done").assertIsDisplayed()

        // Students: free version allows one student, so adding prompts for Pro.
        rule.onNodeWithContentDescription("Students").performClick()
        waitForTag("add_student")
        rule.onNodeWithTag("add_student").performClick()
        waitFor("The free version has one student")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithContentDescription("Back").performClick()

        // Settings.
        rule.onNodeWithContentDescription("Settings").performClick()
        waitForTag("settings_screen")
        rule.onNodeWithText("Hifz Path Pro").assertIsDisplayed()
        rule.onNodeWithText("Save plan").assertExists()
    }
}
