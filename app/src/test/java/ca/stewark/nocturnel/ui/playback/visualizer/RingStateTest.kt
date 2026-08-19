package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RingStateTest {
    @Test fun smoothsOnePreviousIncreasingFrame() {
        val first = updateRingState(RingState.Empty, VisualizerDisplayMode.RING, frame(1, 0f), true)
        val second = updateRingState(first, VisualizerDisplayMode.RING, frame(2, 1f), true)
        assertEquals(1L, first.frameId)
        assertTrue(first.previousMagnitudes.isEmpty())
        assertEquals(first.magnitudes, second.previousMagnitudes)
        assertTrue(second.magnitudes.all { it in .649f..651f })
    }

    @Test fun nonIncreasingFrameResetsSmoothingAndEcho() {
        val first = updateRingState(RingState.Empty, VisualizerDisplayMode.RING, frame(4, 1f, 1f), true)
        val reset = updateRingState(first, VisualizerDisplayMode.RING, frame(3, 0f), true)
        assertEquals(3L, reset.frameId)
        assertTrue(reset.previousMagnitudes.isEmpty())
        assertTrue(reset.magnitudes.all { it == 0f })
        assertNull(reset.echo)
    }

    @Test fun clearsOutsideActiveRingButEffectsOffStillSmooths() {
        val active = updateRingState(RingState.Empty, VisualizerDisplayMode.RING, frame(1, 0f), true)
        listOf(VisualizerDisplayMode.ART, VisualizerDisplayMode.RADAR, VisualizerDisplayMode.BANDS).forEach { mode ->
            assertEquals(RingState.Empty, updateRingState(active, mode, frame(2, 1f), true))
        }
        assertEquals(RingState.Empty, updateRingState(active, VisualizerDisplayMode.RING, AudioAnalysisFrame.Idle, true))
        assertEquals(RingState.Empty, updateRingState(active, VisualizerDisplayMode.RING, AudioAnalysisFrame.Unavailable, true))
        val effectsOff = updateRingState(active, VisualizerDisplayMode.RING, frame(2, 1f, 1f), false)
        assertTrue(effectsOff.previousMagnitudes.isNotEmpty())
        assertNull(effectsOff.echo)
    }

    @Test fun echoRequiresThresholdAndExpiresAfterFourFreshFrames() {
        val below = updateRingState(RingState.Empty, VisualizerDisplayMode.RING, frame(1, 0f, .64f), true)
        assertNull(below.echo)
        var state = updateRingState(below, VisualizerDisplayMode.RING, frame(2, 0f, .65f), true)
        assertEquals(RingEchoState(0, .65f), state.echo)
        state = updateRingState(state, VisualizerDisplayMode.RING, frame(3, 0f), true)
        assertEquals(1, state.echo?.age)
        state = updateRingState(state, VisualizerDisplayMode.RING, frame(4, 0f), true)
        assertEquals(2, state.echo?.age)
        state = updateRingState(state, VisualizerDisplayMode.RING, frame(5, 0f), true)
        assertEquals(3, state.echo?.age)
        state = updateRingState(state, VisualizerDisplayMode.RING, frame(6, 0f), true)
        assertNull(state.echo)
    }

    private fun frame(id: Long, sample: Float, transient: Float = 0f) = AudioAnalysisFrame(
        waveform = List(128) { sample }, bands = List(32) { 0f }, energy = 0f,
        lowEnergy = 0f, midEnergy = 1f, highEnergy = 0f, transient = transient,
        frameId = id, status = AnalysisStatus.ACTIVE,
    )
}
