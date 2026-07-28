package ca.stewark.nocturnel.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import ca.stewark.nocturnel.artwork.TerminalArtworkPlaceholder
import ca.stewark.nocturnel.data.entity.AlbumEntity
import coil.compose.AsyncImage

@Composable
fun AlbumArtwork(album: AlbumEntity, modifier: Modifier = Modifier) {
    val bitmap = remember(album.embeddedArtwork) { album.embeddedArtwork?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
    when {
        album.manualArtworkUri != null -> AsyncImage(model = album.manualArtworkUri, contentDescription = "Cover art for ${album.title}", modifier = modifier)
        bitmap != null -> Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Cover art for ${album.title}", modifier = modifier)
        album.folderArtworkUri != null -> AsyncImage(model = album.folderArtworkUri, contentDescription = "Cover art for ${album.title}", modifier = modifier)
        else -> Box(modifier, contentAlignment = Alignment.Center) { Text("▓▓", color = TerminalArtworkPlaceholder.accentFor(album.id), style = MaterialTheme.typography.displayLarge) }
    }
}
