package ca.stewark.nocturnel.ui.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.Scanlines
import ca.stewark.nocturnel.ui.components.terminalBorder
import ca.stewark.nocturnel.ui.theme.TerminalTheme
import androidx.compose.ui.geometry.Offset

@Composable
fun CrtArtwork(
    album: AlbumEntity,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = TerminalTheme.palette
    Box(modifier.terminalBorder(palette.borderEmphasis, emphasized = true).testTag(if (effectsEnabled) "crt-artwork" else "plain-artwork")) {
        RetroArtwork(album, Modifier.matchParentSize())
        if (effectsEnabled) {
            Canvas(Modifier.matchParentSize().testTag("chromatic-overlay")) {
                drawLine(palette.accentPrimary.copy(alpha = .18f), Offset(1f, 0f), Offset(1f, size.height), 2f)
                drawLine(palette.accentSecondary.copy(alpha = .18f), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 2f)
            }
        }
        Scanlines(effectsEnabled, Modifier.matchParentSize())
    }
}
