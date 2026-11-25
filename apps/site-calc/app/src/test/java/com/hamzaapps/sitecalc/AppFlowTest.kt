package com.hamzaapps.sitecalc

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the real app through every screen on a simulated device (Robolectric). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) = rule.waitUntilAtLeastOneExists(hasText(text, substring = true), 10_000)
    private fun waitForTag(tag: String) = rule.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)


    @Test
    fun fullFlow() {
        waitForTag("home_grid")

        // Concrete slab 5 x 4 x 0.15 m with a cement price.
        rule.onNodeWithTag("open_CONCRETE").performClick()
        waitForTag("input_length")
        rule.onNodeWithText("Enter the dimensions to see the materials you need.").assertExists()
        rule.onNodeWithTag("input_length").performTextInput("5")
        rule.onNodeWithTag("input_width").performTextInput("4")
        rule.onNodeWithTag("input_waste").performTextClearance()
        rule.onNodeWithTag("input_waste").performTextInput("0")
        rule.onNodeWithText("Prices (optional)").performScrollTo().performClick()
        rule.onNodeWithTag("price_CEMENT_BAG").performScrollTo().performTextInput("10")
        waitForTag("results_card")
        rule.onNodeWithText("20 bags").performScrollTo().assertIsDisplayed()

        // Invalid input shows an error instead of results.
        rule.onNodeWithTag("input_width").performScrollTo().performTextClearance()
        rule.onNodeWithTag("input_width").performTextInput("abc")
        waitFor("Check the highlighted value.")
        rule.onNodeWithTag("input_width").performTextClearance()
        rule.onNodeWithTag("input_width").performTextInput("4")
        waitForTag("results_card")

        // Save into a new project; this opens the project.
        rule.onNodeWithTag("save_calc").performScrollTo().performClick()
        rule.onNodeWithTag("new_project_name").performTextInput("Test House")
        rule.onNodeWithTag("confirm_save").performClick()
        waitForTag("project_total")
        rule.onNodeWithText("Test House").assertIsDisplayed()
        rule.onNodeWithTag("project_total").assertTextContains("200.00", substring = true)

        // PDF is a Pro feature.
        rule.onNodeWithTag("export_pdf").performClick()
        waitFor("PDF reports are part of Pro")
        rule.onNodeWithText("Cancel").performClick()

        // Back to the calculators, then the Projects tab lists the project.
        rule.onNodeWithContentDescription("Back").performClick()
        waitForTag("home_grid")
        rule.onNodeWithText("Projects").performClick()
        waitForTag("project_list")
        rule.onNodeWithText("1 item").assertIsDisplayed()

        // Second project is allowed, third needs Pro.
        rule.onNodeWithTag("fab_new_project").performClick()
        rule.onNodeWithTag("name_field").performTextInput("Shop")
        rule.onNodeWithTag("name_save").performClick()
        waitFor("This project is empty")
        rule.onNodeWithContentDescription("Back").performClick()
        waitForTag("fab_new_project")
        rule.onNodeWithTag("fab_new_project").performClick()
        waitFor("The free version holds 2 projects")
        rule.onNodeWithText("OK").performClick()

        // Every other calculator opens and computes with its defaults.
        rule.onNodeWithText("Calculators").performClick()
        waitForTag("home_grid")
        rule.onNodeWithTag("home_grid").performScrollToNode(hasTestTag("open_PAINT"))
        rule.onNodeWithTag("open_PAINT").performClick()
        waitForTag("input_area")
        rule.onNodeWithTag("input_area").performTextInput("50")
        waitFor("10 L") // 50 m² x 2 coats / 10 m²/L
        rule.onNodeWithContentDescription("Back").performClick()

        // Settings.
        waitForTag("home_grid")
        rule.onNodeWithContentDescription("Settings").performClick()
        waitForTag("settings_screen")
        rule.onNodeWithText("Site Calc Pro").assertIsDisplayed()
        rule.onNodeWithTag("currency").performTextInput("KES")
    }
}
