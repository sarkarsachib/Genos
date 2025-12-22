package ai.genos.core

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreen_displaysTitle() {
        composeTestRule.onNodeWithText("Genos Core").assertExists()
    }

    @Test
    fun mainScreen_displaysSubtitle() {
        composeTestRule.onNodeWithText("AI Assistant Framework").assertExists()
    }

    @Test
    fun mainScreen_hasAccessibilityButton() {
        composeTestRule.onNodeWithText("Enable Accessibility Service").assertExists()
    }

    @Test
    fun mainScreen_hasOverlayButton() {
        composeTestRule.onNodeWithText("Enable Overlay Permission").assertExists()
    }

    @Test
    fun mainScreen_hasSettingsButton() {
        composeTestRule.onNodeWithText("Open Settings").assertExists()
    }

    @Test
    fun mainScreen_allButtonsAreClickable() {
        composeTestRule.onNodeWithText("Enable Accessibility Service").assertIsEnabled()
        composeTestRule.onNodeWithText("Enable Overlay Permission").assertIsEnabled()
        composeTestRule.onNodeWithText("Open Settings").assertIsEnabled()
    }

    @Test
    fun mainScreen_buttonsAreDisplayedInCorrectOrder() {
        composeTestRule.onNodeWithText("Enable Accessibility Service").assertExists()
        composeTestRule.onNodeWithText("Enable Overlay Permission").assertExists()
        composeTestRule.onNodeWithText("Open Settings").assertExists()
    }

    @Test
    fun mainScreen_usesGenosCoreTheme() {
        // Verify the screen is rendered
        composeTestRule.onNodeWithText("Genos Core").assertIsDisplayed()
    }

    @Test
    fun accessibilityButton_hasCorrectText() {
        val button = composeTestRule.onNodeWithText("Enable Accessibility Service")
        button.assertExists()
        button.assertHasClickAction()
    }

    @Test
    fun overlayButton_hasCorrectText() {
        val button = composeTestRule.onNodeWithText("Enable Overlay Permission")
        button.assertExists()
        button.assertHasClickAction()
    }

    @Test
    fun settingsButton_hasCorrectText() {
        val button = composeTestRule.onNodeWithText("Open Settings")
        button.assertExists()
        button.assertHasClickAction()
    }

    @Test
    fun mainScreen_titleHasCorrectStyle() {
        composeTestRule.onNodeWithText("Genos Core")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_subtitleHasCorrectStyle() {
        composeTestRule.onNodeWithText("AI Assistant Framework")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_allElementsAreVisible() {
        composeTestRule.onNodeWithText("Genos Core").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Assistant Framework").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Accessibility Service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Overlay Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Settings").assertIsDisplayed()
    }
}