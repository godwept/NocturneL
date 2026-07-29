package ca.stewark.nocturnel.ui.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import ca.stewark.nocturnel.artwork.RetroArtworkTransformation
import ca.stewark.nocturnel.artwork.RetroArtworkCacheKey
import ca.stewark.nocturnel.artwork.TerminalArtworkPlaceholder
import ca.stewark.nocturnel.data.entity.AlbumEntity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

@Composable
fun RetroArtwork(album: AlbumEntity, modifier: Modifier = Modifier) {
    val candidates: List<Any> = remember(
        album.manualArtworkUri,
        album.embeddedArtwork,
        album.folderArtworkUri,
    ) {
        buildList {
            album.manualArtworkUri?.takeIf(String::isNotBlank)?.let(::add)
            album.embeddedArtwork?.let(::add)
            album.folderArtworkUri?.takeIf(String::isNotBlank)?.let(::add)
        }
    }
    var candidateIndex by remember(candidates) { mutableIntStateOf(0) }
    val candidate = candidates.getOrNull(candidateIndex)
    Box(
        modifier
            .background(TerminalArtworkPlaceholder.accentFor(album.id).copy(alpha = .12f))
            .testTag("retro-artwork"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "▓▓",
            color = TerminalArtworkPlaceholder.accentFor(album.id),
            style = MaterialTheme.typography.displayLarge,
        )
        if (candidate != null) {
            val sourceToken = if (candidate is ByteArray) "embedded:${candidate.contentHashCode()}" else candidate.toString()
            val identity = "${album.id}:${candidateIndex}:$sourceToken"
            val cacheKey = RetroArtworkCacheKey(identity).toString()
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(candidate)
                    .crossfade(false)
                    .transformations(RetroArtworkTransformation(identity))
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .build(),
                contentDescription = "Cover art for ${album.title}",
                contentScale = ContentScale.Crop,
                onError = { candidateIndex++ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
