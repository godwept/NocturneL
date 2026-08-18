# Editable Playback Queue Implementation Plan

**Date:** 2026-08-18  
**Design doc:** `docs/specs/2026-08-18-editable-playback-queue-design.md`  
**Status:** Ready for review

## Overview

Add duplicate-safe Play Next and Add to Queue commands for tracks, albums, and playlists, backed by a pure queue-editing policy and applied to Media3 without replacing the current or historical portion of playback. Replace the read-only Up Next list with a dedicated terminal-styled queue editor supporting jump, remove, deterministic Undo, Clear Upcoming confirmation, drag reordering, and accessible Move Up/Down actions. Keep Media3 as the runtime source of truth, preserve the existing SharedPreferences snapshot format, and add no Room entities or dependencies.

## Tasks

### Task 1: Define duplicate-safe queue editing contracts (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Create `QueueEditingPolicyTest` with one contract test constructing two entries with the same `relativePath` and different `occurrenceId` values. Assert that `QueueSnapshot` retains both occurrences, the supplied `currentIndex`, `shuffle`, and `repeat`, and that `QueueUndoToken` retains an occurrence plus its upcoming index. Confirm the test fails because these contracts do not exist.

**Implementation:**

Define these Android-free contracts in `QueueEditingPolicy.kt`:

- `QueueEntry(occurrenceId, relativePath, title, artist, album, durationMs)`.
- `QueueSnapshot(entries, currentIndex, shuffle, repeat)` using the existing `RepeatMode`.
- A sealed `QueueEditCommand` with `InsertNext(entries)`, `Append(entries)`, `Move(occurrenceId, targetUpcomingIndex)`, `Remove(occurrenceId)`, `ClearUpcoming`, and `RestoreRemoved(token)`.
- `QueueUndoToken(entry, upcomingIndex)`.
- `QueueEditResult(snapshot, removed, changed, notice)`.

Do not place `TrackEntity`, `MediaItem`, Compose state, or Room types in this file.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. The contract test passes.

### Task 2: Implement Insert Next ordering (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests proving:

- A two-entry block is inserted directly after the current entry in its original order.
- A second Insert Next goes before the earlier inserted block.
- Current and historical entries remain byte-for-byte equal and at the same indices.
- Empty input produces `changed = false` and leaves the snapshot equal to the input.

Run the focused test and confirm the new cases fail.

**Implementation:**

Add `QueueEditingPolicy.apply(snapshot, command)`. For `InsertNext`, split at `currentIndex + 1`, insert the supplied entries between history/current and existing upcoming entries, preserve `currentIndex`, set shuffle to false when a change occurs, and report `SHUFFLE DISABLED · QUEUE UPDATED` only when shuffle was previously enabled.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. Insert Next tests pass.

### Task 3: Implement append and empty-queue behavior (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests that Append places a block after all upcoming entries, preserves block order and duplicates, and disables shuffle. Add empty-snapshot tests for both Insert Next and Append asserting the supplied entries become the complete queue while `currentIndex` remains `-1` so the playback layer can prepare without autoplaying.

**Implementation:**

Implement `Append` and normalize insertion into an empty snapshot without selecting a current entry. Do not deduplicate by path or occurrence identifier.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. Append and empty-queue tests pass.

### Task 4: Implement upcoming-only move rules (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests for moving an upcoming occurrence up and down by `targetUpcomingIndex`. Assert that duplicate paths are resolved by occurrence ID, target indices are clamped to the upcoming range, a current/history occurrence is rejected, an unknown occurrence is rejected, and no-op movement returns `changed = false`.

**Implementation:**

Implement `Move` against an upcoming-only copy, then join it back to the unchanged prefix through `currentIndex`. Disable shuffle only when the order actually changes. Return a stable stale-entry notice for rejected IDs rather than throwing.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. Move tests pass.

