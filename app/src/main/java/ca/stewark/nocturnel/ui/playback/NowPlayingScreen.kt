package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.artwork.CrtArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.NoticeSeverity
import ca.stewark.nocturnel.ui.components.TerminalMarquee
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalSeekBar
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    albumArtwork: AlbumEntity?,
    effectsEnabled: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        item {
            AsciiFrame("NOW PLAYING") {
                if (albumArtwork != null) CrtArtwork(albumArtwork, effectsEnabled, Modifier.fillMaxWidth().aspectRatio(1f))
                else Text("▓▓", style = MaterialTheme.typography.displayLarge)
                TerminalMarquee(state.title ?: "NO TRACK SELECTED", effectsEnabled, Modifier.padding(top = TerminalDimensions.sm))
                TerminalMarquee(state.artist.orEmpty(), effectsEnabled)
                TerminalMarquee(state.album.orEmpty(), effectsEnabled)
                state.error?.let { TerminalNotice(it, severity = NoticeSeverity.WARNING) }
                TerminalSeekBar(
                    if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                    { onSeek((state.durationMs * it).toLong()) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(state.positionMs))
                    Text(formatDuration(state.durationMs))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BracketIconButton("|<", "Previous", onPrevious)
                    BracketIconButton(if (state.playing) "II" else ">", if (state.playing) "Pause" else "Play", onToggle)
                    BracketIconButton(">|", "Next", onNext)
                }
                Row {
                    BracketIconButton("SHF", "Shuffle", onShuffle, selected = state.shuffle)
                    BracketIconButton("RPT", "Repeat", onRepeat, selected = state.repeatMode != 0)
                }
            }
        }
        if (state.upNext.isNotEmpty()) {
            item { Text("+--[ UP NEXT ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.md)) }
            items(state.upNext, key = { it.relativePath }) { item ->
                Column(Modifier.padding(vertical = TerminalDimensions.xs)) {
                    Text(item.title)
                    Text(item.artist, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
