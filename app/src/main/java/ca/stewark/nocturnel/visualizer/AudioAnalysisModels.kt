package ca.stewark.nocturnel.visualizer

enum class AnalysisStatus { IDLE, ACTIVE, UNAVAILABLE }

data class AudioAnalysisFrame(
    val waveform: List<Float>,
    val bands: List<Float>,
    val energy: Float,
    val lowEnergy: Float,
    val midEnergy: Float,
    val highEnergy: Float,
    val transient: Float,
    val frameId: Long,
    val status: AnalysisStatus,
) {
    companion object {
        private val zeroWaveform = List(128) { 0f }
        private val zeroBands = List(32) { 0f }

        val Idle = AudioAnalysisFrame(zeroWaveform, zeroBands, 0f, 0f, 0f, 0f, 0f, 0, AnalysisStatus.IDLE)
        val Unavailable = Idle.copy(status = AnalysisStatus.UNAVAILABLE)
    }
}
