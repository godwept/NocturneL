# Queue Drag Reordering Implementation Plan

**Date:** 2026-08-18  
**Design doc:** `docs/specs/2026-08-18-queue-drag-reordering-design.md`  
**Status:** Ready for review

## Overview

Replace the Queue Editor's one-row-at-a-time drag callback with a screen-local, occurrence-ID-based drag session. The real row will lift and track the pointer, neighboring rows will animate through a temporary preview order, edge proximity will scroll long queues, and a valid release will call the existing `onMove` exactly once. Pure geometry and state transitions will receive JVM coverage first, while Compose tests will cover gestures, stale-state cancellation, accessibility, and independent row actions. No playback policy, persistence, build dependency, or non-queue list changes are required.

## Tasks

### Task 1: Define drag-session identity and preview ordering (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueDragStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueDragState.kt`

**Test first:**

Create `QueueDragStateTest` with tests that begin a session from `listOf("a", "b", "c", "d")` and assert:

- Dragging occurrence `"b"` starts at index 1, captures the complete starting order and expected current occurrence, and initially has the same preview order.
- Moving the session to index 3 produces `a, c, d, b` without changing the captured starting order.
- Moving back to index 0 produces `b, a, c, d`.
- Beginning with an unknown occurrence returns `null`.
- Target indexes below zero and beyond the final index clamp to the first and last positions.

Use duplicate paths/titles only at higher layers; this state accepts occurrence IDs exclusively, so distinct duplicate occurrences remain distinct by construction.

**Implementation:**

Add an internal immutable `QueueDragSession` containing `draggedOccurrenceId`, `startingOrder`, `previewOrder`, `startIndex`, `targetIndex`, `expectedCurrentOccurrenceId`, and `translationY`. Add focused pure functions to begin a session and move its occurrence to a clamped preview index. Reordering must remove and insert the occurrence by ID, never swap only adjacent rows and never mutate the authoritative `QueueEditorState` list.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests '*QueueDragStateTest'`. The new ordering tests pass.

---

### Task 2: Resolve destinations from measured row midpoints (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueDragStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueDragState.kt`

**Test first:**

Add table-driven tests for a pure `queueDragTargetIndex` calculation using visible row bounds expressed as occurrence ID, preview index, top, and bottom pixels. Cover:

- Remaining within the source row keeps the source index.
- Passing the next row's midpoint selects that row's preview index.
- One large downward movement can select the last of several crossed rows.
- One large upward movement can select the first of several crossed rows.
- A center above or below all visible bounds clamps to the first or last visible preview index.
- The dragged occurrence's own bounds are ignored when selecting a crossed neighbor.

**Implementation:**

Add internal `QueueDragItemBounds` and the pure midpoint-based target resolver. It must consume current `LazyListState.layoutInfo` data translated into simple bounds, use preview indexes rather than track metadata, and return the existing target when there is no usable visible neighbor. Keep Compose types out of this file so JVM tests remain fast.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests '*QueueDragStateTest'`. All midpoint and ordering tests pass.

---

### Task 3: Define edge-scroll and stale-session policy (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueDragStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueDragState.kt`

**Test first:**

Add tests that assert:

- A dragged center outside the top 64 dp and bottom 64 dp edge regions produces zero scroll velocity.
- Moving deeper into the top region produces a negative velocity, and moving deeper into the bottom region produces a positive velocity, capped at 900 dp/second.
- A session is compatible only while both the authoritative occurrence order and current-track occurrence equal the values captured at drag start.
- Removing the dragged occurrence, changing the order, or changing the current occurrence makes the session incompatible.
- A session whose target equals its start index has no move to commit; a changed target returns exactly the dragged ID, target index, and expected current ID required by `onMove`.

**Implementation:**

