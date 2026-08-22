package ca.stewark.nocturnel.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FontPresetWiringTest {
    @Test fun rootThemeAndSettingsUseOneSharedPresetState() {
        val activity = File("src/main/java/ca/stewark/nocturnel/MainActivity.kt").readText()
        val app = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()
        val previews = File("src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt").readText()

        assertTrue("val settingsViewModel: SettingsViewModel = viewModel()" in activity)
        assertTrue("val settings by settingsViewModel.state.collectAsState()" in activity)
        assertTrue("fontPreset = settings.fontPreset" in activity)
        assertTrue("colorTheme = settings.colorTheme" in activity)
        assertTrue("effectsEnabled = settings.effectiveEffectsEnabled" in activity)
        assertTrue("NocturneLApp(settingsViewModel = settingsViewModel)" in activity)
        assertTrue("onCycleFontPreset = settingsViewModel::cycleFontPreset" in app)
        assertTrue("onCycleColorTheme = settingsViewModel::cycleColorTheme" in app)
        assertTrue("fontPreset: FontPreset = FontPreset.DEFAULT" in previews)
        assertTrue("NocturneLTheme(fontPreset, colorTheme, effectsEnabled)" in previews)
    }
}
