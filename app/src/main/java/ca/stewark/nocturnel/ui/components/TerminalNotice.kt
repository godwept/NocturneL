package ca.stewark.nocturnel.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.theme.TerminalTheme

enum class NoticeSeverity { INFO, WARNING, ERROR }

@Composable
fun TerminalNotice(text: String, modifier: Modifier = Modifier, severity: NoticeSeverity = NoticeSeverity.INFO) {
    val palette = TerminalTheme.palette
    val color = when (severity) {
        NoticeSeverity.INFO -> palette.textPrimary
        NoticeSeverity.WARNING -> palette.warning
        NoticeSeverity.ERROR -> palette.error
    }
    Text(":: $text", modifier = modifier, color = color)
}
