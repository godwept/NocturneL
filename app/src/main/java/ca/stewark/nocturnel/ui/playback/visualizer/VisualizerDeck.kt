package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
    var modeTapCount by remember { mutableIntStateOf(0) }
    var modeLabelVisible by remember { mutableStateOf(false) }
    val modeLabelAlpha = remember { Animatable(0f) }
    var syncLabelGeneration by remember { mutableIntStateOf(0) }
    var syncLabelVisible by remember { mutableStateOf(false) }
    val syncLabelAlpha = remember { Animatable(0f) }
    val visualizerActive = mode != VisualizerDisplayMode.ART

    DisposableEffect(visualizerActive) {
        onVisualizerActiveChanged(visualizerActive)
        onDispose { onVisualizerActiveChanged(false) }
    }
    LaunchedEffect(modeTapCount) {
        if (modeTapCount == 0) return@LaunchedEffect
        modeLabelVisible = true
        modeLabelAlpha.snapTo(1f)
        delay(800)
        modeLabelAlpha.animateTo(0f, tween(400))
        modeLabelVisible = false
    }
    LaunchedEffect(mode, syncLabelGeneration) {
        if (!visualizerActive) {
            syncLabelVisible = false
            syncLabelAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        syncLabelVisible = true
        syncLabelAlpha.snapTo(1f)
        delay(SYNC_LABEL_HOLD_MS)
        syncLabelAlpha.animateTo(0f, tween(SYNC_LABEL_FADE_MS))
        syncLabelVisible = false
    }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        if (mode == VisualizerDisplayMode.ART) {
            Box(Modifier.fillMaxSize()) { albumArt() }
        } else {
            TerminalVisualizerScene(mode, frame, effectsEnabled, Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .testTag(if (mode == VisualizerDisplayMode.ART) "visualizer-art" else "visualizer-deck")
                .semantics { stateDescription = mode.accessibilityName }
                .clickable(onClickLabel = "Show ${mode.next().accessibilityName}") {
                    mode = mode.next()
                    modeTapCount++
                },
        ) {
        }
        if (visualizerActive) {
            VisualizerSyncControls(
                syncOffsetMs = syncOffsetMs,
                onDecrease = {
                    syncLabelGeneration++
                    onDecreaseSyncOffset()
                },
                onIncrease = {
                    syncLabelGeneration++
                    onIncreaseSyncOffset()
                },
                onReset = {
                    syncLabelGeneration++
                    onResetSyncOffset()
                },
                labelVisible = syncLabelVisible,
                labelAlpha = syncLabelAlpha.value,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (modeLabelVisible) {
            Text(
                mode.label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TerminalDimensions.xs)
                    .alpha(modeLabelAlpha.value)
                    .testTag("visualizer-mode-label"),
            )
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
    labelVisible: Boolean = true,
    labelAlpha: Float = 1f,
) {
    val offsetMs = VisualizerSyncOffset.clamp(syncOffsetMs)
    Box(modifier.testTag("visualizer-sync-controls")) {
        SyncCornerButton(
            glyph = "-",
            contentDescription = "Decrease visualizer sync offset",
            onClick = onDecrease,
            modifier = Modifier.align(Alignment.TopStart),
            testTag = "visualizer-sync-decrease",
            enabled = offsetMs > VisualizerSyncOffset.MIN_MS,
        )
        if (labelVisible) {
            BracketButton(
                label = "VIS SYNC ${VisualizerSyncOffset.label(offsetMs)}",
                onClick = onReset,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .alpha(labelAlpha)
                    .testTag("visualizer-sync-reset"),
                contentDescription = "Reset visualizer sync offset, currently ${VisualizerSyncOffset.label(offsetMs)}",
            )
        }
        SyncCornerButton(
            glyph = "+",
            contentDescription = "Increase visualizer sync offset",
            onClick = onIncrease,
            modifier = Modifier.align(Alignment.TopEnd),
            testTag = "visualizer-sync-increase",
            enabled = offsetMs < VisualizerSyncOffset.MAX_MS,
        )
    }
}

@Composable
private fun SyncCornerButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.then(
            if (enabled) Modifier else Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = {})
            },
        ),
    ) {
        BracketIconButton(
            glyph = glyph,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = Modifier.testTag(testTag),
            enabled = enabled,
        )
    }
}

private const val SYNC_LABEL_HOLD_MS = 2_600L
private const val SYNC_LABEL_FADE_MS = 400
