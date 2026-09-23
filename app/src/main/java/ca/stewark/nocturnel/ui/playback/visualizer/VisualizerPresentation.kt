package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

@Composable
internal fun VisualizerPresentation(
    session: VisualizerSessionState,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    nowVisible: Boolean,
    onSessionChange: (VisualizerSessionState) -> Unit,
    onVisualizerActiveChanged: (Boolean) -> Unit,
    normalContent: @Composable () -> Unit,
) {
    val latestActiveCallback = rememberUpdatedState(onVisualizerActiveChanged)
    val analysisNeeded = session.analysisNeeded(nowVisible)
    LaunchedEffect(analysisNeeded) { latestActiveCallback.value(analysisNeeded) }
    DisposableEffect(Unit) { onDispose { latestActiveCallback.value(false) } }

    if (session.expanded) {
        ImmersiveSystemBars()
        BackHandler { onSessionChange(session.close()) }
        FullScreenVisualizer(
            mode = session.mode,
            frame = frame,
            effectsEnabled = effectsEnabled,
            onExit = { onSessionChange(session.close()) },
            onSwipeLeft = { onSessionChange(session.swipeLeft()) },
            onSwipeRight = { onSessionChange(session.swipeRight()) },
        )
    } else {
        normalContent()
    }
}
