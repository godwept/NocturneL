package ca.stewark.nocturnel.library

object AlbumScanPolicy {
    fun isClean(
        currentPaths: Set<String>,
        previousPaths: Set<String>,
        reusablePaths: Set<String>,
    ): Boolean = currentPaths == previousPaths && currentPaths.all(reusablePaths::contains)
}
