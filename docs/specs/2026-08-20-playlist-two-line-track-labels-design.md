# Playlist Two-Line Track Labels Design

**Date:** 2026-08-20
**Status:** Approved

## Goal

Make track titles consistently visible on the Playlist Details screen, even when artist names are long, by placing artist and title on separate predictable lines.

## Success Criteria

- [ ] Playlist entries show artist on the first line and track title on the second.
- [ ] Add Track results use the same two-line arrangement.
- [ ] Each field occupies exactly one line and ellipsizes independently.
- [ ] Long artist names never consume or obscure the title line.
- [ ] Existing reorder, add, remove, queue, and drag interactions remain unchanged.
- [ ] Both lists share one label component and visual treatment.

## Scope

**In scope:**

- Replace `ARTIST :: TRACK` text in playlist entries with a shared two-line label.
- Apply the same label to Add Track search results.
- Keep artist above title.
- Limit each line to one line with end ellipsis.
- Allow affected rows to grow just enough for two text lines.
- Update Compose tests and playlist-detail screenshots.

**Out of scope:**

- Changes to playlist data, sorting, filtering, or persistence.
- Changes to drag/reorder, add, remove, play, or queue actions.
- Album or duration metadata in these rows.
- Changes to track rows on other screens.
- User-configurable row density or typography.

## Design

### State and Component Design

No new state or data-model fields are required. Both affected row types already receive artist and title separately.

Add one internal playlist-specific composable that accepts `artist`, `title`, and an optional `Modifier`. It renders a vertical stack filling the available row width: artist on the first line and title on the second. Each text element uses `maxLines = 1` and `TextOverflow.Ellipsis`, so either value can truncate without consuming the other value's line.

The existing playlist row places this component between the drag handle and remove button. The Add Track result places it between the add button and the row edge. Both call sites give the shared component the flexible middle width. Existing callbacks, semantics, keys, and drag state remain unchanged.

### Visual Treatment and Interaction

Both lines retain the screen's existing text style and color. The change separates the values spatially without introducing a new hierarchy or color treatment. Horizontal padding remains consistent with the current rows, and the two-line label takes only the flexible middle width so action buttons preserve their existing touch targets.

Drag handles and add/remove buttons remain vertically centered beside the two lines. Tapping action buttons behaves exactly as before. The text adds no click or accessibility action. Compose exposes artist and title as separate text nodes so each can be verified independently.

### Error Handling and Edge Cases

- Long artist and title values ellipsize independently and remain confined to their assigned lines.
- A blank artist leaves the first line empty; the title remains on the second line. No new fallback text is introduced.
- Missing and unavailable playlist entries retain the same two-line presentation and existing actions.
- Duplicate tracks remain independent rows with unchanged keys and positions.
- Narrow screens preserve action buttons first; the flexible text area takes the remaining width.
- Two-line row height grows naturally without changing drag calculations or fixed action-button touch targets.

## Testing Strategy

- Add focused Compose coverage proving artist and title are separate displayed nodes.
- Verify each text node produces exactly one layout line and independently reports visual overflow for deliberately long values at a narrow width.
- Verify the title remains displayed when the artist overflows.
- Update Playlist Details integration assertions for both existing entries and Add Track results.
- Retain existing tests for add, remove, queue, reorder, accessibility moves, duplicate rows, and drag cancellation.
- Update affected playlist-detail and dragged-row screenshot references.
- Inspect screenshots at the existing Pixel 7 width for clipping, button alignment, readable separation, and consistent row spacing.
- Run unit tests, Android-test compilation, screenshot validation, lint, and debug assembly.

## Open Questions

None.
