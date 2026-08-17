package ca.stewark.nocturnel.playlist

import java.util.Locale

fun uniqueImportedPlaylistName(requestedName: String, usedNames: MutableSet<String>): String {
    val base = requestedName.trim().ifBlank { "Imported playlist" }
    val normalized = usedNames.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    var candidate = base
    var suffix = 2
    while (candidate.lowercase(Locale.ROOT) in normalized) candidate = "$base (${suffix++})"
    usedNames += candidate
    return candidate
}
