package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.FavoriteToggle
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
internal fun ListeningTrackRow(
    title: String,
    artist: String,
    playCount: Long,
    favorite: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
            .then(if (enabled) Modifier.clickable(onClick = onPlay) else Modifier)
            .padding(vertical = TerminalDimensions.xs),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(
                listOfNotNull(artist.takeIf { it.isNotBlank() }, "$playCount PLAY(S)", supportingText).joinToString(" · "),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        FavoriteToggle(title, favorite, onToggleFavorite)
    }
}

internal fun ca.stewark.nocturnel.data.model.ListeningHistoryRow.toTrackEntityOrNull(): TrackEntity? {
    if (status != "PLAYABLE" || title == null || artist == null || album == null || albumId == null || durationMs == null || documentUri == null) return null
    return TrackEntity(relativePath, documentUri, albumId, title, artist, album, durationMs, null, null, status, 0)
}
