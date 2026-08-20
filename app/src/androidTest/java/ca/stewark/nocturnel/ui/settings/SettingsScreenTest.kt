package ca.stewark.nocturnel.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun privacyPolicyButtonOpensPublicPolicy() {
        var opened = false
        compose.setContent {
            NocturneLTheme {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = {},
                    state = TerminalSettingsState(),
                    onEffectsChanged = {},
                    onOpenPrivacyPolicy = { opened = true },
                )
            }
        }

        compose.onNodeWithText("[ PRIVACY POLICY ]").assertIsDisplayed().performClick()
        assertTrue(opened)
    }
}
