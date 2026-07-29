package ca.stewark.nocturnel.ui.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.TerminalTextField
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun PlaylistDetailScreen(
    state: PlaylistDetailState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var name by remember(state.playlist.id, state.playlist.name) { mutableStateOf(state.playlist.name) }
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        AsciiFrame(state.playlist.name) {
            TerminalTextField(name, { name = it }, "PLAYLIST NAME")
            Row {
                BracketButton("BACK", onBack)
                BracketButton("PLAY", onPlay, enabled = state.entries.any { it.available })
                BracketButton("RENAME", { onRename(name) }, enabled = name.isNotBlank() && name != state.playlist.name)
                BracketButton(if (adding) "CLOSE ADD" else "ADD TRACK", { adding = !adding })
            }
        }
        LazyColumn {
            if (adding) {
                item { TerminalTextField(query, { query = it }, "FILTER AVAILABLE TRACKS", Modifier.padding(vertical = TerminalDimensions.xs)) }
                items(
                    state.availableTracks.filter { "${it.title} ${it.artist} ${it.album}".contains(query, true) },
                    key = { "available:${it.relativePath}" },
                ) { track ->
                    Row(Modifier.fillMaxWidth()) {
                        BracketIconButton("+", "Add ${track.title}", { onAdd(track.relativePath) })
                        Text("${track.artist} :: ${track.title}", Modifier.weight(1f).padding(top = TerminalDimensions.sm))
                    }
                }
            }
            items(state.entries, key = { "${it.position}:${it.relativePath}" }) { row ->
                Row(Modifier.fillMaxWidth()) {
                    BracketIconButton("↑", "Move ${row.title} up", { onMove(row.position, row.position - 1) }, enabled = row.canMoveUp)
                    BracketIconButton("↓", "Move ${row.title} down", { onMove(row.position, row.position + 1) }, enabled = row.canMoveDown)
                    Column(Modifier.weight(1f).padding(vertical = TerminalDimensions.xs)) {
                        Text(row.title)
                        Text("${row.artist} · ${formatDuration(row.durationMs)}")
                    }
                    BracketIconButton("X", "Remove ${row.title}", { onRemove(row.position) })
                }
            }
        }
    }
}