### Task 5: Implement removal and deterministic Undo tokens (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests asserting Remove deletes exactly the selected upcoming occurrence, returns its original upcoming index in `removed`, preserves duplicate siblings, and rejects current/history or unknown IDs. Add a test proving a successful removal disables shuffle.

**Implementation:**

Implement `Remove`. Populate `QueueUndoToken` only for a successful single-entry removal and use a notice containing the removed title. Do not retain an Undo stack in the pure policy.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. Removal tests pass.

### Task 6: Restore removed entries with clamping (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests that Restore Removed returns an entry to its former upcoming position, preserves its occurrence ID, clamps beyond the remaining upcoming range, and inserts immediately after the current track when playback has advanced beyond its former location.

**Implementation:**

Implement `RestoreRemoved` by clamping `token.upcomingIndex` to `0..upcoming.size`. Treat restoration as a manual mutation, disable shuffle when needed, and do not generate a second Undo token.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. Restore tests pass.

### Task 7: Clear upcoming and normalize repeat state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingPolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueEditingPolicy.kt`

**Test first:**

Add tests proving Clear Upcoming retains history and current, removes every later occurrence, disables shuffle, changes Repeat All to Off, preserves Repeat One, and is a no-op when no upcoming entries exist.

**Implementation:**

Implement `ClearUpcoming` with the approved repeat behavior. Set the notice to report both disabled modes when applicable. Do not create an Undo token for clear.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest'`. The complete policy suite passes.

### Task 8: Preserve the selected duplicate during snapshot restoration (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackStateRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackStateRepository.kt`

**Test first:**

Add a snapshot containing `same.flac`, another path, and `same.flac` again with `currentIndex = 2`. Assert restoration keeps both duplicate occurrences and maps the selected current occurrence to restored index 2 rather than the first matching path. Add a variant with an unavailable path before the selected duplicate and assert the remapped index is correct.

**Implementation:**

Change `PlaybackRestorePlanner` to retain original indexed occurrences while filtering availability. Derive the restored current index from the original index, not `indexOf(currentPath)`. Keep codec version 1 and its path-only persistence format unchanged.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*PlaybackStateRepositoryTest'`. Existing and duplicate restoration tests pass.

### Task 9: Define queue-editor presentation state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/QueueEditorStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorState.kt`

**Test first:**

Create tests for a pure mapper that converts current metadata plus upcoming `QueueEntry` values into `QueueEditorState`. Assert current is read-only, duplicate occurrences remain distinct, `canClear` follows upcoming emptiness, `canUndo` follows the supplied flag, and each row receives correct Move Up/Down availability.

**Implementation:**

Define `QueueEditorTrack`, `QueueEditorRow`, and `QueueEditorState`, plus `queueEditorState(...)`. Keep confirmation visibility and drag progress local to the composable; keep authoritative queue, notice, and Undo availability in the projected state.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditorStateTest'`. Mapper tests pass.

### Task 10: Add occurrence metadata to Media3 items (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`

**Test first:**

Create a source-wiring test asserting both MediaItem builders write `queue_occurrence_id`, `album_id`, and `duration_ms`, and that restored service items generate a fresh occurrence ID rather than deriving it from the relative path. Confirm it fails before the metadata exists.

**Implementation:**

Add shared internal metadata-key constants in `QueueEditingPolicy.kt`. Update both MediaItem builders to write a new UUID occurrence ID, album ID, and duration. Continue using `mediaId = relativePath` so the version-1 playback snapshot remains compatible.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'`. The wiring contract passes.

### Task 11: Project duplicate-safe Media3 queue state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Extend the wiring test to require `PlaybackQueueItem` to carry occurrence ID, album, duration, and absolute index, and `PlaybackUiState` to carry the current occurrence ID, queue notice, and Undo availability. Require `refresh` to read occurrence IDs from metadata extras rather than using path-based identity.

**Implementation:**

