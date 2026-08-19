# Queue-Only Single-Line Track Rows Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-queue-only-single-line-track-rows-design.md`  
**Status:** Ready for review

## Overview

Remove all user-facing per-track and collection-level play-next actions, make append-to-queue the sole queue insertion path, and remove the now-dead insert-next playback policy. Reorganize Album Detail and Playlist Detail controls into the approved rows, then constrain every track title or track metadata line to one line with an ellipsis while preserving full semantic text and fixed control touch targets. The work must retain all existing uncommitted visualizer/ring-removal changes and layer only the approved track-list changes onto shared screenshot sources and references.

## Tasks

### Task 1: Make shared track queue actions append-only (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/QueueTrackActionsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/QueueTrackActions.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

Replace `actionsHaveDistinctAccessibleCallbacks` with an append-only contract:

```kotlin
@Test fun exposesOnlyTheAppendQueueAction() {
    var appended = 0
    compose.setContent {
        NocturneLTheme { QueueTrackActions("Carrier", { appended++ }) }
    }

    compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
    compose.onNodeWithText("[ NXT ]").assertDoesNotExist()
    compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
    assertEquals(1, appended)
}
```

Run the focused Android test before implementation; it fails to compile against the old two-callback signature or fails the NXT absence assertions.

**Implementation:**

- Remove `onPlayNext` from `QueueTrackActions` and delete its `BracketIconButton("NXT", ...)`.
- Keep `[ +Q ]`, optional play count/favorite controls, and their accessibility descriptions unchanged.
- Update all three call sites to pass only the append callback plus existing named optional arguments:
  - Album Detail: `QueueTrackActions(track.title, { onAddTrackToQueue(track) })`
  - Search: append callback plus favorite arguments.
  - Playlist Detail: append callback plus play-count/favorite arguments.
- Leave screen-level play-next parameters temporarily intact for compilation; later tasks remove each contract together with its caller wiring.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device available, run `connectedDebugAndroidTest` for `QueueTrackActionsTest`; only `[ +Q ]` exists and invokes append exactly once.

### Task 2: Simplify Album Detail controls and title rows (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Update `albumAndTrackQueueActionsUsePlayableTracks` to capture album append and track append callbacks, click `[ ADD QUEUE ]` and the first track's `Add Carrier to queue` action, and assert the correct playable tracks are supplied. Assert `[ PLAY NEXT ]` and the first track's play-next accessibility action do not exist.

Add an action-row position check by fetching the bounds of `[ BACK ]`, `[ PLAY ]`, `[ SHUFFLE ]`, and `[ ADD QUEUE ]` and asserting their `top` values differ by no more than one pixel.

Add a long-title case using a fixed-width root and a copy of the first track with a unique repeated title. Collect its `TextLayoutResult` with `SemanticsActions.GetTextLayoutResult`:

```kotlin
val layouts = mutableListOf<TextLayoutResult>()
compose.onNodeWithText(longTitle).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
    action(layouts)
}
assertEquals(1, layouts.single().lineCount)
assertTrue(layouts.single().hasVisualOverflow)
```

Also confirm `onNodeWithText(longTitle)` still finds the complete semantic text.

**Implementation:**

- Put `BACK`, `PLAY`, `SHUFFLE`, and `ADD QUEUE` in the first existing row, in that order.
- Delete the second action row and `PLAY NEXT` button.
- Remove `onPlayAlbumNext` and `onPlayTrackNext` from `AlbumDetailScreen`.
- Remove their two arguments from the Album Detail call in `NocturneLApp.kt`.
- Add `maxLines = 1` and `overflow = TextOverflow.Ellipsis` to each track-title `Text`; retain its `weight(1f)` so number, count, duration, favorite, and `[ +Q ]` remain fixed.
- Preserve empty/playable enablement and all non-queue album controls.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device, run the focused `AlbumDetailScreenTest`; the controls share one line, append callbacks work, play-next is absent, and the long title reports one overflowing line.

### Task 3: Simplify Search track actions and constrain track results (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/SearchScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Change `trackResultSeparatesPlayAndQueueActions` to remove the next counter/callback, assert the play-next content description is absent, click `[ +Q ]`, and then click the weighted track result to prove queueing does not trigger playback and row playback does not trigger queueing.

Add a unique long track title in a fixed-width Search screen, query it, collect the combined `artist :: title · play count` node's `TextLayoutResult`, and assert `lineCount == 1` and `hasVisualOverflow`. Confirm the complete combined text remains the semantic text.

