package ca.stewark.nocturnel.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.theme.AlertAmber
import ca.stewark.nocturnel.ui.theme.TerminalError

enum class NoticeSeverity { INFO, WARNING, ERROR }

@Composable
fun TerminalNotice(text: String, modifier: Modifier = Modifier, severity: NoticeSeverity = NoticeSeverity.INFO) {
    val color = when (severity) {
        NoticeSeverity.INFO -> MaterialTheme.colorScheme.primary
        NoticeSeverity.WARNING -> AlertAmber
        NoticeSeverity.ERROR -> TerminalError
    }
    Text(":: $text", modifier = modifier, color = color)
}
