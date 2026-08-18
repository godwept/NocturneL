package ca.stewark.nocturnel.playback

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

data class PlaybackOccurrenceSnapshot(
    val relativePath: String,
    val occurrenceId: String,
    val accumulatedListeningMs: Long = 0,
    val qualified: Boolean = false,
)

data class PlaybackSnapshot(
    val paths: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeat: RepeatMode,
    val wasPlaying: Boolean,
    val occurrences: List<PlaybackOccurrenceSnapshot> = paths.mapIndexed { index, path ->
        PlaybackOccurrenceSnapshot(path, "legacy:$index:$path")
    },
    val completed: Boolean = false,
    val playbackSessionId: String? = null,
)

data class PlaybackRestorePlan(
    val paths: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val occurrences: List<PlaybackOccurrenceSnapshot> = paths.mapIndexed { index, path ->
        PlaybackOccurrenceSnapshot(path, "legacy:$index:$path")
    },
)

object PlaybackRestorePlanner {
    fun plan(snapshot: PlaybackSnapshot, playablePaths: Set<String>): PlaybackRestorePlan? {
        if (snapshot.completed) return null
        val sourceOccurrences = snapshot.occurrences.takeIf { it.size == snapshot.paths.size }
            ?: snapshot.paths.mapIndexed { index, path -> PlaybackOccurrenceSnapshot(path, "legacy:$index:$path") }
        val available = sourceOccurrences.withIndex().filter { it.value.relativePath in playablePaths }
        if (available.isEmpty()) return null
        val restoredIndex = available.indexOfFirst { it.index == snapshot.currentIndex }.takeIf { it >= 0 } ?: 0
        val restoredPosition = if (available[restoredIndex].index == snapshot.currentIndex) snapshot.positionMs else 0
        var occurrences = available.map { indexed ->
            if (restoredPosition == 0L && indexed == available[restoredIndex] && indexed.index != snapshot.currentIndex) {
                indexed.value.copy(accumulatedListeningMs = 0, qualified = false)
            } else indexed.value
        }
        if (occurrences.all { it.occurrenceId.startsWith("legacy:") }) {
            occurrences = occurrences.mapIndexed { index, occurrence ->
                occurrence.copy(occurrenceId = "legacy:$index:${occurrence.relativePath}")
            }
        }
        return PlaybackRestorePlan(occurrences.map { it.relativePath }, restoredIndex, restoredPosition, occurrences)
    }
}

object PlaybackStateCodec {
    private const val VERSION = 2

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
            output.writeBoolean(snapshot.completed)
            output.writeBoolean(snapshot.playbackSessionId != null)
            snapshot.playbackSessionId?.let(output::writeUTF)
            output.writeInt(snapshot.occurrences.size)
            snapshot.occurrences.forEach {
                output.writeUTF(it.relativePath)
                output.writeUTF(it.occurrenceId)
                output.writeLong(it.accumulatedListeningMs)
                output.writeBoolean(it.qualified)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(encoded: String?): PlaybackSnapshot? = runCatching {
        if (encoded.isNullOrBlank()) return null
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use { input ->
            val version = input.readInt()
            require(version in 1..VERSION)
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
            if (version == 1) {
                PlaybackSnapshot(paths, currentIndex, positionMs, shuffle, repeat, wasPlaying)
            } else {
                val completed = input.readBoolean()
                val sessionId = if (input.readBoolean()) input.readUTF() else null
                val occurrenceCount = input.readInt()
                require(occurrenceCount == count)
                val occurrences = List(occurrenceCount) {
                    PlaybackOccurrenceSnapshot(
                        relativePath = input.readUTF(),
                        occurrenceId = input.readUTF(),
                        accumulatedListeningMs = input.readLong().also { require(it >= 0) },
                        qualified = input.readBoolean(),
                    )
                }
                require(occurrences.map { it.relativePath } == paths)
                PlaybackSnapshot(paths, currentIndex, positionMs, shuffle, repeat, wasPlaying, occurrences, completed, sessionId)
            }
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
    private var cachedEncoded: String? = null
    private var cachedSnapshot: PlaybackSnapshot? = null

    override fun load(): PlaybackSnapshot? {
        val encoded = preferences.getString(KEY, null)
        if (encoded == cachedEncoded) return cachedSnapshot
        return PlaybackStateCodec.decode(encoded).also {
            cachedEncoded = encoded
            cachedSnapshot = it
        }
    }

    override fun save(snapshot: PlaybackSnapshot) {
        val encoded = PlaybackStateCodec.encode(snapshot)
        cachedEncoded = encoded
        cachedSnapshot = snapshot
        preferences.edit().putString(KEY, encoded).apply()
    }

    override fun clear() {
        cachedEncoded = null
        cachedSnapshot = null
        preferences.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "snapshot"
    }
}
