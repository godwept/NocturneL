# Album Detail Metadata Actions Row Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Condense the Album Detail metadata controls by placing `FAV`, `SET COVER`, and `ADD TO PLAYLIST` together on one natural-width row while keeping the album play count on its own line immediately above them.

## Success Criteria

- [ ] Album play count appears on a dedicated line.
- [ ] `FAV`, `SET COVER`, and `ADD TO PLAYLIST` appear in that order on one row.
- [ ] All three controls retain their current labels, natural widths, and minimum touch-target height.
- [ ] The row fits at the existing 412dp narrow-screen reference width without wrapping or clipping.
- [ ] `CLEAR` is absent from Album Detail.
- [ ] Favorite, cover selection, and playlist-picker behavior otherwise remains unchanged.

## Scope

**In scope:**

- Reorganize the Album Detail album-information controls.
- Move album `FAV` beside `SET COVER` and `ADD TO PLAYLIST`.
- Keep the album play count immediately above the new control row.
- Remove the Album Detail `CLEAR` control and its now-unused screen callback and caller wiring.
- Update Album Detail layout tests and screenshot references.

**Out of scope:**

- Track-row controls or spacing.
- The top-level `BACK`, `PLAY`, `SHUFFLE`, and `ADD QUEUE` controls.
- Playlist-picker contents or behavior.
- Changing or deleting an already stored manual cover.
- Other artwork-management screens or persisted artwork data.

## Design

### State and Interfaces

No new state is required. Existing album favorite state and playlist-picker state continue to drive `FAV` and `ADD TO PLAYLIST`.

`AlbumDetailScreen` drops `onClearArtwork`, and its caller removes the corresponding callback. `onChooseArtwork`, favorite handling, playlist-picker callbacks, and stored manual artwork remain unchanged. Manual artwork continues to display, but Album Detail no longer exposes an action to clear it.

### Layout

The album play-count text occupies a dedicated line. The following natural-width `Row` contains:

```text
[ FAV ] [ SET COVER ] [ ADD TO PLAYLIST ]
```

The controls retain their current typography, labels, natural widths, and minimum touch targets. `ADD TO PLAYLIST` remains disabled when there are no playable tracks and remains selected while the playlist picker is expanded.

### Error Handling and Edge Cases

- Albums without playable tracks keep `ADD TO PLAYLIST` disabled.
- Existing manual artwork remains visible and selectable artwork may still replace it.
- All labels remain single-line and fully visible at the 412dp reference width.
- Removing `CLEAR` affects only Album Detail UI and does not delete or migrate stored artwork data.

## Testing Strategy

- Add an Album Detail layout test confirming `FAV`, `SET COVER`, and `ADD TO PLAYLIST` share the same vertical position.
- Confirm the play count appears above the three controls.
- Assert `CLEAR` is absent even when the album has manual artwork.
- Preserve coverage for favorite toggling, cover selection, playlist-picker enablement, and selected state.
- Update and inspect the 412dp Album Detail screenshot for one-line labels without clipping.
- Run unit tests, Android-test compilation, screenshot validation, lint, and debug assembly.

## Open Questions

None.
