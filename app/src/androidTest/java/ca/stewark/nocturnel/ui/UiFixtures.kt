package ca.stewark.nocturnel.ui

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity

val sampleAlbum = AlbumEntity(
    id = "album-1",
    relativeFolder = "artist/album",
    title = "Night Signals",
    artist = "Terminal Echo",
    year = "2026",
    manualArtworkUri = null,
    folderArtworkUri = null,
    embeddedArtwork = null,
)

val sampleTracks = listOf(
    TrackEntity("artist/album/01.flac", "content://track/1", sampleAlbum.id, "Carrier", sampleAlbum.artist, sampleAlbum.title, 183_000, 1, 1, "PLAYABLE", 1),
    TrackEntity("artist/album/02.flac", "content://track/2", sampleAlbum.id, "Afterimage", sampleAlbum.artist, sampleAlbum.title, 241_000, 2, 1, "PLAYABLE", 1),
)

val samplePlaylist = PlaylistEntity(id = 1, name = "Night Run", updatedEpochMillis = 1)
