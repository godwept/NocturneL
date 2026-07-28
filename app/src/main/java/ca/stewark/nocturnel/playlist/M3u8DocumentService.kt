package ca.stewark.nocturnel.playlist

import android.content.ContentResolver
import android.net.Uri

class M3u8DocumentService(private val resolver: ContentResolver) {
    fun read(uri: Uri): String = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("Could not open playlist file")

    fun write(uri: Uri, text: String) {
        resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
            ?: error("Could not create playlist file")
    }
}