Expand the presentation models and `refresh`. Preserve the existing playback error while refreshing, and separately preserve queue notice/Undo state. Build the policy snapshot from the controller timeline using occurrence identity; never use `relativePath` as a Compose key or edit target.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'`. The projection wiring test passes and `testDebugUnitTest` compiles.

### Task 12: Add queued additions before controller connection (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueuePendingActionsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueuePendingActions.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Create an Android-free pending-action queue test proving multiple Play Next/Add requests retain call order, draining removes each request exactly once, and release clears all pending requests.

**Implementation:**

Define a small `PendingQueueActions` holder for addition requests only. Replace the single `pendingQueue` limitation for edit actions while keeping existing Play behavior. Drain pending additions after the controller connects and clear them in `release`.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueuePendingActionsTest'`. Pending-action tests pass.

### Task 13: Validate and describe queue additions (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueAdditionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/QueueAddition.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Create tests for `queueAddition(tracks, externallySkipped)` asserting playable tracks retain input order and duplicates, non-playable tracks increase skipped count, empty playable results return `NO PLAYABLE TRACKS`, and partial success reports queued and skipped counts.

**Implementation:**

Add an Android-free addition projection returning playable tracks, total skipped count, and exact terminal feedback. Use it from public `playNext(tracks, skippedCount = 0)` and `addToQueue(tracks, skippedCount = 0)` methods after the same selected-folder access check used by `playQueue`.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueAdditionTest'`. Addition validation tests pass.

### Task 14: Apply Insert Next and Append to Media3 (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Extend the wiring test to require Play Next/Add to Queue to call `QueueEditingPolicy`, resolve target entries by occurrence ID, and apply the resulting upcoming range with Media3 `replaceMediaItems`. Require the empty-queue branch to call `setMediaItems`, `prepare`, and set `playWhenReady = false` without calling `play()`.

**Implementation:**

Create MediaItems for validated additions, retain them in an occurrence-ID map for the policy application, and replace only indices after the current item. Apply the result's shuffle/repeat state before refreshing. For an empty player, install and prepare the queue paused. Store the addition feedback in queue notice.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 15: Persist timeline edits immediately (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`

**Test first:**

Add a wiring assertion that `Player.EVENT_TIMELINE_CHANGED` participates in the service's save condition. This must fail because queue additions/removals currently do not trigger an explicit snapshot save.

**Implementation:**

Include timeline changes in the existing event set that invokes `savePlaybackState()`. Do not add another timer, database store, or snapshot format.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'`. The persistence wiring test passes.

### Task 16: Wire jump and upcoming-only move commands (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Require public `jumpToQueueOccurrence` and `moveQueueOccurrence` methods to resolve the latest controller index by occurrence ID and reject any index at or before current. Require move to accept the current occurrence ID captured when dragging began and refresh with `QUEUE CHANGED · TRY AGAIN` if playback has advanced.

**Implementation:**

