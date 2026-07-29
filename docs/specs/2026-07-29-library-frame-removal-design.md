# Library Frame Removal Design

**Date:** 2026-07-29
**Status:** Approved

## Goal

Remove the complete folder and rescan frame from the top of the Library screen so the album grid becomes the screen's immediate focus. Rescanning remains available from Settings, while scan progress and result messages continue to use the existing shared terminal status area.

## Success Criteria

- [ ] The Library screen opens directly into the album grid.
- [ ] The Library screen no longer shows the source-folder frame, Rescan button, Cancel button, or scan-report summary.
- [ ] Settings continues to provide the Rescan Library action.
- [ ] Scan progress, completion, cancellation, and error messages remain visible through the shared terminal scaffold.
- [ ] Scanning behavior, library setup, navigation, and album-grid styling remain unchanged.

## Scope

**In scope:**

- Remove the folder title, Rescan, Cancel, and scan-report summary frame from Library.
- Simplify the Library screen to render the album grid directly.
- Remove the Library screen's now-unused ViewModel parameter.
- Update affected screenshot coverage and tests.

**Out of scope:**

- Changes to scanning logic or scan state.
- Changes to the Settings screen.
- Changes to library setup, navigation, or album-grid styling.
- Moving scan reports to another Library-specific component.

## Design

`LibraryScreen` will accept only the album list and album-selection callback. It will render `AlbumGridScreen` directly without a wrapping column or status frame.

`NocturneLApp` will continue to own `LibrarySourceViewModel` and pass `scanState.message` to `TerminalScaffold`. This preserves scan feedback without reserving space above the album grid. The Settings destination will retain its existing `RESCAN LIBRARY` action and callback.

No data model, repository, database, scanning, or ViewModel APIs will change.

## Testing Strategy

- Update the Library/root screenshot reference so the album grid begins at the top of the content area.
- Verify the Library screen no longer renders `RESCAN`, `CANCEL`, the source-folder frame, or scan-summary text.
- Verify Settings still renders `RESCAN LIBRARY`.
- Run unit tests, Android-test compilation, screenshot validation, lint, and debug assembly.

## Open Questions

None.
