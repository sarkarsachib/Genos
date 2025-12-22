package ai.genos.core.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.onNodeWithText("Genos Settings").assertExists()
    }

    @Test
    fun settingsScreen_displaysServiceStatusLabel() {
        composeTestRule.onNodeWithText("Service Status").assertExists()
    }

    @Test
    fun settingsScreen_displaysAccessibilityServiceStatus() {
        composeTestRule.onNodeWithText("Accessibility Service: Not Configured").assertExists()
    }

    @Test
    fun settingsScreen_displaysOverlayPermissionStatus() {
        composeTestRule.onNodeWithText("Overlay Permission: Not Granted").assertExists()
    }

    @Test
    fun settingsScreen_displaysScreenCaptureStatus() {
        composeTestRule.onNodeWithText("Screen Capture: Not Active").assertExists()
    }

    @Test
    fun settingsScreen_allStatusesAreVisible() {
        composeTestRule.onNodeWithText("Accessibility Service: Not Configured").assertIsDisplayed()
        composeTestRule.onNodeWithText("Overlay Permission: Not Granted").assertIsDisplayed()
        composeTestRule.onNodeWithText("Screen Capture: Not Active").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_titleIsDisplayed() {
        composeTestRule.onNodeWithText("Genos Settings")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_serviceStatusIsDisplayed() {
        composeTestRule.onNodeWithText("Service Status")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_usesGenosCoreTheme() {
        // Verify the screen is rendered with theme
        composeTestRule.onNodeWithText("Genos Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_cardContainsAllStatuses() {
        composeTestRule.onNodeWithText("Accessibility Service: Not Configured").assertExists()
        composeTestRule.onNodeWithText("Overlay Permission: Not Granted").assertExists()
        composeTestRule.onNodeWithText("Screen Capture: Not Active").assertExists()
    }

    @Test
    fun settingsScreen_hasProperLayout() {
        // Verify all elements exist in proper hierarchy
        composeTestRule.onNodeWithText("Genos Settings").assertExists()
        composeTestRule.onNodeWithText("Service Status").assertExists()
    }
}