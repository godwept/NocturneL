# Album-to-Playlist and App Icon Design

**Date:** 2026-07-29  
**Status:** Approved

## Goal

Let listeners add a complete album to one playlist directly from album detail without leaving the screen, while giving NocturneL a distinctive launcher icon that matches its terminal presentation.

## Success Criteria

- [ ] Album detail exposes an `[ ADD TO PLAYLIST ]` action for albums with playable tracks.
- [ ] The action expands a terminal-styled inline single-playlist picker.
- [ ] Adding an album appends only tracks that are not already in the chosen playlist.
- [ ] When no playlists exist, the picker can create one and immediately add the album.
- [ ] The user receives clear terminal-style success, no-op, and error feedback.
- [ ] Android uses an adaptive launcher icon featuring a retro CRT and glowing pixel-art `N`.

## Scope

**In scope:**

- Add album tracks in their displayed order.
- Preserve the existing playlist order.
- Skip paths that already exist in the selected playlist.
- Show added and skipped counts.
- Inline playlist selection and empty-state creation.
- Disable the album action when there are no playable tracks.
- Add adaptive launcher assets and legacy square fallback assets.
- Update behavior and screenshot coverage.

**Out of scope:**

- Selecting multiple playlists in one action.
- Removing an album from a playlist as a group.
- Showing playlist creation when playlists already exist.
- Automatically navigating away from album detail after adding.
- Room schema changes.
- Playlist import/export format changes.

## Design

### Album Detail Interaction

Album detail adds `[ ADD TO PLAYLIST ]` alongside its existing album commands. Selecting it expands an `AsciiFrame` within the album screen rather than opening a dialog or navigating away.

When playlists exist, the frame lists each playlist as one 48 dp terminal row. Selecting a row starts the add operation. On success, the frame collapses and a terminal notice reports the result.

When no playlists exist, the frame displays a `TerminalTextField` for the name and `[ CREATE + ADD ]`. The action remains disabled until the trimmed name is non-empty.

### Playlist Mutation

`PlaylistRepository` receives a bulk append operation. It:

1. Reads the playlist's existing paths.
2. Filters the album paths to playable, ordered, distinct paths not already present.
3. Appends the missing paths to the existing paths.
4. Performs one ordered playlist-entry replacement.
5. Returns added and skipped counts.

Existing playlist entries retain their order. Album tracks retain album-detail order. No database entities or migrations are introduced.

`PlaylistViewModel` exposes add-album and create-and-add commands. Creating a playlist and adding the album occur in one coroutine. The UI receives a small result state representing idle, working, success, no-op, or error.

### Feedback and Edge Cases

- No playable tracks: `[ ADD TO PLAYLIST ]` is disabled.
- Complete overlap: show `:: ALBUM ALREADY IN PLAYLIST`.
- Partial overlap: show added and skipped counts.
- Playlist deleted while the picker is open: refresh the list and show a warning.
- Blank new-playlist name: disable `[ CREATE + ADD ]`.
- Repository failure: retain the expanded picker and show a red terminal error.
- Successful add: collapse the picker and show confirmation.

### Launcher Icon

The icon depicts a sharp retro CRT terminal with a glowing pixel-art `N`.

- Background: terminal black.
- CRT body/screen: phosphor green with restrained glow.
- Monogram accent: amber detail on the pixel `N`.
- Geometry: square, sharp, pixel-oriented, and legible at launcher sizes.
- Adaptive icon: foreground artwork stays within Android's safe mask area.
- Legacy fallback: square PNG resources derived from the same mark.
- No additional words, gradients that soften the pixel form, rounded consumer-app styling, or unrelated musical symbols.

## Testing Strategy

- Unit-test bulk append ordering, duplicate skipping, complete overlap, and empty input.
- Unit-test create-and-add result counts.
- Compose-test picker expansion, single-playlist selection, empty-state creation, disabled state, and notices.
- Update album-detail and root screenshot references.
- Compile adaptive and legacy launcher resources.
- Run `testDebugUnitTest`, `assembleDebugAndroidTest`, `validateDebugScreenshotTest`, `lintDebug`, and `assembleDebug`.

## Open Questions

None.