Add pure edge-velocity, compatibility, and optional commit-projection helpers. Accept edge size and maximum velocity as arguments in pixel units so density conversion stays in Compose. Velocity should scale linearly with edge penetration and clamp at the configured maximum. Do not add fling, vibration, or sound policy.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests '*QueueDragStateTest'`. All state-policy tests pass.

---

### Task 4: Hoist one drag coordinator into the queue list (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Add a Compose test with four upcoming occurrences. Drag the second handle far enough to cross two row midpoints, release it, and assert the captured callback is exactly one triple: `("second", 3, "current")`. Add the inverse upward case from the fourth row to index 0. The tests must fail against the current implementation, which emits repeated adjacent callbacks from stale row indexes.

**Implementation:**

In `QueueEditorScreen`, remember one `LazyListState` and one nullable `QueueDragSession`. Derive displayed `QueueEditorRow`s by looking up the authoritative rows in `session.previewOrder`, copying `upcomingIndex`, `canMoveUp`, and `canMoveDown` from each preview position. Move gesture ownership out of each row's local `dragDistance`; the handle instead calls list-level start, delta, end, and cancel lambdas. On each delta:

1. Consume the pointer change.
2. Add the delta to the active row's translation.
3. Convert visible `LazyListItemInfo` entries to `QueueDragItemBounds`.
4. Resolve the crossed midpoint and update the preview order when its target changes.
5. Correct the stored translation by the source/destination layout offset difference so the row does not jump when the preview list reorders.

On release, project at most one commit, clear the temporary session, and invoke the existing `onMove(occurrenceId, targetIndex, expectedCurrentOccurrenceId)` only when the destination changed. Keep the callback signature and `NocturneLApp` wiring unchanged.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.QueueEditorScreenTest`. The new multi-row cases pass and emit one callback each.

---

### Task 5: Render the real row as a lifted terminal surface (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Tag every upcoming row as `queue-row-<occurrenceId>`. While a pointer remains down after crossing a midpoint, assert the dragged row exposes a state description in the form `Dragging, position 3 of 4`; after release, assert that state description is absent. Also assert the non-dragged rows appear in preview order during the gesture.

**Implementation:**

Extract the existing row body into an internal `UpcomingQueueRow` that receives `isDragging`, `dragTranslationY`, and the proposed position. Apply `Modifier.animateItem()` to stable-keyed rows so neighbors animate into preview positions. For the active row, use `graphicsLayer` to apply its live Y translation, a 1.02 scale, 8 dp shadow elevation, and a higher `zIndex`; add a `TerminalBlackAlt` background and a 1 dp `AlertAmber` border for terminal-theme emphasis. Expose the dragging position through `stateDescription` on the row. Preserve the existing title, artist, duration, handle, Jump, Remove, and minimum touch targets.

**Verify:** Run the focused `QueueEditorScreenTest` connected test. The lifted semantics and preview-order assertions pass.

---

### Task 6: Cancel no-op, interrupted, and stale drags (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Add Compose cases that assert no move callback when:

- A handle is pressed and released without crossing a midpoint.
- The pointer gesture is cancelled.
- A state-backed test changes `current.occurrenceId` during the drag.
- A state-backed test removes the dragged occurrence or changes the authoritative upcoming occurrence order during the drag.
- The queue has only one upcoming track.

After cancellation, assert the authoritative order is restored and no row retains the dragging state description.

**Implementation:**

Use `LaunchedEffect` keyed by the authoritative current occurrence and upcoming occurrence order to compare active sessions with the pure compatibility helper. Clear incompatible sessions without calling `onMove`. Wire `detectVerticalDragGestures.onDragCancel` to the same local reset. Disable user-driven `LazyColumn` scrolling only while a handle drag is active so the list cannot steal the reorder gesture; programmatic edge scrolling must remain enabled.

**Verify:** Run the focused connected `QueueEditorScreenTest`. Every cancellation case passes without a stale callback.

---

### Task 7: Add frame-driven edge auto-scroll (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Render a height-constrained queue with at least twelve upcoming rows and a paused Compose test clock. Start dragging a visible row, move its center into the bottom edge region, advance clock frames, and assert later rows become visible before releasing at a destination beyond the initially visible set. Repeat at the top edge after initially scrolling the list downward. Assert scrolling stops after release/cancellation.

**Implementation:**

