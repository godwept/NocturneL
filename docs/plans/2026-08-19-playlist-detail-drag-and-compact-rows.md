# Playlist Detail Drag and Compact Rows Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-playlist-detail-drag-and-compact-rows-design.md`  
**Status:** Ready for review

## Overview

Extract the Queue Editor's proven reorder session, midpoint targeting, lifted-row treatment, and edge auto-scroll into shared internal Compose primitives, then use them for compact Playlist Detail rows rendered as `[::] Artist :: Track title [X]`. Playlist drops will continue through the existing position-based `onMove` callback, while per-track favorite, queue, play-count, duration, and arrow controls and their now-unused wiring are removed. The implementation must preserve the unrelated uncommitted Album Detail changes already present in `NocturneLApp.kt`, `TerminalUiScreenshotTest.kt`, and their references.

## Tasks

### Task 1: Generalize the pure reorder model (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/components/DragReorderStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/DragReorderState.kt`

**Test first:**

Create `DragReorderStateTest` and write JVM tests against a string-keyed shared API:

```kotlin
val session = beginDragReorder(listOf("a", "b", "c", "d"), "b")!!
assertEquals(listOf("a", "c", "d", "b"), session.moveTo(3).previewOrder)
assertEquals(DragReorderCommit("b", 3), session.moveTo(3).commitOrNull())
```

Also cover unknown keys, clamping, no-op commits, unchanged-order compatibility, removed/reordered-key incompatibility, midpoint targeting in both directions, ignoring the dragged row's bounds, and proportional/capped edge velocities. Write these tests before the shared source file so the test source initially fails to compile.

**Implementation:**

Add package-internal, Compose-free types and functions under `ca.stewark.nocturnel.ui.components`:

- `DragReorderSession(draggedKey, startingOrder, previewOrder, startIndex, targetIndex, translationY)`
- `DragReorderItemBounds(key, previewIndex, top, bottom)`
- `DragReorderCommit(key, targetIndex)`
- `beginDragReorder`, `moveTo`, `dragReorderTargetIndex`, `dragReorderEdgeVelocity`, `isCompatible`, and `commitOrNull`

Use the same immutable remove-and-insert, midpoint, translation-correction, and linear edge-velocity rules currently implemented in `QueueDragState.kt`. Do not include queue-specific current-occurrence state in the shared model.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*DragReorderStateTest'`. All shared pure-state tests pass.

---

### Task 2: Add a shared lazy-list drag coordinator (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/components/DragReorderLazyListState.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`

**Test first:**

Extend `QueueEditorScreenTest` before adding the coordinator:

- Add an authoritative-order invalidation case that starts dragging `second`, replaces the upcoming order, releases, and asserts no move.
- Add a height-constrained long-queue case that drags a visible handle into the bottom edge, advances the Compose clock, and asserts a row outside the initial viewport becomes visible before release.
- Assert edge scrolling stops after release or cancellation.

The stale-order case fills the current gap next to `staleCurrentTrackCancelsActiveDrag`; the edge case locks down the behavior being extracted.

**Implementation:**

Add `rememberDragReorderLazyListState(authoritativeOrder: List<String>)`, backed by one `LazyListState`, one nullable `DragReorderSession`, and edge velocity. Its returned internal state object must expose:

- `listState`, `previewOrder`, and `activeSession`
- `start(key)`, `dragBy(delta)`, `cancel()`, and `finish(): DragReorderCommit?`
- `isDragging(key)` and the active translation

Move visible-bound conversion, midpoint retargeting, translation correction, the approved 64 dp edge region, the 900 dp/second maximum, and the `withFrameNanos`/`scrollBy` loop from Queue Editor into this shared coordinator. Cancel automatically when `authoritativeOrder` no longer matches the starting snapshot. Keep screen-specific compatibility tokens and commit callbacks outside it.

