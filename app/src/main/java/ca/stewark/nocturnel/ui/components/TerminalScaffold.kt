package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import ca.stewark.nocturnel.R
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun TerminalScaffold(
    selected: NocturneLDestination,
    onSelected: (NocturneLDestination) -> Unit,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
    status: String? = null,
    notice: AppNotice? = null,
    content: @Composable () -> Unit,
) {
    val settingsSelected = selected == NocturneLDestination.SETTINGS
    val activeNavigationPulse = rememberActiveNavigationPulse(effectsEnabled)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = TerminalDimensions.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "NOCTURNEL",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                TerminalIconButton(
                    iconRes = R.drawable.ic_settings,
                    contentDescription = "Settings",
                    onClick = { onSelected(NocturneLDestination.SETTINGS) },
                    selected = settingsSelected,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (settingsSelected) activeNavigationPulse else 1f
                    },
                )
            }
            TerminalNavigation(selected, onSelected, effectsEnabled)
            Box(Modifier.weight(1f).fillMaxSize()) { content() }
            notice?.let { TerminalNotice(it.text, Modifier.padding(TerminalDimensions.xs), it.severity) }
                ?: status?.let { TerminalNotice(it, Modifier.padding(TerminalDimensions.xs)) }
        }
        Scanlines(effectsEnabled)
    }
}