**Implementation:**

- Remove `onPlayNext` from `SearchScreen` and from its call in `NocturneLApp.kt`.
- Update the track `QueueTrackActions` call to append only.
- Extend private `ResultRow` with optional `maxLines` and `overflow` parameters that preserve existing defaults for album and artist results.
- Pass `maxLines = 1` and `TextOverflow.Ellipsis` only for track results, keeping their weighted width before favorite and `[ +Q ]` controls.
- Do not change search matching, album results, artist results, or favorite behavior.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device, run `SearchScreenTest`; queue/play actions stay independent and the combined track result is a single ellipsized line.

### Task 4: Reorganize Playlist Detail and constrain its track text (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`

**Test first:**

Update `playlistAndAvailableRowsExposeQueueActions` for the new callback order and assert:

- `[ PLAY NEXT ]` and `Play Carrier next` do not exist.
- `[ ADD QUEUE ]` passes only playable tracks plus the missing count.
- Per-track `[ +Q ]` exists only for the playable row.
- `[ BACK ]` has a smaller `top` bound than the action controls.
- `[ PLAY ]`, `[ RENAME ]`, `[ ADD TRACK ]`, and `[ ADD QUEUE ]` have equal `top` bounds within one pixel.

Add fixed-width long-title coverage for both an available-track result and a playlist entry. Collect `TextLayoutResult` for the available combined artist/title line, entry title line, and entry artist/duration line; assert each has one line and the long title-bearing lines report visual overflow.

**Implementation:**

- Move `BACK` outside `AsciiFrame` into its own top `Row`.
- Keep the playlist name field inside the frame.
- Put `PLAY`, `RENAME`, `ADD TRACK`/`CLOSE ADD`, and `ADD QUEUE` in one row inside the frame.
- Delete the `PLAY NEXT` row/button.
- Remove `onPlayNext` and `onPlayTrackNext` from `PlaylistDetailScreen`.
- Remove both play-next lambdas from `PlaylistsScreen` and retain only collection and per-track `addToQueue` lambdas.
- Add single-line ellipsis to available-track combined text, playlist entry title, and artist/duration text. Keep the text column weighted so reorder, remove, favorite, and queue controls retain space.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device, run `PlaylistDetailScreenTest`; layout, playable filtering, append callbacks, and all one-line assertions pass.

### Task 5: Remove the dead insert-next playback pipeline (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Replace the play-next-positive wiring assertions with a source-removal contract:

```kotlin
assertFalse("fun playNext" in connection)
assertFalse("QueueAddMode.NEXT" in connection)
assertFalse("InsertNext" in policy)
assertTrue("fun addToQueue" in connection)
assertTrue("QueueEditCommand.Append" in connection)
```

Keep positive assertions for Queue Editor wiring and `addToQueue` calls in `NocturneLApp.kt` and `PlaylistsScreen.kt`. Run the focused unit test before implementation and confirm it fails on the current insert-next symbols.

In `QueueEditingPolicyTest`, remove only insert-next ordering and empty-insert cases. Preserve and, if needed, rename the append test so it still checks order, duplicates, empty-queue current index, and empty append as a no-op.

**Implementation:**

- Delete `QueueEditCommand.InsertNext` and its `when` branch.
- Replace the boolean `insert(..., afterUpcoming)` helper with append-only logic that returns `snapshot.entries + additions` and preserves the empty-addition no-op.
- Delete `PlaybackConnection.playNext`.
- Remove `QueueAddMode`, the mode field from `PendingQueueAction`, and the mode parameter from `enqueue`.
- Make `addToQueue` call append-only `enqueue`; make pending action application always use `QueueEditCommand.Append`.
- Preserve playable filtering, skipped notices, pending-controller behavior, occurrence IDs, queue notices, shuffle normalization, and media-item application.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest' --tests '*QueueEditingWiringTest'`. Both pass and `rg -n 'playNext|InsertNext|QueueAddMode.NEXT|PLAY NEXT|"NXT"' app/src/main` returns no matches.

### Task 6: Constrain listening and resume track text (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningScreensTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningRows.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Add a fixed-width test using a unique long title and supporting text in `ListeningTrackRow`. Collect the title and combined metadata `TextLayoutResult` values and assert each has one line, the title overflows, and the complete title remains discoverable by `onNodeWithText`.