**Verify:** Run `\.\gradlew.bat assembleDebugAndroidTest`. With an emulator/device, run `\.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.QueueEditorScreenTest` and confirm the new invalidation and edge-scroll cases pass.

---

### Task 3: Refactor Queue Editor onto the shared foundation (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueDragState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueDragStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/DragReorderRow.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`

**Test first:**

Run the existing queue unit and Compose tests before refactoring and retain their assertions for multi-row drops, no-op/cancelled gestures, stale current-track cancellation, custom accessibility moves, independent Jump/Remove actions, lifted-state semantics, and single-line text. Add an assertion that the Queue Editor still emits `("second", 3, "current")` after a multi-row drop through the extracted coordinator.

**Implementation:**

- Replace Queue Editor's local `LazyListState`, session, geometry, and edge loop with `rememberDragReorderLazyListState(authoritativeOrder)`.
- Keep the current occurrence captured in a queue-local variable at drag start. Cancel when it changes, and pass it unchanged to `onMove(commit.key, commit.targetIndex, expectedCurrentOccurrenceId)`.
- Add shared `DragReorderHandle` and `Modifier.dragReorderRow(...)` primitives in `DragReorderRow.kt`. The handle owns `detectVerticalDragGestures`, the `[::]` button, custom Move Up/Down actions, and the supplied test tag. The row modifier owns translation, 1.02 scale, 8 dp elevation, z-order, `TerminalBlackAlt` background, `AlertAmber` border, animation state, and `Dragging, position N of M` semantics.
- Update `UpcomingQueueRow` to consume those primitives without changing its title, metadata, Jump, Remove, tags, descriptions, or public callback contract.
- After Queue Editor is green on the shared implementation, remove the superseded `QueueDragState.kt` and `QueueDragStateTest.kt`; their generic coverage now lives in `DragReorderStateTest`.

Do not change queue playback, persistence, row content, or callback semantics.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*DragReorderStateTest'` and `\.\gradlew.bat assembleDebugAndroidTest`; run the focused connected Queue Editor test when a device is available. `rg -n 'QueueDragSession|QueueDragItemBounds|queueDragEdgeVelocity' app/src` returns no obsolete queue-only implementation.

---

### Task 4: Simplify Playlist Detail state and callback wiring (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailState.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Extend `PlaylistDetailStateTest` with playable and missing rows and assert that mapping still preserves position, path, title, artist fallback (`UNAVAILABLE`), availability, boundary flags, backing playable `track`, and available-track filtering. Then update Playlist Detail call sites in tests to the intended reduced signature so compilation identifies every stale per-track parameter.

**Implementation:**

- Remove `durationMs` from `PlaylistTrackRow`; retain `position`, `relativePath`, `title`, `artist`, `available`, `canMoveUp`, `canMoveDown`, and `track` because they still drive identity, labels, playability, accessibility, and playlist-level queueing.
- Remove `onAddTrackToQueue`, `favoriteTrackPaths`, `trackPlayCounts`, and `onToggleTrackFavorite` from `PlaylistDetailScreen`.
- Remove the same now-unused inputs from `PlaylistsScreen` and remove only their arguments from the `NocturneLDestination.PLAYLISTS` call in `NocturneLApp.kt`.
- Preserve the existing `onAddToQueue(playableTracks, skippedTracks)` playlist-level behavior.

`NocturneLApp.kt` already has unrelated Album Detail edits. Modify only the Playlist destination arguments; do not reformat or restore neighboring work.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*PlaylistDetailStateTest'` and `\.\gradlew.bat assembleDebugAndroidTest`. Run `rg -n 'onAddTrackToQueue|favoriteTrackPaths|trackPlayCounts|onToggleTrackFavorite' app/src/main/java/ca/stewark/nocturnel/ui/playlist` and confirm no playlist-package matches remain.

---

### Task 5: Render compact playlist rows and preserve Add Track results (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

Replace the existing playlist-entry presentation assertions with a compact-row test that verifies:

