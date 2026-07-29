package ca.stewark.nocturnel.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.NoticeSeverity
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalToggle
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun SettingsScreen(
    onChooseFolder: () -> Unit,
    onRescan: () -> Unit,
    state: TerminalSettingsState,
    onEffectsChanged: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        AsciiFrame("SETTINGS") {
            Text("LOCAL LIBRARY")
            BracketButton("CHANGE MUSIC FOLDER", onChooseFolder)
            BracketButton("RESCAN LIBRARY", onRescan)
            TerminalToggle("CRT EFFECTS", state.savedEffectsEnabled, onEffectsChanged)
            if (state.reducedMotion && state.savedEffectsEnabled) {
                TerminalNotice("Effects are paused by Android reduced-motion settings.", severity = NoticeSeverity.WARNING)
            }
        }
    }
}
