package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.BracketButton

@Composable
fun ArtistDetailScreen(artist: ArtistRow, onBack: () -> Unit, onAlbumSelected: (AlbumEntity) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BracketButton("BACK · ${artist.name}", onBack)
        AlbumGridScreen(artist.albums, onAlbumSelected = onAlbumSelected)
    }
}