```kotlin
compose.onNodeWithText("Artist :: Carrier").assertIsDisplayed()
compose.onNodeWithContentDescription("Reorder Carrier").assertIsDisplayed()
compose.onNodeWithContentDescription("Remove Carrier").assertIsDisplayed()
compose.onNodeWithContentDescription("Add Carrier to queue").assertDoesNotExist()
compose.onNodeWithText("Artist · 0:01").assertDoesNotExist()
```

Also assert visible `↑`/`↓`, FAV, and play-count text are absent. Open Add Track and retain the assertion that its `[+] Artist :: Track title` result appears and invokes `onAdd`; retain the playlist-level Add Queue callback/count test separately.

Update the long-text case to inspect one combined `"$longArtist :: $longTitle"` entry layout, assert `lineCount == 1` and `hasVisualOverflow`, and separately prove the Add Track result remains one line.

**Implementation:**

Extract an internal `PlaylistEntryRow` composable that renders a shared `[::]` drag handle, one weighted `Text("${row.artist} :: ${row.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)`, and `[X]`. Remove the entry's arrow buttons, nested metadata column, `QueueTrackActions`, duration formatter import, and per-track favorite/queue/count presentation. Keep the Add Track branch byte-for-byte equivalent except for signature fallout.

**Verify:** Run `\.\gradlew.bat assembleDebugAndroidTest`. With a device, run `\.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playlist.PlaylistDetailScreenTest`. The compact and Add Track cases pass.

---

### Task 6: Connect Playlist Detail to shared drag ordering (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

Add four-entry Compose cases using stable handle tags `playlist-drag-<starting-position>`:

- Drag starting position 1 below two row midpoints and assert exactly `Move(1, 3)` after release.
- Drag starting position 3 above all earlier rows and assert exactly `Move(3, 0)`.
- Inspect the list while the pointer is down and assert the dragged row reports its preview position; release and assert no second callback occurs.

**Implementation:**

- Build each authoritative snapshot key as `"${row.position}:${row.relativePath}"`; use it as the lazy-item key while `playlist-drag-<position>` and `playlist-row-<position>` remain readable test tags.
- Remember one shared drag coordinator from the authoritative key order.
- Derive displayed rows from `previewOrder`, recalculating preview index and Move Up/Down availability without mutating `PlaylistDetailState`.
- Disable user scrolling only while a handle drag is active, keep shared programmatic edge scrolling enabled, and apply `Modifier.animateItem()` to the stable-keyed rows.
- On release, map `commit.key` to its authoritative `PlaylistTrackRow`, then call `onMove(row.position, commit.targetIndex)` once. A no-op commit calls nothing.

Do not call the ViewModel or repository during pointer movement.

**Verify:** Run the focused connected `PlaylistDetailScreenTest`. Both directions produce one position-based move with the expected destination.

---

### Task 7: Cover cancellation, duplicates, unavailable entries, and edges (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

Add focused Compose cases asserting no move for a no-op release, cancelled gesture, one-entry playlist, and state-backed authoritative-order replacement during an active drag. Add two rows with the same `relativePath` but different starting positions and prove dragging the second calls its own starting position. Include a missing row and prove its `UNAVAILABLE :: <title>` text, reorder handle, and remove control remain present.

Add a constrained-height playlist with at least twelve entries. Drag a visible handle into the bottom edge, advance frames, and assert an initially off-screen row appears; repeat from a scrolled-down position toward the top. Assert release/cancel stops scrolling and stale refresh restores authoritative order.

**Implementation:**

Use the shared coordinator's compatibility and edge behavior without playlist-specific duplication. Ensure snapshot keys, not paths or titles, drive lookup. Let changed authoritative keys cancel the coordinator before `finish()` can project a commit. Preserve missing-row fallback data from `playlistDetailState`.

**Verify:** Run the focused connected `PlaylistDetailScreenTest`. All no-op, cancellation, duplicate, unavailable, and two-direction edge-scroll cases pass.

