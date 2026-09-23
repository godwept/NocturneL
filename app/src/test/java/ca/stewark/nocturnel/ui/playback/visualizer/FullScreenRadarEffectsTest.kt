package ca.stewark.nocturnel.ui.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenRadarEffectsTest {
    @Test fun risingTransientCreatesOnePulse() {
        val first = updateRadarFullScreenEffects(RadarFullScreenEffectState.Empty, .6f, 0L)
        val same = updateRadarFullScreenEffects(first, .6f, 16_000_000L)
        val decayed = updateRadarFullScreenEffects(same, .4f, 16_000_000L)

        assertEquals(1, first.pulses.size)
        assertEquals(.6f, first.pulses.single().strength, 0f)
        assertEquals(1, same.pulses.size)
        assertEquals(1, decayed.pulses.size)
    }

    @Test fun pulseProgressExpandsAndFadesMonotonically() {
        val start = RadarFullScreenPulse(1f, 0L)
        val middle = RadarFullScreenPulse(1f, RADAR_FULL_SCREEN_PULSE_DURATION_NANOS / 2)
        val end = RadarFullScreenPulse(1f, RADAR_FULL_SCREEN_PULSE_DURATION_NANOS)

        assertTrue(radarFullScreenPulseProgress(start) < radarFullScreenPulseProgress(middle))
        assertTrue(radarFullScreenPulseProgress(middle) < radarFullScreenPulseProgress(end))
        assertTrue(
            radarFullScreenPulseAlpha(start, RADAR_PULSE_EDGE_MAX_ALPHA) >
                radarFullScreenPulseAlpha(middle, RADAR_PULSE_EDGE_MAX_ALPHA),
        )
        assertEquals(0f, radarFullScreenPulseAlpha(end, RADAR_PULSE_EDGE_MAX_ALPHA), 0f)
        assertEquals(
            RADAR_PULSE_EDGE_MAX_ALPHA,
            radarFullScreenPulseAlpha(RadarFullScreenPulse(4f, 0L), RADAR_PULSE_EDGE_MAX_ALPHA),
            0f,
        )
    }

    @Test fun expiredPulsesAreRemoved() {
        val state = RadarFullScreenEffectState(
            previousTransient = .5f,
            pulses = listOf(
                RadarFullScreenPulse(.5f, RADAR_FULL_SCREEN_PULSE_DURATION_NANOS - 1L),
            ),
        )

        val updated = updateRadarFullScreenEffects(state, .4f, 2L)

        assertTrue(updated.pulses.isEmpty())
    }

    @Test fun closeTransientsRemainBounded() {
        var state = RadarFullScreenEffectState.Empty
        repeat(10) {
            state = updateRadarFullScreenEffects(state, .9f, 1_000_000L)
            state = updateRadarFullScreenEffects(state, .1f, 1_000_000L)
        }

        assertEquals(RADAR_FULL_SCREEN_MAX_PULSES, state.pulses.size)
    }

    @Test fun invalidTransientDoesNotCreatePulse() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 0f)) {
            val state = updateRadarFullScreenEffects(RadarFullScreenEffectState.Empty, value, 0L)
            assertTrue(state.pulses.isEmpty())
            assertEquals(0f, state.previousTransient, 0f)
        }
    }
}
