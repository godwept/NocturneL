# LIB Terminal Cover Flow Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Add a persistent alternate view to the LIB page that presents the sorted album collection as a horizontally swiped terminal reel. The existing two-column grid remains the initial default, while users can switch to a focused album-browsing experience that fits NocturneL's phosphor, ASCII-terminal visual language.

## Success Criteria

- [ ] LIB exposes a `[ VIEW: GRID ]` / `[ VIEW: FLOW ]` control whenever albums are available.
- [ ] The chosen view persists across app restarts, with grid as the first-run and invalid-value fallback.
- [ ] Flow mode snaps one album into the center and stops at both ends of the collection.
- [ ] Tapping a side cover centers it, while tapping the centered cover opens album detail.
- [ ] Grid and flow retain independent scroll positions.
- [ ] Sorting, favorites, play counts, and album selection behave consistently in both views.
- [ ] Flow remains usable with effects disabled or reduced motion enabled.

## Scope

**In scope:**

- Add a LIB-only toggle between the existing two-column grid and the terminal reel.
- Persist the selected view through the existing settings and preferences system.
- Add horizontal swipe navigation with centered-item snapping and bounded ends.
- Implement the agreed center-cover and side-cover tap behavior.
- Show the centered album's title, artist, play count, and favorite control.
- Apply the existing sort order identically to grid and flow.
- Provide terminal styling, accessibility semantics, reduced-motion behavior, tests, and screenshot coverage.

**Out of scope:**

- Replacing album grids on Artist or Search screens.
- Infinite wrapping, autoplay, audio previews, or automatic reel movement.
- Changes to album data, artwork processing, scanning, playback, or database schemas.
- New sorting modes or new album actions.
- Synchronizing scroll position between grid and flow.
- True 3D rendering or heavy physics effects.

## Design

### View state

Introduce a two-value library view mode: `GRID` and `FLOW`.

The persisted preference owns the active mode, with `GRID` as the fallback for missing or invalid saved values. The existing grid keeps its own `LazyGridState`; the reel keeps an independent horizontal-list state whose centered index represents the selected album. Scroll positions are saveable session state rather than durable preferences.

Both views consume the same already-sorted album list. If sorting or a library rescan changes that list, the reel preserves the centered album by ID when it still exists; otherwise it selects the nearest valid album. An empty library continues to show the existing notice, with no view or sort controls because there is nothing to browse.

### Controls and layout

LIB's control row contains two bracket controls:

```text
[ SORT: ARTIST ]  [ VIEW: GRID ]
```

The view button toggles directly between `GRID` and `FLOW`; its label describes the currently active view.

Flow mode presents a horizontally swipeable, snapping album reel. One larger centered cover appears inside a bright ASCII-style frame, while smaller and dimmer neighboring covers remain visible without perspective distortion. Lightweight scale, spacing, framing, and optional motion create depth without true 3D rendering.

Below the reel, a `> CURRENT_` marker identifies the selection alongside an album position such as `03 / 48`. The selected album's title, artist, play count, and existing favorite control are displayed as a compact terminal readout. The first and last positions have clear edge treatment and do not imply wrapping.

### Interaction

A horizontal swipe moves through albums and settles with exactly one album centered. Tapping a side cover scrolls it into the center without navigating. Tapping the centered cover invokes the existing album-selection callback and opens album detail.

Sorting continues through the existing sort callback, and both views receive the same sorted album list. Changing the view uses a new persisted-view callback supplied by the app shell. Grid and flow keep independent positions when toggling between them.

The reel exposes appropriate spoken descriptions for covers, selection, view switching, position, and favorite state. Visible terminal controls retain accessible touch targets.

### Motion and visual effects

The reel follows the existing effects and reduced-motion policy. With effects enabled, selection changes may use restrained scrolling, scale, dimming, and glow transitions. With effects disabled or reduced motion active, decorative transitions are removed or minimized while snapping, selection, and navigation remain functional.

### Error handling and edge cases

- A single album is centered with no implied scrolling; tapping it opens album detail.
- With two albums, the selected cover remains centered where possible and the reel visibly stops at each end.
- Missing or failed artwork uses the existing deterministic terminal placeholder.
- Long album and artist names truncate cleanly without shifting controls.
- Sorting, rescanning, and favorite changes do not leave stale indexes or cause crashes.
- If the selected album disappears, selection moves to the nearest remaining album.
- Rapid swipes settle on exactly one album.
- Rotation and process recreation restore the chosen view. Each view's scroll position receives normal saveable-session restoration only.
- An empty library retains the existing notice and omits inactive sort and view controls.

## Testing Strategy

Unit tests will verify persisted view-mode parsing, including the `GRID` fallback; view-mode toggling; reel selection reconciliation when albums are reordered, removed, or emptied; and centered-index bounds at both ends.

Compose tests will verify that the view button toggles grid and flow; side-cover taps center without opening; center-cover taps open album detail; swipes snap to one album without wrapping; selected-album metadata, play count, favorite state, and position are exposed; favorite and sort actions work in flow mode; and grid and flow retain independent positions. Coverage will include one-album, two-album, empty-library, long-text, missing-artwork, touch-target, and accessibility cases.

Screenshot coverage will add a populated terminal-reel state and an effects-off or reduced-motion state. The default LIB golden will be updated only where the new view button changes the existing grid presentation.

## Open Questions

None.
