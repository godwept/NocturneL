package ca.stewark.nocturnel.library

data class FileFingerprint(
    val fileSizeBytes: Long?,
    val lastModifiedEpochMillis: Long?,
) {
    val reliable: Boolean
        get() = fileSizeBytes != null && fileSizeBytes >= 0 &&
            lastModifiedEpochMillis != null && lastModifiedEpochMillis > 0

    fun matches(other: FileFingerprint): Boolean = reliable && other.reliable && this == other
}
