package ca.stewark.nocturnel.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class ReadMetadata(
    val title: String?, val artist: String?, val album: String?, val year: String?,
    val trackNumber: Int?, val discNumber: Int?, val durationMs: Long, val embeddedArtwork: ByteArray?,
)

class AndroidMediaMetadataReader(private val context: Context) {
    fun read(uri: Uri): Result<ReadMetadata> = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            fun tag(key: Int) = retriever.extractMetadata(key)
            ReadMetadata(
                title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = tag(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                year = tag(MediaMetadataRetriever.METADATA_KEY_YEAR),
                trackNumber = tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull(),
                discNumber = tag(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.toIntOrNull(),
                durationMs = tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                embeddedArtwork = retriever.embeddedPicture,
            )
        }
    }
}