While a compatible drag session has non-zero edge velocity, run a cancellable `LaunchedEffect` loop paced by `withFrameNanos`. Convert the approved 64 dp edge region and 900 dp/second maximum through `LocalDensity`, integrate velocity using frame elapsed time, and call `LazyListState.scrollBy`. Add the consumed scroll distance to the active row translation so the real row remains under the stationary finger, then rerun midpoint targeting against the newly visible bounds. End the loop when the pointer leaves the edge, the list cannot consume more scroll, or the session ends.

**Verify:** Run the focused connected `QueueEditorScreenTest`. Both edge directions reveal off-screen rows and stop cleanly.

---

### Task 8: Preserve accessibility and independent row actions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Extend the existing editor tests to invoke the handle's custom accessibility actions and assert:

- Move Up sends exactly `(occurrenceId, index - 1, currentOccurrenceId)`.
- Move Down sends exactly `(occurrenceId, index + 1, currentOccurrenceId)`.
- Boundary actions remain absent for the first/last upcoming positions.

Click Jump and Remove after exercising a drag and assert each invokes only its own callback. Click Back and Clear outside an active gesture and retain their existing behavior.

**Implementation:**

Keep the existing `CustomAccessibilityAction` labels and immediate one-position callback behavior on the handle. Ensure the pointer-input modifier is confined to the `[::]` handle, with the row, Jump, Remove, Back, and Clear excluded from drag initiation. Do not add whole-row long-press dragging or change any control labels.

**Verify:** Run the focused connected `QueueEditorScreenTest`. Accessibility and callback-separation coverage passes.

---

### Task 9: Add a deterministic lifted-row screenshot (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/QueueDraggedRowPreview_Queue dragged row_*.png`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Add a `@PreviewTest` named `Queue dragged row` that renders the internal `UpcomingQueueRow` with deterministic track data, `isDragging = true`, zero translation, and position 2 of 4. Run screenshot validation before generating a reference and confirm it reports the missing preview reference.

**Implementation:**

Keep the extracted row composable `internal` so the screenshot source set can render its active visual state without introducing a production-only preview flag on `QueueEditorScreen`. Generate the new reference, inspect it for the amber border, dark surface, lifted shadow, readable metadata, and unclipped 48 dp controls, and retain only the new approved reference. Do not regenerate or accept unrelated screenshots.

**Verify:** Run `./gradlew.bat updateDebugScreenshotTest`, inspect the new image, then run `./gradlew.bat validateDebugScreenshotTest`. All references pass.

---

### Task 10: Run regression and device checks (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueDragState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueDragStateTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

No new code in this task. Review the approved design's success criteria against the focused automated coverage before running the full suite; add no unrelated cleanup.

**Implementation:**

Run the complete verification set. On a Pixel 7 or equivalent emulator/device, manually drag one occurrence from near the top to the bottom of a queue longer than the viewport and back again. Confirm the row remains under the finger, neighbors shift smoothly, edge scroll begins and stops predictably, release performs one move, cancellation restores order, and duplicate tracks remain independently movable. Start a drag and advance playback to confirm the stale drag disappears without moving the wrong occurrence.

**Verify:**

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebugAndroidTest
./gradlew.bat connectedDebugAndroidTest
./gradlew.bat validateDebugScreenshotTest
./gradlew.bat assembleDebug
git diff --check
git status --short
```

All automated checks pass, manual drag behavior matches the design, and status shows only the approved design, plan, queue UI, focused tests, and new screenshot reference.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New behavior was developed test-first.
- [ ] A row follows the finger across multiple positions and surrounding rows preview the order.
- [ ] Edge auto-scroll works in both directions on a long queue.
- [ ] Drop emits exactly one occurrence-safe move; no-op and stale drags emit none.
- [ ] Existing accessibility Move Up/Down and row controls still work independently.
- [ ] Unit, connected Compose, screenshot, and build checks pass.
- [ ] No playback policy, persistence, dependency, or unrelated files were modified.
- [ ] The feature behaves exactly as described in the approved design document.
