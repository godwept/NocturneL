package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.theme.TerminalTheme
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlinx.coroutines.delay

private const val EXIT_HOLD_MS = 3_000L

@Composable
internal fun FullScreenVisualizer(
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    onExit: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TerminalTheme.palette
    var exitVisible by remember { mutableStateOf(false) }
    var exitTapGeneration by remember { mutableIntStateOf(0) }
    val latestSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val latestSwipeRight by rememberUpdatedState(onSwipeRight)
    val revealExit = { exitVisible = true; exitTapGeneration++ }

    LaunchedEffect(exitTapGeneration) {
        if (exitTapGeneration == 0) return@LaunchedEffect
        delay(EXIT_HOLD_MS)
        exitVisible = false
    }

    BoxWithConstraints(modifier.fillMaxSize().background(palette.background).testTag("full-screen-visualizer")) {
        val square = mode == VisualizerDisplayMode.RADAR || mode == VisualizerDisplayMode.GRID
        val side = minOf(maxWidth, maxHeight)
        val glowAlpha = ambientGlowAlpha(mode, frame, effectsEnabled)
        if (square && glowAlpha > 0f) {
            Canvas(Modifier.fillMaxSize().testTag("full-screen-glow")) {
                val squareSide = minOf(size.width, size.height)
                val top = (size.height - squareSide) / 2f
                val bottom = top + squareSide
                val glow = palette.visualizerPrimary.copy(alpha = glowAlpha)
                if (top > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(listOf(palette.background, glow), startY = 0f, endY = top),
                        size = Size(size.width, top),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(listOf(glow, palette.background), startY = bottom, endY = size.height),
                        topLeft = Offset(0f, bottom),
                        size = Size(size.width, size.height - bottom),
                    )
                }
            }
        }
        TerminalVisualizerScene(
            mode = mode,
            frame = frame,
            effectsEnabled = effectsEnabled,
            modifier = if (square) Modifier.align(Alignment.Center).size(side) else Modifier.fillMaxSize(),
            showBorder = false,
        )
        Box(
            Modifier
                .fillMaxSize()
                .testTag("full-screen-touch-surface")
                .semantics { onClick(label = "Show exit control") { revealExit(); true } }
                .pointerInput(Unit) { detectTapGestures(onTap = { revealExit() }) }
                .pointerInput(Unit) {
                    var horizontalTravel = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalTravel = 0f },
                        onHorizontalDrag = { change, amount ->
                            horizontalTravel += amount
                            change.consume()
                        },
                        onDragEnd = {
                            val threshold = 56.dp.toPx()
                            when {
                                horizontalTravel <= -threshold -> latestSwipeLeft()
                                horizontalTravel >= threshold -> latestSwipeRight()
                            }
                        },
                    )
                },
        )
        if (exitVisible) {
            BracketButton(
                label = "EXIT",
                onClick = onExit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .testTag("full-screen-exit"),
                contentDescription = "Exit full screen visualization",
            )
        }
    }
}
