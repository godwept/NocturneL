package ca.stewark.nocturnel.ui.playback.visualizer

internal data class VisualizerSessionState(
    val mode: VisualizerDisplayMode = VisualizerDisplayMode.ART,
    val expanded: Boolean = false,
) {
    fun open(): VisualizerSessionState = copy(
        mode = if (mode == VisualizerDisplayMode.ART) VisualizerDisplayMode.RADAR else mode,
        expanded = true,
    )

    fun close(): VisualizerSessionState = copy(expanded = false)

    fun swipeLeft(): VisualizerSessionState = if (!expanded) this else copy(mode = when (mode) {
        VisualizerDisplayMode.RADAR -> VisualizerDisplayMode.BANDS
        VisualizerDisplayMode.BANDS -> VisualizerDisplayMode.GRID
        VisualizerDisplayMode.GRID -> VisualizerDisplayMode.RADAR
        VisualizerDisplayMode.ART -> VisualizerDisplayMode.RADAR
    })

    fun swipeRight(): VisualizerSessionState = if (!expanded) this else copy(mode = when (mode) {
        VisualizerDisplayMode.RADAR -> VisualizerDisplayMode.GRID
        VisualizerDisplayMode.BANDS -> VisualizerDisplayMode.RADAR
        VisualizerDisplayMode.GRID -> VisualizerDisplayMode.BANDS
        VisualizerDisplayMode.ART -> VisualizerDisplayMode.RADAR
    })

    fun analysisNeeded(nowVisible: Boolean): Boolean =
        mode != VisualizerDisplayMode.ART && (expanded || nowVisible)
}
