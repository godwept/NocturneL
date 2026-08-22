package ca.stewark.nocturnel.artwork

import androidx.compose.ui.graphics.Color

object TerminalArtworkPlaceholder {
    fun accentFor(albumId: String, colors: List<Color>): Color {
        require(colors.isNotEmpty()) { "Artwork placeholder palette must not be empty" }
        return colors[albumId.hashCode().and(Int.MAX_VALUE) % colors.size]
    }
}
