package ca.stewark.nocturnel.ui.playback.visualizer

enum class VisualizerDisplayMode(val label: String, val accessibilityName: String) {
    ART("ART 1/3", "Album art"),
    RADAR("RADAR 2/3", "Circular radar"),
    BANDS("BANDS 3/3", "Spectrum bars");

    fun next(): VisualizerDisplayMode = when (this) {
        ART -> RADAR
        RADAR -> BANDS
        BANDS -> ART
    }
}
