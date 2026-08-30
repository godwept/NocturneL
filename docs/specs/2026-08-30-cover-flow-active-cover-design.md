# Cover Flow Active Cover Design

**Date:** 2026-08-30
**Status:** Approved

## Goal

Make the active album artwork the clear focal point of the portrait cover-flow view while retaining enough of the previous and next covers to communicate horizontal navigation within limited screen space.

## Success Criteria

- [ ] The active cover reaches `340dp` when space permits.
- [ ] The active cover uses approximately 84% of the available width, capped at `340dp`.
- [ ] Available height can reduce the cover size so metadata and controls remain visible without vertical scrolling.
- [ ] Approximately 10–15% of each available neighboring cover remains visible.
- [ ] Neighboring covers are smaller, dimmed, flat, and layered behind the active cover.
- [ ] Swiping snaps one album into the centered position.
- [ ] The collection retains hard stops at its first and last albums.
- [ ] Tapping a side cover centers it; tapping the active cover opens it.
- [ ] The layout remains usable with one album, two albums, and compact portrait screens.

## Scope

**In scope:**

- Resize the active cover responsively up to `340dp`.
- Overlap the immediately previous and next covers behind the active cover.
- Adjust cover scale, opacity, stacking order, and horizontal placement.
- Preserve the current snapping, selection, tap behavior, effects setting, and accessibility descriptions.
- Handle narrow and short portrait screens responsively.
- Present one-album, two-album, and collection-endpoint states correctly.
- Update UI and screenshot coverage for the revised layout.

**Out of scope:**

- Continuous wrap-around navigation.
- Angled or 3D cover transformations.
- Titles or controls on neighboring covers.
- Changes to the metadata panel, favorite behavior, sorting, or grid view.
- A user setting for cover size or overlap amount.
- A landscape or tablet redesign beyond keeping those layouts functional.

## Design

### Visual Layout

Use an overlapping snapping reel. The centered album is displayed at the responsive active-cover size and stacked above its immediate neighbors. The neighboring covers remain flat, are scaled down and dimmed, and are positioned behind the active cover so approximately 10–15% of each available cover is exposed.

The active cover size is derived from both available width and height:

- Target approximately 84% of the available width.
- Never exceed `340dp`.
- Reduce the size further when required to keep the existing metadata and controls visible without vertical scrolling.

The immediate neighbors should begin around 76% of the active-cover scale and 50% opacity, with final values visually tuned within the approved smaller-and-dimmed treatment. More distant covers are hidden or remain outside the visible stage so they do not produce additional slivers.

### State

No new persistent state is required. The existing selected album ID and reel scroll state remain authoritative.

Each visible cover's scale, opacity, horizontal position, and stacking order are derived continuously from its distance to the viewport center. The centered item receives full emphasis and the highest stacking order. As the user drags, emphasis transitions toward the approaching album. After release, the nearest album snaps to the center and becomes selected.

### Interaction Contract

The existing `AlbumCoverFlowScreen` interface remains unchanged.

- Horizontal dragging moves the overlapping reel.
- Releasing snaps the nearest album to the center.
- Tapping the exposed portion of a neighboring cover centers it without opening it.
- Tapping the active cover opens that album.
- The active album drives the metadata and favorite controls.
- The active cover receives touches across its full area; neighboring covers respond only in their exposed regions.
- Accessibility descriptions continue to identify the selected album and its collection position.
- With visual effects disabled, selection changes occur without decorative transition animations, while sizing, overlap, dimming, and navigation remain intact.

### Edge Cases

- **One album:** Show one centered, full-size cover without empty side placeholders.
- **Two albums:** Show only the available neighbor at each hard-stopped endpoint.
- **First or last album:** Leave the unavailable side open; do not wrap or duplicate artwork.
- **Compact portrait height:** Shrink the cover below its width-derived size so metadata, favorite control, and status area remain visible.
- **Very narrow width:** Preserve outer breathing room and reduce the cover proportionally.
- **Missing artwork:** Use the existing retro placeholder at the same dimensions and layering as normal artwork.
- **Library changes or reordering:** Preserve selection by album ID where possible. If the selected album disappears, retain the nearest valid position.
- **Long metadata:** Preserve the existing single-line ellipsis behavior for album and artist names.
- **Constraint changes:** Recalculate cover sizing and overlap without changing the selected album.
- **Rapid or interrupted swipes:** Settle on the album nearest the viewport center and keep its metadata synchronized.

## Testing Strategy

- Unit-test responsive sizing, including the `340dp` cap and width and height constraints.
- Unit-test overlap geometry and visual states for centered, adjacent, and distant covers.
- Verify one-album, two-album, first-album, middle-album, and last-album arrangements.
- Use Compose UI tests to verify side-cover taps, active-cover opening, snapping, hard stops, synchronized metadata and favorite state, and exposed clickable regions.
- Verify accessibility descriptions for selected state and collection position.
- Add screenshot coverage for standard `412 x 915dp` portrait and a compact portrait size.
- Cover middle and endpoint selections with visual effects enabled and disabled.
- Keep existing grid-view and library-ordering tests passing unchanged.

## Open Questions

- Final neighbor scale, opacity, and exact exposure within the approved 10–15% range may be tuned through screenshot review without changing the interaction or layout model.
