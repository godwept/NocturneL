# NOW Tab Condensed Controls Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Condense the NOW tab by removing the standalone play-count row so the scrubber, track times, playback controls, and queue section appear higher on the screen. Preserve the existing terminal presentation while making the favorite and repeat states clearer.

## Success Criteria

- [ ] Album metadata displays as `ALBUM · N PLAY(S)` when a track is loaded.
- [ ] The scrubber, track times, playback controls, and queue section move upward by one text row.
- [ ] `SHF` and `RPT` remain grouped on the left of the secondary controls row.
- [ ] `FAV` appears at the far right of the secondary controls row.
- [ ] Repeat displays `[ RPT ]` when off, `[ RPT:A ]` for repeat all, and `[ RPT:1 ]` for repeat one.
- [ ] Existing selected-state styling remains for active shuffle, repeat, and favorite states.

## Scope

**In scope:**

- Condense only the NOW tab.
- Append the current track's play count to the album metadata line.
- Remove the standalone play-count/favorite row.
- Move `FAV` to the right side of the shuffle/repeat row.
- Make the repeat button text reflect OFF, ALL, and ONE.
- Keep current button actions and repeat cycling order unchanged.
- Update NOW-tab UI and screenshot coverage for the new layout and repeat labels.

**Out of scope:**

- Changes to playback, favorite, repeat, or play-count persistence.
- Changes to other screens that display favorites or play counts.
- Redesigning the transport controls, visualizer, queue editor, or bottom navigation.
- Additional responsive layouts beyond the existing supported screen conventions.

## Design

### State and display rules

No new persisted state is required. The NOW tab continues to use the existing playback and listening state:

- Repeat off renders `RPT` with inactive styling.
- Repeat all renders `RPT:A` with active styling.
- Repeat one renders `RPT:1` with active styling.
- Active shuffle retains the selected `SHF` styling.
- A favorite current track retains the selected `FAV` styling.

When a current track exists, the album line becomes `ALBUM · N PLAY(S)`. When no track exists, the existing empty metadata presentation remains, the play count is omitted, and the `FAV` button is not displayed.

### UI interface

The existing `NowPlayingScreen` inputs and callbacks remain unchanged. The album marquee presents the combined album and play-count text. The secondary controls row uses two zones: a left group containing `SHF` and the dynamic `RPT` button, and a far-right `FAV` button.

The repeat glyph is derived from the current repeat mode. Its accessibility description identifies the current mode while the callback continues cycling OFF → ALL → ONE → OFF. The favorite callback continues to toggle only the current track. No new navigation, repository, playback-service, or persistence interface is introduced.

### Error handling and edge cases

- With no current track, the album line remains empty and neither the play count nor `FAV` is shown.
- A zero play count displays as `0 PLAY(S)`, matching existing wording.
- Long combined album/count text uses the existing marquee behavior instead of wrapping onto another row.
- `RPT:A` and `RPT:1` remain single-line button labels.
- The far-right `FAV` position remains stable when the repeat label changes width.
- Active repeat styling applies to ALL and ONE; OFF remains inactive.
- Accessibility text distinguishes repeat off, repeat all, and repeat one so the state is not conveyed by color or abbreviation alone.

## Testing Strategy

- Verify the album metadata displays `ALBUM · N PLAY(S)` and no standalone count row remains.
- Verify `SHF` and `RPT` occupy the left control group while `FAV` is aligned at the far right.
- Verify repeat modes render as `RPT`, `RPT:A`, and `RPT:1` with the correct selected styling and accessibility descriptions.
- Verify `FAV` appears only when a current track exists and still invokes its callback.
- Verify shuffle, repeat, favorite, transport, seek, and queue callbacks remain functional.
- Update the NOW screenshot reference to confirm the condensed vertical layout and higher queue section.
- Run the relevant unit, instrumentation, and screenshot tests and confirm there are no unrelated changes.

## Open Questions

None.