---

### Task 8: Preserve accessibility moves and remove isolation (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/DragReorderRow.kt`

**Test first:**

Read `SemanticsActions.CustomActions` from the middle row's handle and invoke `Move <title> up` and `Move <title> down`. Assert callbacks are exactly `Move(originalPosition, previewIndex - 1)` and `Move(originalPosition, previewIndex + 1)`, and assert first/last handles omit invalid boundary actions. Then click Remove after a drag and assert only `onRemove(originalPosition)` fires—no additional move or add callback.

**Implementation:**

Supply the shared handle with playlist-specific one-position callbacks and the row's current preview boundary flags. Keep pointer input confined to `[::]`; `[X]`, the text region, Add Track controls, and screen-level actions must not initiate dragging. Keep full titles in accessibility labels even when visible text is ellipsized.

**Verify:** Run the focused connected `PlaylistDetailScreenTest`. Accessibility actions and Remove remain independent and position-correct.

---

### Task 9: Update deterministic playlist screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/PlaylistDetailPreview_Playlist detail_1da3cfda_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/PlaylistDraggedRowPreview_Playlist dragged row_*.png`

**Test first:**

Keep `PlaylistDetailPreview` at 412 x 915 dp, add a `@PreviewTest` named `Playlist dragged row` that renders the internal row with deterministic missing/playable data in active drag state, and run screenshot validation before updating references. Confirm the existing Playlist Detail reference differs and the new dragged-row reference is missing.

**Implementation:**

Update only the Playlist Detail reference and generate the new dragged-row reference. Inspect them for:

- `[::] Artist :: Track title [X]` alignment and compact height
- One-line ellipsis before fixed controls
- No FAV, `+Q`, play count, duration, or arrows
- Amber border, dark surface, elevation, and unclipped buttons in active state

`TerminalUiScreenshotTest.kt` and unrelated Album Detail references already contain uncommitted work. Preserve those edits exactly and do not regenerate, remove, or accept unrelated references.

**Verify:** Run `\.\gradlew.bat updateDebugScreenshotTest`, inspect only the two intended Playlist images, then run `\.\gradlew.bat validateDebugScreenshotTest`. All screenshot tests pass without unrelated reference changes.

---

### Task 10: Run full regression and device verification (2–5 min)

**Files:** All files listed in Tasks 1–9

**Test first:**

No new code in this task. Cross-check every approved success criterion against a focused automated test before running the full suite. Add no cleanup outside the design.

**Implementation:**

Run the complete verification set. On a Pixel 7 or equivalent emulator/device, manually reorder a playlist longer than the viewport in both directions; confirm the row remains under the finger, neighbors shift, edge scrolling starts and stops predictably, release performs one move, cancellation restores order, duplicate and unavailable rows remain independent, Remove still targets the intended entry, and Add Track remains unchanged.

**Verify:**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
git status --short
```

If no device is attached, report connected tests and manual interaction checks as pending rather than claiming they passed. Confirm the final diff contains only this approved feature, its design/plan/tests/references, and the pre-existing Album Detail work.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every behavior change was developed test-first.
- [ ] Queue Editor passes unchanged behavior through the shared drag foundation.
- [ ] Playlist entries render only `[::] Artist :: Track title [X]` on one ellipsized line.
- [ ] Playlist dragging previews continuously, edge-scrolls, and commits exactly one position-based move.
- [ ] No-op, cancelled, stale, duplicate, single-item, and unavailable-entry cases behave as designed.
- [ ] Accessible Move Up/Down and Remove remain correct and independent.
- [ ] Add Track and playlist-level actions remain unchanged.
- [ ] Unit tests, Android-test assembly, connected tests when available, screenshots, lint, and debug assembly pass.
- [ ] No persistence, dependency, Album Detail, Search, or unrelated files were changed.
- [ ] The feature behaves exactly as described in the approved design document.
