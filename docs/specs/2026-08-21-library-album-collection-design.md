# LIB Album Collection Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Simplify the LIB tab into a focused album collection. Resume, listening-history, and dedicated favorites content will no longer compete with album browsing, while favorited albums remain easy to reach by appearing first in the collection.

## Success Criteria

- [ ] LIB displays one continuous album grid without Resume, Favorites, or Recently Played sections.
- [ ] Favorites and Listening History subviews are no longer reachable from LIB.
- [ ] Favorited albums appear first, alphabetically by album title, followed by all other albums alphabetically.
- [ ] Toggling an album's favorite status immediately moves it to its new sorted position.
- [ ] Listening history, play counts, resume state, and favorite data remain stored for use outside the removed LIB sections.

## Scope

**In scope:**

- Simplify the LIB landing screen to the album grid and existing empty-library notice.
- Apply favorite-first, case-insensitive alphabetical sorting for display.
- Keep favorite markers, favorite toggling, and play counts on album cards.
- Remove LIB callbacks, routing, and UI tests tied to Resume, Recently Played, Favorites, and History.
- Update affected screenshot coverage.

**Out of scope:**

- Deleting listening-history or favorite data.
- Changing playback resume behavior outside LIB.
- Removing track favorites or play-count tracking.
- Changing album detail, Search, playlists, queue, or Now Playing.
- Adding a replacement screen or navigation destination for listening history.

## Design

### Album collection presentation

`LibraryLandingScreen` will render one continuous album grid. It will no longer render the Resume card, dedicated Favorites section, Recently Played section, or links to Favorites and Listening History.

The displayed albums will be derived in the LIB presentation layer from the existing album collection and favorite album IDs. Albums will be ordered using favorite status first, with favorites leading, and then album title using case-insensitive alphabetical comparison. Existing source order will act as the stable tie-breaker for titles that compare equally. Missing or blank titles will retain the existing display fallback and sort consistently.

Because the ordering is derived from reactive favorite state, favoriting or unfavoriting an album will immediately move its card to the appropriate group. No visual divider or separate favorites block will interrupt the grid.

### State and persistence

No database or persisted-state changes are required. Listening history, resume state, play counts, album favorites, and track favorites will continue to be recorded and retained.

When no albums exist, LIB will show the existing `No playable albums yet. Rescan after adding music.` notice regardless of retained resume, history, or favorite state. When no albums are favorited, the full collection will simply be alphabetical.

### Interfaces and navigation

`LibraryLandingScreen` will accept only the album-collection state and actions it needs: albums, favorite album IDs, album play counts, grid state, album selection, and album favorite toggling.

Resume state and its callback, track selection and track-favorite callbacks, and Favorites and History navigation callbacks will be removed from this screen. The LIB app-shell branch will render the simplified landing screen directly, and its internal Favorites and History routing cases will be removed. Existing scan-status presentation above the album collection will remain unchanged.

## Testing Strategy

- Verify LIB renders only the album collection, without Resume, Favorites, or Recently Played sections.
- Verify favorited albums precede non-favorites and both groups are ordered alphabetically without regard to case.
- Verify toggling favorite status immediately reorders the album.
- Verify an empty album collection shows the existing empty-library notice even when listening data exists.
- Verify Favorites and History subviews are no longer wired through LIB.
- Preserve coverage for scan-state presentation, album selection, favorite markers, and play counts.
- Update affected screenshot references and run the relevant unit, Compose UI, screenshot, and regression suites during implementation.

## Open Questions

None.
