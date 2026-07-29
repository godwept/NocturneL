package ca.stewark.nocturnel.ui.effects

data class EffectsPolicy(
    val savedEffectsEnabled: Boolean = true,
    val systemAnimationsEnabled: Boolean = true,
) {
    val effectiveEffectsEnabled: Boolean
        get() = savedEffectsEnabled && systemAnimationsEnabled
}
