package ca.stewark.nocturnel.ui.playback.visualizer

enum class VisualizerDisplayMode(val label: String, val accessibilityName: String) {
    ART("ART 1/4", "Album art"),
    RADAR("RADAR 2/4", "Circular radar"),
    BANDS("BANDS 3/4", "Spectrum bars"),
    GRID("GRID 4/4", "Frequency grid");

    fun next(): VisualizerDisplayMode = when (this) {
        ART -> RADAR
        RADAR -> BANDS
        BANDS -> GRID
        GRID -> ART
    }
}
