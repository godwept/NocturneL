package ca.stewark.nocturnel.playback

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

data class PlaybackSnapshot(
    val paths: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeat: RepeatMode,
    val wasPlaying: Boolean,
)

data class PlaybackRestorePlan(
    val paths: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
)

object PlaybackRestorePlanner {
    fun plan(snapshot: PlaybackSnapshot, playablePaths: Set<String>): PlaybackRestorePlan? {
        val available = snapshot.paths.withIndex().filter { it.value in playablePaths }
        if (available.isEmpty()) return null
        val restoredIndex = available.indexOfFirst { it.index == snapshot.currentIndex }.takeIf { it >= 0 } ?: 0
        val restoredPosition = if (available[restoredIndex].index == snapshot.currentIndex) snapshot.positionMs else 0
        return PlaybackRestorePlan(available.map { it.value }, restoredIndex, restoredPosition)
    }
}

object PlaybackStateCodec {
    private const val VERSION = 1

    fun encode(snapshot: PlaybackSnapshot): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeInt(snapshot.paths.size)
            snapshot.paths.forEach(output::writeUTF)
            output.writeInt(snapshot.currentIndex)
            output.writeLong(snapshot.positionMs)
            output.writeBoolean(snapshot.shuffle)
            output.writeInt(snapshot.repeat.ordinal)
            output.writeBoolean(snapshot.wasPlaying)
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(encoded: String?): PlaybackSnapshot? = runCatching {
        if (encoded.isNullOrBlank()) return null
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use { input ->
            require(input.readInt() == VERSION)
            val count = input.readInt()
            require(count in 0..100_000)
            val paths = List(count) { input.readUTF() }
            val currentIndex = input.readInt()
            val positionMs = input.readLong()
            val shuffle = input.readBoolean()
            val repeat = RepeatMode.entries[input.readInt()]
            val wasPlaying = input.readBoolean()
            require(currentIndex in -1 until count)
            require(positionMs >= 0)
            PlaybackSnapshot(paths, currentIndex, positionMs, shuffle, repeat, wasPlaying)
        }
    }.getOrNull()
}

interface PlaybackStateRepository {
    fun load(): PlaybackSnapshot?
    fun save(snapshot: PlaybackSnapshot)
    fun clear()
}

class SharedPreferencesPlaybackStateRepository(context: Context) : PlaybackStateRepository {
    private val preferences = context.getSharedPreferences("playback-state", Context.MODE_PRIVATE)

    override fun load(): PlaybackSnapshot? = PlaybackStateCodec.decode(preferences.getString(KEY, null))

    override fun save(snapshot: PlaybackSnapshot) {
        preferences.edit().putString(KEY, PlaybackStateCodec.encode(snapshot)).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "snapshot"
    }
}
