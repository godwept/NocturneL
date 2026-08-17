package ca.stewark.nocturnel.playlist

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import ca.stewark.nocturnel.ui.playlist.PlaylistImportPayload
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class PlaylistDocumentService(private val resolver: ContentResolver) {
    fun readImport(uri: Uri): PlaylistImportPayload {
        val input = resolver.openInputStream(uri) ?: error("Could not open playlist file")
        return input.use { raw ->
            val buffered = BufferedInputStream(raw)
            buffered.mark(ZIP_SIGNATURE_SIZE)
            val signature = ByteArray(ZIP_SIGNATURE_SIZE)
            val signatureLength = buffered.read(signature)
            buffered.reset()
            if (isZipSignature(signature, signatureLength)) {
                PlaylistImportPayload.Bundle(PlaylistBundleCodec.decode(buffered))
            } else {
                PlaylistImportPayload.Standalone(suggestedName(uri), decodeUtf8(buffered.readBytes()))
            }
        }
    }

    fun writeBundle(uri: Uri, playlists: List<PlaylistBundlePlaylist>) {
        resolver.openOutputStream(uri)?.use { PlaylistBundleCodec.encode(playlists, it) }
            ?: error("Could not create playlist file")
    }

    fun readM3u8(uri: Uri): String {
        val input = resolver.openInputStream(uri) ?: error("Could not open playlist file")
        return input.use { decodeUtf8(it.readBytes()) }
    }

    fun writeM3u8(uri: Uri, text: String) {
        resolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not create playlist file")
    }

    private fun suggestedName(uri: Uri): String {
        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return (displayName ?: uri.lastPathSegment ?: "Imported playlist")
            .substringBeforeLast('.', displayName ?: uri.lastPathSegment ?: "Imported playlist")
    }

    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun isZipSignature(bytes: ByteArray, length: Int): Boolean =
        length == ZIP_SIGNATURE_SIZE && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            ((bytes[2] == 3.toByte() && bytes[3] == 4.toByte()) ||
                (bytes[2] == 5.toByte() && bytes[3] == 6.toByte()) ||
                (bytes[2] == 7.toByte() && bytes[3] == 8.toByte()))

    private companion object {
        const val ZIP_SIGNATURE_SIZE = 4
    }
}
