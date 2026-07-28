package ca.stewark.nocturnel.library

object SupportedAudioFormats {
    private val extensions = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac")

    fun isCandidateAudioFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in extensions
}
