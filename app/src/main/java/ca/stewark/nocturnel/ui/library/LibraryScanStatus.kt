package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.library.ScanProgress
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun LibraryScanStatus(progress: ScanProgress, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    AsciiFrame("LIBRARY SCAN", modifier.fillMaxWidth()) {
        Column {
            when (progress) {
                ScanProgress.Discovering -> Text("DISCOVERING FILES...")
                is ScanProgress.Indexing -> {
                    Text("INDEXING ${progress.completed} OF ${progress.total} FILES")
                    LinearProgressIndicator(
                        progress = { progress.completed.toFloat() / progress.total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().padding(top = TerminalDimensions.xs),
                    )
                }
            }
            BracketButton("CANCEL", onCancel, Modifier.padding(top = TerminalDimensions.sm))
        }
    }
}
