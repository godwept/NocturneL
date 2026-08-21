package ca.stewark.nocturnel.library

data class ReadMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
)

interface MediaMetadataReader {
    fun readTags(documentUri: String): Result<ReadMetadata>
    fun readArtwork(documentUri: String): Result<ByteArray?>
}
