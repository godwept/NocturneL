package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.artwork.CrtArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.FavoriteToggle
import ca.stewark.nocturnel.ui.components.NoticeSeverity
import ca.stewark.nocturnel.ui.components.TerminalMarquee
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalSeekBar
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDeck
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset

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
    onOpenQueue: () -> Unit = {},
    analysisFrame: AudioAnalysisFrame = AudioAnalysisFrame.Idle,
    onVisualizerActiveChanged: (Boolean) -> Unit = {},
    visualizerSyncOffsetMs: Int = VisualizerSyncOffset.DEFAULT_MS,
    onDecreaseVisualizerSyncOffset: () -> Unit = {},
    onIncreaseVisualizerSyncOffset: () -> Unit = {},
    onResetVisualizerSyncOffset: () -> Unit = {},
    currentTrackFavorite: Boolean = false,
    currentTrackPlayCount: Long = 0,
    onToggleCurrentFavorite: () -> Unit = {},
) {
    val albumMetadata = if (state.currentPath != null) {
        "${state.album.orEmpty()} · $currentTrackPlayCount PLAY(S)"
    } else {
        state.album.orEmpty()
    }
    val repeatGlyph = when (state.repeatMode) {
        Player.REPEAT_MODE_ALL -> "RPT:A"
        Player.REPEAT_MODE_ONE -> "RPT:1"
        else -> "RPT"
    }
    val repeatDescription = when (state.repeatMode) {
        Player.REPEAT_MODE_ALL -> "Repeat all"
        Player.REPEAT_MODE_ONE -> "Repeat one"
        else -> "Repeat off"
    }
    LazyColumn(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        item {
            AsciiFrame {
                VisualizerDeck(
                    frame = analysisFrame,
                    effectsEnabled = effectsEnabled,
                    onVisualizerActiveChanged = onVisualizerActiveChanged,
                    modifier = Modifier.fillMaxWidth(),
                    syncOffsetMs = visualizerSyncOffsetMs,
                    onDecreaseSyncOffset = onDecreaseVisualizerSyncOffset,
                    onIncreaseSyncOffset = onIncreaseVisualizerSyncOffset,
                    onResetSyncOffset = onResetVisualizerSyncOffset,
                ) {
                    if (albumArtwork != null) CrtArtwork(albumArtwork, effectsEnabled, Modifier.fillMaxSize())
                    else Text("▓▓", style = MaterialTheme.typography.displayLarge)
                }
                TerminalMarquee(state.title ?: "NO TRACK SELECTED", effectsEnabled, Modifier.padding(top = TerminalDimensions.sm))
                TerminalMarquee(state.artist.orEmpty(), effectsEnabled)
                TerminalMarquee(albumMetadata, effectsEnabled)
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        BracketIconButton("SHF", "Shuffle", onShuffle, selected = state.shuffle)
                        BracketIconButton(
                            repeatGlyph,
                            repeatDescription,
                            onRepeat,
                            selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                        )
                    }
                    if (state.currentPath != null) {
                        FavoriteToggle(state.title ?: "current track", currentTrackFavorite, onToggleCurrentFavorite)
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = TerminalDimensions.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (state.upNext.isEmpty()) "QUEUE EMPTY" else "${state.upNext.size} TRACK(S) UPCOMING",
                    color = MaterialTheme.colorScheme.secondary,
                )
                BracketButton("QUEUE", onOpenQueue)
            }
        }
    }
}
