package ca.stewark.nocturnel.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.theme.FontPreset
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
                    onCycleFontPreset = {},
                    onOpenPrivacyPolicy = { opened = true },
                )
            }
        }

        compose.onNodeWithText("[ PRIVACY POLICY ]").assertIsDisplayed().performClick()
        assertTrue(opened)
    }

    @Test fun fontPresetControlShowsCurrentPresetAndCyclesOnce() {
        var cycles = 0
        compose.setContent {
            NocturneLTheme(FontPreset.PIXEL) {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = {},
                    state = TerminalSettingsState(fontPreset = FontPreset.PIXEL),
                    onEffectsChanged = {},
                    onCycleFontPreset = { cycles++ },
                )
            }
        }

        compose.onNodeWithText("FONT PRESET: PIXEL").assertIsDisplayed()
        compose.onNodeWithText("[ NEXT ]").assertIsDisplayed().performClick()
        assertTrue(cycles == 1)
    }

    @Test fun fontPresetControlCanScrollIntoViewOnAConstrainedScreen() {
        var cycled = false
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(width = 320.dp, height = 180.dp)) {
                    SettingsScreen(
                        onChooseFolder = {},
                        onRescan = {},
                        state = TerminalSettingsState(),
                        onEffectsChanged = {},
                        onCycleFontPreset = { cycled = true },
                    )
                }
            }
        }

        compose.onNodeWithText("[ NEXT ]").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(cycled)
    }
}
