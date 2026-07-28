package ca.stewark.nocturnel.data

import ca.stewark.nocturnel.library.model.TrackStatus

data class ReconciledTrack<T>(val value: T, val status: TrackStatus)

object ScanReconciler {
    fun missingPaths(previousPaths: Set<String>, seenPaths: Set<String>): Set<String> = previousPaths - seenPaths
}
