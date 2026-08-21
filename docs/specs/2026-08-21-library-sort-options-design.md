# LIB Sort Options Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Add a persistent sort control to the LIB album collection while keeping favorited albums permanently grouped first. Users can cycle among Artist, Title, Year, and Most Played ordering, with Artist as the first-run default.

## Success Criteria

- [ ] LIB displays one `[ SORT: ARTIST ]`-style button above the album grid.
- [ ] Each tap cycles `Artist -> Title -> Year -> Most Played -> Artist`.
- [ ] The chosen mode is saved immediately and restored after app restart.
- [ ] A fresh install defaults to Artist.
- [ ] Favorites always precede non-favorites.
- [ ] The selected comparison is applied independently inside both groups.
- [ ] Changing the sort reorders the grid immediately without changing favorite or catalog data.

## Scope

**In scope:**

- Add the four LIB sort modes and deterministic comparators.
- Add the single cycling bracket button above the album grid.
- Persist the selected mode in the existing terminal preferences.
- Pass preference state and the cycle action from the app shell into LIB.
- Preserve favorite-first grouping for every mode.
- Keep the selected mode during rescans, navigation, and app restarts.
- Add unit, preference, and Compose UI coverage.

**Out of scope:**

- Ascending/descending toggles.
- Sorting Search, Artists, playlists, tracks, or album-detail contents.
- New database columns or catalog migrations.
- Recently Added, manual ordering, filters, or configurable favorite placement.
- Resetting sort when the library source changes.

## Design

### Data model and state

Introduce a `LibrarySortMode` enum with stable persisted values:

- `ARTIST` -- default.
- `TITLE`.
- `YEAR`.
- `MOST_PLAYED`.

`TerminalPreferencesRepository` will store the selected value as a string under a dedicated preference key and expose it as state. Missing or unrecognized values will safely resolve to `ARTIST`, protecting startup when preferences are absent or malformed.

`TerminalSettingsState` will include the current `librarySortMode`. Its view model will expose one cycling action that advances through the enum in the approved order, persists the new value immediately, and updates observable state.

The preference is device-wide presentation state. It survives navigation, rescans, source changes, and process restarts, but requires no Room schema change.

### Interfaces and UI behavior

`LibraryLandingScreen` will receive the current `LibrarySortMode` and an `onCycleSort` callback in addition to its existing album, favorite, play-count, selection, and favorite-toggle inputs.

A full-width or naturally sized `[ SORT: <MODE> ]` bracket button will sit immediately above the grid. It remains visible whenever LIB shows the album collection, including while a scan-status panel is present. If the library is empty, the button will be omitted because there is nothing to sort.

`orderLibraryAlbums` will accept the selected mode and play-count map. It will always compare favorite status first, then apply:

- Artist: artist A-Z, then title A-Z.
- Title: title A-Z, then artist A-Z.
- Year: parsed year newest-first, unknown or invalid years last, then artist and title A-Z.
- Most Played: count highest-first, with missing counts treated as zero, then artist and title A-Z.

String comparisons will be case-insensitive, with original input order preserved when all compared values are equal. Sort changes will retain normal keyed-grid item identity, but the grid will not automatically scroll to the top.

### Error handling and edge cases

- Missing, blank, or invalid persisted sort values fall back to `ARTIST`.
- Preference writes use the existing non-blocking `SharedPreferences.apply()` convention; the UI changes immediately.
- Unknown, blank, or non-numeric years sort after valid years in Year mode.
- Albums with no play-count entry are treated as zero plays.
- Favorite status is always the primary comparison, including when an album is favorited or unfavorited while another sort is active.
- Case-only differences and otherwise identical sort keys preserve source order.
- An empty library continues to show only the existing empty-library notice.
- Rescanning may update album metadata or play counts; the displayed order recomputes from the latest state.
- No user-facing error notice is added for preference persistence because the existing preference layer does not report asynchronous `apply()` failures.

## Testing Strategy

Unit tests will verify:

- The cycle order wraps correctly.
- Artist is the default mode.
- Favorites remain first under every mode.
- Artist and Title modes use the approved primary and secondary comparisons.
- Year mode places newest valid years first and unknown years last.
- Most Played sorts descending and treats absent counts as zero.
- All final ties preserve source order.

Preference integration tests will verify:

- A selected mode survives repository recreation.
- Missing and malformed saved values resolve to Artist.
- Cycling updates both exposed state and persisted storage.

Compose UI tests will verify:

- The button displays the active mode.
- Tapping it cycles through all four labels.
- The visible album order changes accordingly.
- The control is omitted for an empty library.
- The control remains available while scan status is displayed.
- Favorite toggling repositions an album correctly under the active mode.

Existing LIB screenshots will be updated to include the new control, and the relevant unit, instrumentation, screenshot, and regression suites must pass without unrelated changes.

## Open Questions

None.
