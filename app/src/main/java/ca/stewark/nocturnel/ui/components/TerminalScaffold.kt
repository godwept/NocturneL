package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun TerminalScaffold(
    selected: NocturneLDestination,
    onSelected: (NocturneLDestination) -> Unit,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
    status: AppNotice? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "NOCTURNEL",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = TerminalDimensions.md, vertical = TerminalDimensions.xs),
            )
            TerminalNavigation(selected, onSelected, effectsEnabled)
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
            status?.let { TerminalNotice(it.text, Modifier.padding(TerminalDimensions.xs), it.severity) }
        }
        Scanlines(effectsEnabled)
    }
}
