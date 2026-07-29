package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.NoticeSeverity
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalTextField
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun AlbumPlaylistPicker(
    playlists: List<PlaylistEntity>,
    state: AlbumPlaylistUiState,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val working = state is AlbumPlaylistUiState.Working
    var name by rememberSaveable { mutableStateOf("") }

    AsciiFrame("ADD ALBUM TO PLAYLIST", modifier) {
        if (playlists.isEmpty()) {
            TerminalTextField(
                value = name,
                onValueChange = { name = it },
                label = "NEW PLAYLIST",
                modifier = Modifier
                    .testTag("new-playlist-name")
                    .semantics(mergeDescendants = true) {},
            )
            BracketButton(
                label = "CREATE + ADD",
                onClick = { onCreateAndAdd(name.trim()) },
                modifier = Modifier.fillMaxWidth().testTag("create-and-add-playlist"),
                enabled = name.trim().isNotEmpty() && !working,
            )
        } else {
            Column {
                playlists.forEach { playlist ->
                    BracketButton(
                        label = playlist.name,
                        onClick = { onPlaylistSelected(playlist) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !working,
                    )
                }
            }
        }

        AlbumPlaylistFeedback(state, Modifier.padding(top = TerminalDimensions.xs))
    }
}

@Composable
fun AlbumPlaylistFeedback(state: AlbumPlaylistUiState, modifier: Modifier = Modifier) {
    when (state) {
        AlbumPlaylistUiState.Idle -> Unit
        AlbumPlaylistUiState.Working -> TerminalNotice("ADDING ALBUM…", modifier)
        is AlbumPlaylistUiState.Success -> TerminalNotice(state.message, modifier)
        is AlbumPlaylistUiState.AlreadyPresent -> TerminalNotice(state.message, modifier, NoticeSeverity.WARNING)
        is AlbumPlaylistUiState.Warning -> TerminalNotice(state.message, modifier, NoticeSeverity.WARNING)
        is AlbumPlaylistUiState.Error -> TerminalNotice(state.message, modifier, NoticeSeverity.ERROR)
    }
}
