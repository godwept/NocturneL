package ca.stewark.nocturnel.playback

object GaplessPolicy {
    /** No silence analysis: only confirmed decoder/encoder metadata may opt a transition in. */
    fun permitsTransition(hasConfirmedGaplessMetadata: Boolean, sameAlbum: Boolean): Boolean = hasConfirmedGaplessMetadata && sameAlbum
}
