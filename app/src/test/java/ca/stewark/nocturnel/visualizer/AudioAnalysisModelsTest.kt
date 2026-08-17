package ca.stewark.nocturnel.visualizer

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioAnalysisModelsTest {
    @Test fun idleAndUnavailableFramesHaveStableShapes() {
        assertEquals(AnalysisStatus.IDLE, AudioAnalysisFrame.Idle.status)
        assertEquals(128, AudioAnalysisFrame.Idle.waveform.size)
        assertEquals(32, AudioAnalysisFrame.Idle.bands.size)
        assertEquals(AnalysisStatus.UNAVAILABLE, AudioAnalysisFrame.Unavailable.status)
    }
}
