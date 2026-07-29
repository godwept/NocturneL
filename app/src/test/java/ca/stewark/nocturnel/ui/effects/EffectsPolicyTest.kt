package ca.stewark.nocturnel.ui.effects

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectsPolicyTest {
    @Test fun `effects default to enabled`() {
        assertTrue(EffectsPolicy().effectiveEffectsEnabled)
    }

    @Test fun `saved disabled value wins`() {
        assertFalse(EffectsPolicy(savedEffectsEnabled = false).effectiveEffectsEnabled)
    }

    @Test fun `reduced motion suppresses effects without changing saved value`() {
        val policy = EffectsPolicy(savedEffectsEnabled = true, systemAnimationsEnabled = false)
        assertTrue(policy.savedEffectsEnabled)
        assertFalse(policy.effectiveEffectsEnabled)
    }
}
