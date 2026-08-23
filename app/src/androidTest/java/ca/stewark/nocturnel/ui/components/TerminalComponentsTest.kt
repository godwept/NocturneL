package ca.stewark.nocturnel.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.R
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import ca.stewark.nocturnel.ui.theme.FontPreset
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

    @Test fun classicFontKeepsEveryPrimaryNavigationTabVisibleAt320Dp() = assertPrimaryTabsVisible(FontPreset.CLASSIC)

    @Test fun mainframeFontKeepsEveryPrimaryNavigationTabVisibleAt320Dp() = assertPrimaryTabsVisible(FontPreset.MAINFRAME)

    @Test fun pixelFontKeepsEveryPrimaryNavigationTabVisibleAt320Dp() = assertPrimaryTabsVisible(FontPreset.PIXEL)

    @Test fun modernFontKeepsEveryPrimaryNavigationTabVisibleAt320Dp() = assertPrimaryTabsVisible(FontPreset.MODERN)

    @Test fun enlargedPixelFontKeepsEveryPrimaryNavigationTabVisibleAt320Dp() {
        compose.setContent {
            NocturneLTheme(FontPreset.PIXEL) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.size(width = 320.dp, height = 80.dp)) {
                        TerminalNavigation(NocturneLDestination.LIBRARY, {}, effectsEnabled = false)
                    }
                }
            }
        }

        assertPrimaryTabNodes()
    }

    @Test fun terminalIconButtonExposesSettingsSelectionAndClick() {
        var clicked = false
        compose.setContent {
            NocturneLTheme {
                TerminalIconButton(R.drawable.ic_settings, "Settings", { clicked = true }, selected = true)
            }
        }

        compose.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertIsSelected()
            .assertHasClickAction()
            .performClick()
        assert(clicked)
    }

    @Test fun scaffoldSettingsButtonNavigatesAndReflectsSelection() {
        var selected: NocturneLDestination? = null
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(width = 320.dp, height = 120.dp)) {
                    TerminalScaffold(
                        selected = NocturneLDestination.SETTINGS,
                        onSelected = { selected = it },
                        effectsEnabled = false,
                    ) {}
                }
            }
        }

        compose.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertIsSelected()
            .assertHasClickAction()
            .performClick()
        assert(selected == NocturneLDestination.SETTINGS)
    }

    @Test fun scaffoldSettingsButtonIsNotSelectedOnPrimaryDestination() {
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(width = 320.dp, height = 120.dp)) {
                    TerminalScaffold(NocturneLDestination.LIBRARY, {}, effectsEnabled = false) {}
                }
            }
        }

        compose.onNodeWithContentDescription("Settings").assertIsNotSelected()
        compose.onNodeWithText("[LIB]").assertIsDisplayed()
    }

    private fun assertPrimaryTabsVisible(fontPreset: FontPreset) {
        compose.setContent {
            NocturneLTheme(fontPreset) {
                Box(Modifier.size(width = 320.dp, height = 80.dp)) {
                    TerminalNavigation(NocturneLDestination.LIBRARY, {}, effectsEnabled = false)
                }
            }
        }

        assertPrimaryTabNodes()
    }

    private fun assertPrimaryTabNodes() {
        NocturneLDestination.entries.filterNot { it == NocturneLDestination.SETTINGS }.forEach { destination ->
            compose.onNodeWithText("[${destination.label}]").assertIsDisplayed().assertHasClickAction()
        }
        compose.onNodeWithText("[ SET ]").assertDoesNotExist()
    }
}
