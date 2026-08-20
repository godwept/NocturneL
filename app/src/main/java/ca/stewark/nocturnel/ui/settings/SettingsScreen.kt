package ca.stewark.nocturnel.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    onCancelRescan: () -> Unit = {},
    state: TerminalSettingsState,
    onEffectsChanged: (Boolean) -> Unit,
    scanRunning: Boolean = false,
    onClearListeningData: () -> Unit = {},
    listeningMessage: String? = null,
    pendingSourceName: String? = null,
    onConfirmSourceChange: () -> Unit = {},
    onCancelSourceChange: () -> Unit = {},
) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        AsciiFrame("SETTINGS") {
            Text("LOCAL LIBRARY")
            BracketButton("CHANGE MUSIC FOLDER", onChooseFolder)
            BracketButton(
                label = if (scanRunning) "SCANNING..." else "RESCAN LIBRARY",
                onClick = onRescan,
                enabled = !scanRunning,
            )
            if (scanRunning) BracketButton("CANCEL SCAN", onCancelRescan)
            TerminalToggle("CRT EFFECTS", state.savedEffectsEnabled, onEffectsChanged)
            if (!confirmingClear) {
                BracketButton("CLEAR HISTORY + COUNTS", { confirmingClear = true })
            } else {
                TerminalNotice("FAVORITES AND RESUME WILL BE PRESERVED", severity = NoticeSeverity.WARNING)
                BracketButton("CONFIRM CLEAR", { confirmingClear = false; onClearListeningData() })
                BracketButton("CANCEL", { confirmingClear = false })
            }
            if (pendingSourceName != null) {
                TerminalNotice("CHANGE TO $pendingSourceName? FAVORITES, HISTORY, COUNTS, AND RESUME WILL BE CLEARED.", severity = NoticeSeverity.WARNING)
                BracketButton("CONFIRM FOLDER CHANGE", onConfirmSourceChange)
                BracketButton("CANCEL FOLDER CHANGE", onCancelSourceChange)
            }
            listeningMessage?.let { TerminalNotice(it) }
            if (state.reducedMotion && state.savedEffectsEnabled) {
                TerminalNotice("Effects are paused by Android reduced-motion settings.", severity = NoticeSeverity.WARNING)
            }
        }
    }
}
