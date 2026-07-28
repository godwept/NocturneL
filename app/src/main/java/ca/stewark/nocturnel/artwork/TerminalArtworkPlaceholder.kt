package ca.stewark.nocturnel.artwork

import androidx.compose.ui.graphics.Color

object TerminalArtworkPlaceholder {
    fun accentFor(albumId: String): Color {
        val colors = listOf(Color(0xFF00E676), Color(0xFF00BFA5), Color(0xFF76FF03), Color(0xFF64DD17))
        return colors[albumId.hashCode().and(Int.MAX_VALUE) % colors.size]
    }
}
