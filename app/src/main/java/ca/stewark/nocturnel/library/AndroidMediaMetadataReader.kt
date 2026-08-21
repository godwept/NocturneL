package ca.stewark.nocturnel.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.Closeable

internal interface MetadataSession : Closeable {
    fun metadata(key: Int): String?
    fun artwork(): ByteArray?
}

internal fun interface MetadataSessionFactory {
    fun open(documentUri: String): MetadataSession
}

class AndroidMediaMetadataReader internal constructor(
    private val sessions: MetadataSessionFactory,
) : MediaMetadataReader {
    constructor(context: Context) : this(MetadataSessionFactory { documentUri ->
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(documentUri))
        } catch (error: Exception) {
            retriever.release()
            throw error
        }
        object : MetadataSession {
            override fun metadata(key: Int): String? = retriever.extractMetadata(key)
            override fun artwork(): ByteArray? = retriever.embeddedPicture
            override fun close() = retriever.release()
        }
    })

    override fun readTags(documentUri: String): Result<ReadMetadata> = runCatching {
        sessions.open(documentUri).use { session ->
            ReadMetadata(
                title = session.metadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = session.metadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = session.metadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                year = session.metadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
                trackNumber = session.metadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull(),
                discNumber = session.metadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.toIntOrNull(),
                durationMs = session.metadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
            )
        }
    }

    override fun readArtwork(documentUri: String): Result<ByteArray?> = runCatching {
        sessions.open(documentUri).use { it.artwork() }
    }
}
