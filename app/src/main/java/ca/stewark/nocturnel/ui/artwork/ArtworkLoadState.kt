package ca.stewark.nocturnel.ui.artwork

sealed interface ArtworkLoadState {
    data object Loading : ArtworkLoadState
    data object Loaded : ArtworkLoadState
    data object Fallback : ArtworkLoadState
}
