package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import kotlinx.coroutines.delay

@Composable
internal fun VisualizerDeck(
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    onVisualizerActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    syncOffsetMs: Int = VisualizerSyncOffset.DEFAULT_MS,
    onDecreaseSyncOffset: () -> Unit = {},
    onIncreaseSyncOffset: () -> Unit = {},
    onResetSyncOffset: () -> Unit = {},
    albumArt: @Composable () -> Unit,
) {
    var mode by remember { mutableStateOf(VisualizerDisplayMode.ART) }
    var tapCount by remember { mutableIntStateOf(0) }
    var labelVisible by remember { mutableStateOf(false) }
    val labelAlpha = remember { Animatable(0f) }
    val visualizerActive = mode != VisualizerDisplayMode.ART

    DisposableEffect(visualizerActive) {
        onVisualizerActiveChanged(visualizerActive)
        onDispose { onVisualizerActiveChanged(false) }
    }
    LaunchedEffect(tapCount) {
        if (tapCount == 0) return@LaunchedEffect
        labelVisible = true
        labelAlpha.snapTo(1f)
        delay(800)
        labelAlpha.animateTo(0f, tween(400))
        labelVisible = false
    }

    Column(modifier) {
        if (visualizerActive) {
            VisualizerSyncControls(
                syncOffsetMs = syncOffsetMs,
                onDecrease = onDecreaseSyncOffset,
                onIncrease = onIncreaseSyncOffset,
                onReset = onResetSyncOffset,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag(if (mode == VisualizerDisplayMode.ART) "visualizer-art" else "visualizer-deck")
                .semantics { stateDescription = mode.accessibilityName }
                .clickable(onClickLabel = "Show ${mode.next().accessibilityName}") {
                    mode = mode.next()
                    tapCount++
                },
        ) {
            if (mode == VisualizerDisplayMode.ART) {
                Box(Modifier.fillMaxSize()) { albumArt() }
            } else {
                TerminalVisualizerScene(mode, frame, effectsEnabled, Modifier.fillMaxSize())
            }
            if (labelVisible) {
                Text(
                    mode.label,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(TerminalDimensions.xs)
                        .alpha(labelAlpha.value),
                )
            }
        }
    }
}

@Composable
internal fun VisualizerSyncControls(
    syncOffsetMs: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetMs = VisualizerSyncOffset.clamp(syncOffsetMs)
    Row(
        modifier = modifier.testTag("visualizer-sync-controls"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BracketIconButton(
            glyph = "-",
            contentDescription = "Decrease visualizer sync offset",
            onClick = onDecrease,
            modifier = Modifier.testTag("visualizer-sync-decrease"),
            enabled = offsetMs > VisualizerSyncOffset.MIN_MS,
        )
        BracketButton(
            label = "VIS SYNC ${VisualizerSyncOffset.label(offsetMs)}",
            onClick = onReset,
            modifier = Modifier.testTag("visualizer-sync-reset"),
            contentDescription = "Reset visualizer sync offset, currently ${VisualizerSyncOffset.label(offsetMs)}",
        )
        BracketIconButton(
            glyph = "+",
            contentDescription = "Increase visualizer sync offset",
            onClick = onIncrease,
            modifier = Modifier.testTag("visualizer-sync-increase"),
            enabled = offsetMs < VisualizerSyncOffset.MAX_MS,
        )
    }
}