Extend the landing test with a long resume title and assert its text layout is also one line with visual overflow.

**Implementation:**

- Add `maxLines = 1` and `TextOverflow.Ellipsis` to `ListeningTrackRow` title and combined artist/play-count/supporting text.
- Add the same constraint to the resume title and supporting artist/progress line in `LibraryLandingScreen`.
- Retain the weighted text column, favorite control, enabled click behavior, section ordering, and listening data.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device, run `ListeningScreensTest`; title and metadata lines do not wrap and existing ordering/empty-state tests still pass.

### Task 7: Constrain Queue Editor track text (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Add fixed-width Queue Editor state containing unique long current and upcoming titles, artists, and album metadata. Collect `TextLayoutResult` for current title, current artist/album, upcoming title, and upcoming artist/duration. Assert each has one line, long lines visually overflow, and full titles still appear in jump/remove/reorder content descriptions.

**Implementation:**

- Add single-line ellipsis to the current title and current artist/album line.
- Add single-line ellipsis to `UpcomingQueueRow` title and artist/duration line.
- Keep the current frame, weighted upcoming text column, drag handle, jump/remove buttons, drag semantics, and callbacks unchanged.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device, run `QueueEditorScreenTest`; new text-layout checks and all drag/reorder/accessibility tests pass.

### Task 8: Update deterministic previews and screenshot references (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/` for Album Detail, Playlist Detail, Search, Favorites, Library Landing/Listening History if previewed, and Queue Editor

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest` after Tasks 1–7. It must fail only for references affected by removed controls or deliberately lengthened track fixtures.

**Implementation:**

- Give each affected preview a local copy of its track/row fixture with a deterministic title long enough to overflow at 412dp; do not change the shared fixture globally because that would alter unrelated previews.
- Keep Album Detail, Search, Playlist Detail, Favorites/listening, and Queue Editor preview dimensions at their existing widths.
- Ensure Album Detail shows the one-line four-button action row and Playlist Detail shows standalone BACK plus the one-line four-button action row.
- Run `.\gradlew.bat updateDebugScreenshotTest`, inspect every changed image, and retain only approved control removal/layout and ellipsis changes.
- The screenshot source and references already contain uncommitted visualizer/ring-removal work; preserve those edits exactly and do not regenerate or restore unrelated visualizer references.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. At 412dp, long track text ends in ellipsis, no track text wraps, all fixed controls remain visible, touch targets are intact, and no unrelated reference changes remain.

### Task 9: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–8

**Test first:**

Run removal and presentation source checks:

```powershell
rg -n 'playNext|InsertNext|QueueAddMode\.NEXT|PLAY NEXT|"NXT"' app/src/main
rg -n 'QueueTrackActions\(' app/src/main
```

The first command returns no matches. Inspect every `QueueTrackActions` call and confirm it supplies only append plus optional count/favorite arguments.

**Implementation:**

Fix only failures caused by queue-only controls and single-line track text. Do not change append order, Queue Editor reorder/undo mechanics, track search behavior, playback selection, database state, album/artist headings, Now Playing marquee, shared button touch targets, visualizer behavior, or dependencies.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
git status --short
```

If a device or emulator is attached, also run `.\gradlew.bat connectedDebugAndroidTest`. Confirm the final diff contains only this approved feature, its design/plan/tests/references, and the pre-existing visualizer/ring-removal work. Report connected tests as pending when no device is available.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Tests were written and observed failing before each behavior change, or device-dependent red execution was explicitly reported pending.
- [ ] No user-facing per-track `[ NXT ]` or collection-level `[ PLAY NEXT ]` action remains.
- [ ] `PlaybackConnection.playNext`, `QueueAddMode.NEXT`, and `QueueEditCommand.InsertNext` are absent.
- [ ] `[ +Q ]` and `[ ADD QUEUE ]` append playable tracks in their existing order and retain skipped-track reporting.
- [ ] Album Detail and Playlist Detail controls match the approved one-line layouts.
- [ ] Every scoped track title and metadata region renders one line with ellipsis under constrained width.
- [ ] Complete titles remain in semantic text and action content descriptions.
- [ ] Favorite, play, shuffle, append, reorder, jump, remove, undo, missing-track, search, and playlist editing behavior remains intact.
- [ ] Updated deterministic screenshots show no wrapping, clipping, lost controls, or unrelated changes at 412dp.
- [ ] Pre-existing visualizer/ring-removal changes remain intact.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected tests pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