Implement jump with `seekToDefaultPosition(resolvedIndex)` without changing shuffle. Implement move through the pure policy and upcoming-range replacement. Clear any prior Undo before a successful move. Never trust a stale absolute index supplied by the UI.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 17: Wire remove and single-level Undo lifetime (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Require `removeQueueOccurrence`, `undoQueueRemoval`, and `expireQueueUndo` methods. Assert from source structure that the connection retains one removed MediaItem plus one policy token, a second successful queue action replaces/expires it, Undo clears it after one use, and no timer is introduced.

**Implementation:**

On removal, retain the exact removed MediaItem keyed by occurrence ID and expose `canUndoQueueRemoval = true`. Undo applies Restore Removed with the retained item. Expire Undo on any subsequent successful mutation and when `expireQueueUndo` is called on editor exit. Failed/stale operations must not discard an otherwise valid Undo.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 18: Wire Clear Upcoming (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/QueueEditingWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Require `clearUpcomingQueue()` to use the pure policy, replace only the upcoming range, apply shuffle/repeat changes, clear prior Undo, and leave current playback position/play state untouched.

**Implementation:**

Add the command and exact notice propagation. If no upcoming entries exist, do not mutate Media3 or clear a valid Undo; refresh and report the empty state.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 19: Build the static queue editor screen (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Create a Compose test rendering one current track and two upcoming duplicate-path occurrences. Assert a `[ BACK ]` command, read-only `CURRENT`, `UPCOMING`, distinct rows, `[::]` handles, Jump/Remove content descriptions, and `[ CLEAR UPCOMING ]` are displayed. Add an empty-state assertion for `QUEUE EMPTY` and a disabled/absent clear action.

**Implementation:**

Create `QueueEditorScreen(state, onBack, onJump, onMove, onRemove, onUndo, onClear, onExpireUndo)`. Render current separately in an `AsciiFrame`; render upcoming rows in a `LazyColumn` keyed by occurrence ID. Give every row and handle deterministic test tags based on occurrence ID.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. The new Compose test compiles.

### Task 20: Add accessible Move Up and Move Down actions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Use Compose semantics assertions/actions to prove the first upcoming row exposes Move Down but not Move Up, the last exposes Move Up but not Move Down, and invoking each action calls `onMove(occurrenceId, targetUpcomingIndex, capturedCurrentOccurrenceId)` with the expected target.

**Implementation:**

Attach `CustomAccessibilityAction` entries to the drag handle. Use the row's projected `canMoveUp/canMoveDown` flags and upcoming index. Keep the visible `[::]` handle at the minimum Android touch target.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. Accessibility test sources compile.

### Task 21: Add drag-to-reorder behavior (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Perform a vertical swipe on a tagged drag handle and assert it requests at least one adjacent move in the correct direction. Recompose with a changed current occurrence during a drag and assert no stale drop callback is emitted.

**Implementation:**

Use `pointerInput(occurrenceId, currentOccurrenceId)` and vertical drag detection only on `[::]`. Accumulate vertical distance and request one adjacent move whenever it crosses the measured row height; reset the accumulator after each request. Cancel automatically when the pointer input key changes because playback advanced. Do not add a reorder dependency.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. Drag test sources compile.

### Task 22: Add removal Undo and clear confirmation UI (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueEditorScreen.kt`

**Test first:**

Assert Remove calls the selected occurrence callback, `canUndo` renders `[ UNDO ]`, and Undo invokes exactly once. Assert Clear Upcoming first reveals `[ CONFIRM CLEAR ]` and `[ CANCEL ]`, Cancel closes the prompt, and confirmation invokes `onClear`. Assert Back invokes both `onExpireUndo` and `onBack`.

**Implementation:**

Keep clear-confirmation state local with `rememberSaveable`. Render queue notices with `TerminalNotice`. The Undo button remains present until the next successful queue action clears it or Back expires it; do not add a timeout.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. Confirmation and Undo tests compile.

### Task 23: Replace static Up Next with a Queue command (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingQueueTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Render Now Playing with two upcoming occurrences. Assert it shows `2 TRACK(S) UPCOMING`, exposes `[ QUEUE ]`, invokes `onOpenQueue`, and no longer renders every upcoming title. Add an empty case that still exposes Queue and reports `QUEUE EMPTY`.

**Implementation:**

Add `onOpenQueue` to `NowPlayingScreen`. Remove the read-only item loop and replace it with a compact summary plus Queue button beneath the playback frame.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. Now Playing queue tests compile.

### Task 24: Wire the queue editor as a Now Playing sub-screen (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/QueueEditingNavigationWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Create a source-wiring test requiring a `rememberSaveable` queue-editor flag, Back handling, `QueueEditorScreen`, all playback edit callbacks, and `expireQueueUndo` on close. Require changing top-level destination to close the sub-screen.

**Implementation:**

Open the editor only from Now Playing. While open, project `playbackState` with `queueEditorState` and render `QueueEditorScreen`; otherwise render `NowPlayingScreen`. Close and expire Undo on Back, Queue Back, or top-level navigation. Do not add a navigation tab or Navigation Compose route.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingNavigationWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 25: Add reusable individual-track queue controls (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/QueueTrackActionsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/QueueTrackActions.kt`

**Test first:**

Create a Compose test asserting visible `[ NXT ]` and `[ +Q ]` controls have content descriptions `Play <title> next` and `Add <title> to queue`, meet the existing minimum touch-target convention, and invoke separate callbacks.

**Implementation:**

Create `QueueTrackActions(title, onPlayNext, onAddToQueue, modifier)` using two `BracketIconButton` instances. Keep this component presentation-only and do not add overflow menus or long-press behavior.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. The component test compiles.

### Task 26: Add album and album-track queue actions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Add assertions that playable albums expose `[ PLAY NEXT ]` and `[ ADD QUEUE ]`, unavailable-only albums disable both, and each individual track exposes NXT/+Q callbacks carrying that track. Confirm existing Play, Shuffle, artwork, and playlist-picker tests remain unchanged.

**Implementation:**

Add album-level and track-level queue callbacks. Pass the original album track list to playback so it can report skipped unavailable entries; preserve disc/track order. Wire callbacks to `playback.playNext` and `playback.addToQueue` in `NocturneLApp`.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest` and `.\gradlew.bat testDebugUnitTest`. Both pass.

### Task 27: Add search-result track queue actions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/SearchScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Create a Search Compose test with one matching track. Assert tapping the result still plays it, while its NXT and +Q controls invoke the new callbacks without invoking Play. Keep album and artist result behavior unchanged.

**Implementation:**

Replace the track-only `ResultRow` rendering with a row containing the existing clickable label and `QueueTrackActions`. Add callbacks to `SearchScreen` and wire them in `NocturneLApp`.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. Search UI tests compile.

### Task 28: Retain playable track objects in playlist detail state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailState.kt`

**Test first:**

Add tests proving every available `PlaylistTrackRow` retains its matching `TrackEntity`, unavailable rows retain `null`, duplicate playlist entries may reference the same track object independently, and row order remains playlist order.

**Implementation:**

Add `track: TrackEntity?` to `PlaylistTrackRow`, populated from `allTracks.associateBy(relativePath)` only when status is playable. Do not remove the existing display fallbacks or availability flag.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*PlaylistDetailStateTest'`. Playlist detail state tests pass.

### Task 29: Add playlist and playlist-track queue actions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`

**Test first:**

Create a Compose test asserting playlist-level Play Next/Add Queue are enabled when any entry is playable, each available row has NXT/+Q controls, unavailable rows do not, and callbacks preserve playlist order. Assert the externally skipped count equals unavailable entry count.

**Implementation:**

Add playlist-level and track-level callbacks to `PlaylistDetailScreen`. In `PlaylistsScreen`, derive ordered playable tracks from `state.entries.mapNotNull { it.track }`, calculate unavailable count, and call playback queue methods with that skipped count. Preserve playlist editing controls and do not add queue actions to the playlist index.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest` and `.\gradlew.bat testDebugUnitTest`. Both pass.

### Task 30: Surface queue feedback through the shared scaffold (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/QueueEditingNavigationWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Extend the wiring test to require queue feedback to take precedence over scan status in `TerminalScaffold` while preserving scan status when no queue feedback exists. Require access loss, no-playable input, partial success, shuffle-disabled edits, stale edits, clear, and Media3 failures to assign concise queue notices.

**Implementation:**

Use `status = playbackState.queueNotice ?: viewModel.scanState.message`. Ensure refresh does not erase the latest notice. A later queue action replaces the previous notice; no timer or message history is added.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*QueueEditingNavigationWiringTest'` and `.\gradlew.bat assembleDebug`. Both pass.

### Task 31: Update previews and approve queue screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*`

**Test first:**

Add a Pixel 7-sized `QueueEditorPreview` with current, duplicate upcoming occurrences, Undo, and a queue notice. Update existing Album Detail, Search, Playlist Detail, and Now Playing preview call sites for new callbacks. Run screenshot validation and confirm it fails because the new queue reference is absent and affected references are stale.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest`, inspect the Queue Editor and every changed reference, and retain only changes caused by the approved queue UI. Verify readable terminal hierarchy, minimum touch targets, visible drag handles, and no clipping at 412x915 dp. Do not accept unrelated screenshot changes.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All references pass validation.

### Task 32: Add Pixel 7 queue acceptance checks (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Compare the checklist against the design and note that it does not cover queue insertion, duplicates, editing, shuffle/repeat normalization, Undo, restoration, or accessibility actions.

**Implementation:**

Add checklist items for:

- Play Next/Add Queue from a track, album, and partially unavailable playlist.
- Repeated Play Next block ordering and duplicate preservation.
- Jump, remove/Undo, drag, and accessibility Move Up/Down.
- Shuffle turning off after edits and Clear Upcoming turning Repeat All off.
- Editing across a track transition without corrupting the queue.
- App/service recreation restoring edited order and the selected duplicate occurrence.
- Notification controls remaining responsive after edits.

**Verify:** Review each new item against the approved design; every device-verification requirement has one checklist entry.

### Task 33: Run the focused automated verification gate (2–5 min)

**Files:** All files changed by Tasks 1–32

**Test first:**

Run focused suites before the full gate:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*QueueEditingPolicyTest' --tests '*PlaybackStateRepositoryTest' --tests '*QueueEditorStateTest' --tests '*QueueAdditionTest' --tests '*QueuePendingActionsTest' --tests '*QueueEditingWiringTest' --tests '*QueueEditingNavigationWiringTest'
.\gradlew.bat assembleDebugAndroidTest
```

Fix only failures caused by this feature. Do not weaken assertions or change unrelated production code.

**Implementation:**

Run the full repository gate:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat lintDebug assembleDebug
git diff --check
git status --short
```

Inspect `git status --short` and confirm every modified path is named by this plan. If an existing unrelated lint failure remains, report it separately rather than suppressing it or adding a baseline.

**Verify:** All automated commands pass, `git diff --check` is clean, and no unplanned file is modified.

### Task 34: Complete the Pixel 7 queue acceptance pass (2–5 min per checklist cluster)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Install the debug artifact and reproduce the pre-feature baseline: album/playlist playback, lock-screen controls, and queue restoration must work before exercising edits.

**Implementation:**

Execute the new queue checklist entries in clusters small enough to isolate failures: insertion/order, editor operations, shuffle/repeat behavior, transition races, restoration, accessibility, and external media controls. Record any device/codec-specific observation without changing scope.

**Verify:** Every new Pixel 7 queue item is checked, or a reproducible failure is recorded with the exact source action, queue state, and observed result.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every new production behavior was introduced after a failing behavior-focused test or contract check.
- [ ] Tracks, albums, and playlists expose Play Next and Add to Queue with ordered partial-success feedback.
- [ ] Repeated Play Next requests put the newest block first; Add to Queue appends; duplicates remain distinct.
- [ ] The dedicated queue editor supports jump, remove, one-level deterministic Undo, confirmed Clear Upcoming, drag, and accessible Move Up/Down.
- [ ] Current/history entries cannot be edited, stale drag operations fail safely, and empty-queue additions prepare without autoplay.
- [ ] Manual edits disable shuffle; Clear Upcoming disables Repeat All while preserving Repeat One.
- [ ] Edited queue order and the selected duplicate occurrence restore correctly after service recreation.
- [ ] Existing playback, seek, repeat, notification/headset controls, visualizers, playlists, and artwork behavior remain unchanged.
- [ ] No Room schema, new dependency, top-level navigation destination, timeout-based Undo, or out-of-scope feature was added.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Pixel 7 queue acceptance checks pass.
- [ ] `git diff --check` passes and no unplanned files are modified.
