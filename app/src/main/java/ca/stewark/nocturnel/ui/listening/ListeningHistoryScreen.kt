package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import java.text.DateFormat
import java.util.Date

@Composable
fun ListeningHistoryScreen(
    state: ListeningUiState,
    onBack: () -> Unit,
    onTrackSelected: (TrackEntity) -> Unit,
    onFavoriteTrack: (String) -> Unit,
    formatTimestamp: (Long) -> String = { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) },
) {
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        Row { BracketButton("BACK", onBack) }
        if (state.history.isEmpty()) {
            TerminalNotice("NO LISTENING HISTORY YET", Modifier.padding(top = TerminalDimensions.md))
            return@Column
        }
        LazyColumn(Modifier.padding(top = TerminalDimensions.sm)) {
            items(state.history, key = { it.id }) { row ->
                val track = row.toTrackEntityOrNull()
                ListeningTrackRow(
                    title = row.title ?: row.relativePath.substringAfterLast('/'),
                    artist = row.artist.orEmpty(),
                    playCount = state.trackPlayCounts[row.relativePath] ?: 0,
                    favorite = row.relativePath in state.favoriteTrackPaths,
                    enabled = track != null,
                    onPlay = { track?.let(onTrackSelected) },
                    onToggleFavorite = { onFavoriteTrack(row.relativePath) },
                    supportingText = if (track == null) "MISSING · ${formatTimestamp(row.playedAtEpochMillis)}" else formatTimestamp(row.playedAtEpochMillis),
                )
            }
        }
    }
}
