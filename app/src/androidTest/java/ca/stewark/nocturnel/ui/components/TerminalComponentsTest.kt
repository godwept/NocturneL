package ca.stewark.nocturnel.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Rule
import org.junit.Test

class TerminalComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bracketButtonClicks() {
        var clicked = false
        compose.setContent { NocturneLTheme { BracketButton("PLAY", { clicked = true }) } }
        compose.onNodeWithText("[ PLAY ]").assertIsDisplayed().assertHasClickAction().performClick()
        assert(clicked)
    }

    @Test fun toggleReportsState() {
        var checked = false
        compose.setContent { NocturneLTheme { TerminalToggle("EFFECTS", checked, { checked = it }) } }
        compose.onNodeWithText("EFFECTS").assertIsOff().performClick().assertIsOn()
    }
}
