package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun AsciiFrame(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = ca.stewark.nocturnel.ui.theme.TerminalTheme.palette
    Column(
        modifier
            .fillMaxWidth()
            .terminalBorder(palette.border)
            .padding(horizontal = TerminalDimensions.sm, vertical = TerminalDimensions.xs),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = "[ $title ]",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = TerminalDimensions.xs),
            )
        }
        content()
    }
}

@Composable
fun TerminalFrame(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    AsciiFrame(title, modifier, content)
