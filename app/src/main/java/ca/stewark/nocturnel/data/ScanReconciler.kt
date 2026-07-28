package ca.stewark.nocturnel.data

data class TrackFingerprint(val relativePath: String, val contentSignature: String)
data class ReconciliationCounts(val added: Int, val changed: Int, val missing: Int)

object ScanReconciler {
    fun missingPaths(previousPaths: Set<String>, seenPaths: Set<String>): Set<String> = previousPaths - seenPaths

    fun count(previous: List<TrackFingerprint>, current: List<TrackFingerprint>): ReconciliationCounts {
        val previousByPath = previous.associateBy(TrackFingerprint::relativePath)
        val currentByPath = current.associateBy(TrackFingerprint::relativePath)
        return ReconciliationCounts(
            added = currentByPath.keys.count { it !in previousByPath },
            changed = currentByPath.count { (path, track) ->
                previousByPath[path]?.contentSignature?.let { it != track.contentSignature } == true
            },
            missing = previousByPath.keys.count { it !in currentByPath },
        )
    }
}
