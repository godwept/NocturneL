# Favorites and Listening History Implementation Plan

**Date:** 2026-08-18  
**Design doc:** `docs/specs/2026-08-18-favorites-listening-history-design.md`  
**Status:** Ready for review

## Overview

Add source-bound favorite albums and tracks, qualified local play history, lifetime track and derived album play counts, and a compact Resume experience to NocturneL. Store curated and listening data in normalized Room tables, retain only the newest 200 history events, extend the existing playback snapshot with stable occurrence progress, and let the Media3 service record actual listening time without counting seeks. Replace the Library album-only body with a terminal-styled landing grid that previews one Resume session, three favorite albums, three favorite tracks, and five distinct recent tracks before the complete album collection; provide nested full Favorites and History views, confirmations for destructive actions, and keep all data on-device.

## Tasks

### Task 1: Enable Room schema export while the database is still version 1 (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/data/RoomSchemaConfigurationTest.kt`, `app/build.gradle.kts`, `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`, `app/schemas/ca.stewark.nocturnel.data.NocturneLDatabase/1.json`

**Test first:**

Create `RoomSchemaConfigurationTest` as a source/configuration guard. Assert that `app/build.gradle.kts` configures `room.schemaLocation` to `app/schemas`, adds that directory to Android-test assets, and that `NocturneLDatabase` uses `exportSchema = true`. Run the focused test and confirm it fails against the current configuration.

**Implementation:**

Set `exportSchema = true` without changing database version 1. Add the KSP Room schema-location argument and `androidTest.assets.srcDir("$projectDir/schemas")` to `app/build.gradle.kts`. Run KSP/compilation once while the database is still version 1 and retain the generated `1.json` as the migration baseline. Do not hand-edit the generated schema.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*RoomSchemaConfigurationTest'` and `.\gradlew.bat compileDebugKotlin`. The guard passes and the version-1 schema exists.

### Task 2: Define normalized listening entities (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/data/entity/ListeningEntitiesTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/entity/ListeningEntities.kt`

**Test first:**

Add equality/default-value tests for `FavoriteTrackEntity`, `FavoriteAlbumEntity`, `TrackPlayStatsEntity`, and `PlayHistoryEntity`. Assert the history entity retains an auto-generated ID, stable `qualificationId`, relative path, and played timestamp. Confirm compilation fails before the entities exist.

**Implementation:**

Create the four Room entities exactly as approved:

- `favorite_tracks(relativePath TEXT PRIMARY KEY, favoritedAtEpochMillis INTEGER NOT NULL)`.
- `favorite_albums(albumId TEXT PRIMARY KEY, favoritedAtEpochMillis INTEGER NOT NULL)`.
- `track_play_stats(relativePath TEXT PRIMARY KEY, playCount INTEGER NOT NULL, lastPlayedAtEpochMillis INTEGER NOT NULL)`.
- `play_history(id INTEGER PRIMARY KEY AUTOINCREMENT, qualificationId TEXT NOT NULL, relativePath TEXT NOT NULL, playedAtEpochMillis INTEGER NOT NULL)`.

Add a unique Room index on `qualificationId`, plus lookup/order indexes on history `relativePath` and `playedAtEpochMillis`. Do not add foreign keys or activity columns to catalog entities.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningEntitiesTest'`. Entity tests pass.

### Task 3: Add database version 2 and its explicit migration (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseMigrationTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/Migrations.kt`, `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/schemas/ca.stewark.nocturnel.data.NocturneLDatabase/2.json`

**Test first:**

Use `MigrationTestHelper` and the exported version-1 schema to create a v1 database containing a library source, album, track, playlist, and playlist entry. Migrate to v2 and assert all old rows remain and each new table is queryable and empty. Confirm the test fails because `MIGRATION_1_2` and version 2 do not exist.

**Implementation:**

Add `MIGRATION_1_2` with explicit `CREATE TABLE` and `CREATE INDEX` statements matching Task 2. Register all four entities in `NocturneLDatabase`, bump the version to 2, expose `listeningDao()` once Task 4 supplies the DAO, and register the migration in `NocturneLApplication` with `.addMigrations(MIGRATION_1_2)`. Generate and retain `2.json`; do not use destructive migration fallback.

**Verify:** Run `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.data.NocturneLDatabaseMigrationTest`. Migration passes and existing data is preserved.

### Task 4: Implement independent favorite persistence (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/ListeningDaoTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/ListeningDao.kt`, `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`

**Test first:**

Create in-memory Room tests proving track and album favorites toggle independently, toggling an existing favorite removes it, and the observed favorite-ID flows update after each write. Use fixed timestamps and assert them. Confirm the tests fail before `ListeningDao` exists.

**Implementation:**

Define `ListeningDao` with observed favorite path/ID flows, existence queries, insert/delete operations, and `@Transaction` toggle methods. A toggle inserts with the supplied clock value when absent and deletes when present. Keep track and album operations separate.

**Verify:** Run the `ListeningDaoTest` instrumentation class. Favorite tests pass.

### Task 5: Record plays idempotently and cap history (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/ListeningDaoTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/ListeningDao.kt`

**Test first:**

Add tests proving `recordQualifiedPlay` inserts one history row and increments the matching stats row, a duplicate `qualificationId` changes neither history nor count, a later distinct qualification increments the count and last-played timestamp, and inserting 205 distinct events retains only the newest 200 while preserving lifetime count 205.

**Implementation:**

Use `@Insert(onConflict = IGNORE)` for history. In one `@Transaction`, increment/upsert stats and prune old rows only when the insert returns a real row ID. Prune with deterministic `ORDER BY playedAtEpochMillis DESC, id DESC LIMIT 200`. Never derive lifetime counts from the retained event table.

**Verify:** Run the `ListeningDaoTest` instrumentation class. Idempotency, aggregation, and retention tests pass.

### Task 6: Add favorite, count, history, and recent projections (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/ListeningDaoProjectionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/model/ListeningRows.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/ListeningDao.kt`

**Test first:**

Seed playable and missing catalog rows, independent favorites, repeated history, and stats. Assert:

- Favorite album/track projections include only playable items.
- Track-count projection returns lifetime counts.
- Album totals sum all track counts in that album.
- Full history is newest-first, preserves repeats, and retains missing rows with nullable catalog metadata.
- Recent preview returns five distinct playable paths ordered by each path's newest event.

**Implementation:**

Add immutable row models for track counts, album totals, and history items. Implement Room `Flow` queries using joins to existing catalog tables. Use a correlated newest-event query or grouped subquery with ID tie-breaking for deterministic recent deduplication. Do not delete or hide missing rows from full history; return their status/metadata as nullable so UI can disable them.

**Verify:** Run the `ListeningDaoProjectionTest` instrumentation class. All projection assertions pass.

### Task 7: Implement clearing boundaries and transactional source replacement (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/ListeningDao.kt`, `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`

**Test first:**

Extend `NocturneLDatabaseTest` to prove:

- `clearHistoryAndCounts` removes history/stats but retains both favorite tables.
- Re-selecting the same source preserves catalog and all listening data.
- Replacing the source clears catalog, all four listening tables, and keeps playlists.

Confirm failures before implementing the transaction boundary.

**Implementation:**

Add `ListeningDao.clearHistoryAndCounts()` and `clearAllListeningData()` transaction methods. Add a database-level `replaceLibrarySource(source, sourceChanged)` method using `withTransaction`: invoke the existing catalog source replacement and, only when `sourceChanged`, clear all listening tables in the same Room transaction. This is the strict database atomicity boundary.

**Verify:** Run the `NocturneLDatabaseTest` instrumentation class. Clearing and source-boundary tests pass.

### Task 8: Add the listening repository contract (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/data/ListeningRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/ListeningRepository.kt`

**Test first:**

Create a fake `ListeningStore` test double and assert repository methods forward fixed clock timestamps, expose the DAO projection flows unchanged, record the supplied qualification ID/path/time, and distinguish `clearHistoryAndCounts` from `clearAllListeningData`.

**Implementation:**

Define an injectable `ListeningStore` interface used by presentation and playback code, and a Room-backed `ListeningRepository` implementation. Expose favorite IDs, favorite entities, counts, album totals, full history, and recent preview. Inject a `nowMillis` function for deterministic tests. Keep playback/media types out of this layer.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningRepositoryTest'`. Repository tests pass.

### Task 9: Route source changes through the Room transaction before clearing Resume (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/data/SourceReplacementOrderTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`

**Test first:**

Add a small injected source-replacement coordinator test with recording fakes. Assert a changed source commits the database replacement before calling `PlaybackStateRepository.clear()`, a failed database replacement does not clear playback state, and a same-source selection does not clear playback state. Confirm the coordinator is absent.

**Implementation:**

Change `CatalogRepository` to use `NocturneLDatabase.replaceLibrarySource` and return whether the source changed. Add an injected coordinator used by `LibrarySourceViewModel` that clears `SharedPreferencesPlaybackStateRepository` only after a successful changed-source Room commit. Rely on restore-time source/path validation to reject the stale snapshot if the process dies in the small post-commit gap, as explicitly approved. Update application construction call sites; do not clear playlists.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*SourceReplacementOrderTest'` and the existing data tests.

### Task 10: Define playback-progress tracker contracts and thresholds (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackProgressTrackerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackProgressTracker.kt`

**Test first:**

Using a fake monotonic clock, add tests for a 100-second track qualifying at 50 seconds, a 10-minute track qualifying at four minutes, a 10-second track qualifying at five seconds, and boundary values one millisecond before/at the threshold. Confirm the tracker does not exist.

**Implementation:**

Create Android-free models for per-occurrence progress and a `PlaybackProgressTracker`. Calculate `min(durationMs / 2, 240_000)` when duration is positive. Accumulate monotonic elapsed time against a stable occurrence ID and emit a qualification containing occurrence ID and relative path exactly at the threshold.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*PlaybackProgressTrackerTest'`. Threshold tests pass.

### Task 11: Exclude pauses, buffering, and seeks from listening time (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackProgressTrackerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackProgressTracker.kt`

**Test first:**

Add tests that elapsed wall time while paused or buffering adds zero, resuming continues from accumulated time, a position jump adds no time, and changing occurrence IDs closes the old timing interval before starting the new one. Include a backward seek followed by more real listening.

**Implementation:**

Give the tracker explicit playing/tick/suspend/discontinuity/transition inputs. Accrue only the monotonic delta since the last active tick for the same occurrence. Reset the tick baseline after discontinuities instead of comparing player positions.

**Verify:** Run the focused tracker suite. Non-playing and seek cases pass.

### Task 12: Handle unknown durations, natural completion, and one-play-per-occurrence (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackProgressTrackerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackProgressTracker.kt`

**Test first:**

Add tests proving unknown duration qualifies at four minutes or natural completion, natural completion of a known-duration track does not bypass its actual-listening threshold, an already-qualified occurrence never emits again, revisiting it remains counted, and a new occurrence ID for the same path can qualify independently.

**Implementation:**

Track `qualified` per occurrence. Add a natural-completion input that emits early only for unknown duration. Provide immutable export/import of all begun occurrence states so service snapshots can persist partial and qualified state.

**Verify:** Run the focused tracker suite. Completion and idempotency tests pass.

### Task 13: Version the playback snapshot with occurrence progress (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackStateRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackStateRepository.kt`

**Test first:**

Add a v2 round-trip test containing duplicate paths with distinct occurrence IDs, partial accumulated listening, qualified flags, completion state, and a playback-session ID. Add a literal v1 encoded fixture and assert it still decodes with generated stable occurrence IDs, zero progress, not completed, and no prior session ID. Retain malformed-input tests.

**Implementation:**

Replace path-only snapshot entries with `PlaybackSnapshotEntry(relativePath, occurrenceId, accumulatedListeningMs, qualified)`. Increment the codec to version 2 and decode both versions. Persist `completed` and `playbackSessionId`. Bound entry counts and validate indices/times as the current codec does.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*PlaybackStateRepositoryTest'`. v1 compatibility and v2 round trips pass.

### Task 14: Restore exact occurrences and invalidate unavailable current tracks (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackStateRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackStateRepository.kt`

**Test first:**

Adapt existing duplicate tests to snapshot entries and assert occurrence IDs/progress survive filtering. Add cases for completed snapshots returning no Resume candidate, no playable rows returning null, a surviving current row preserving position, and a missing current row selecting the first survivor at 0 with its old progress reset.

**Implementation:**

Update `PlaybackRestorePlanner` to filter indexed entries by playable path while preserving occurrence identity. Remap the exact selected entry by original index. If it is gone, reset the chosen survivor's saved position and progress so it needs 10 seconds of new progress before Resume eligibility.

**Verify:** Run the focused playback-state suite. Restoration cases pass.

### Task 15: Distinguish a fresh process from active-session service recovery (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackRestorePolicyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackRestorePolicy.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`

**Test first:**

Test that `shouldAutoPlay` is true only when `wasPlaying` is true and the snapshot session ID equals the current in-memory application session ID. Assert legacy/null and different session IDs restore paused.

**Implementation:**

Add one random `playbackSessionId` created per `NocturneLApplication` instance and a pure restore policy. A fresh process gets a different ID and restores quietly; recreation of the service inside the same application process may continue playback.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*PlaybackRestorePolicyTest'`. Restore policy tests pass.

### Task 16: Wire stable occurrence state into restored Media3 items (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/ListeningPlaybackWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Add a source-wiring guard asserting both newly queued and restored `MediaItem`s carry `QUEUE_OCCURRENCE_ID` and duration metadata, restored items use the snapshot occurrence ID rather than a new UUID, and snapshot saves read occurrence IDs from item extras.

**Implementation:**

Overload/refactor `itemFor` so `PlaybackConnection` creates a UUID only for a genuinely new queue occurrence, while the service accepts the persisted occurrence ID during restoration. Save snapshot entries from the current Media3 timeline plus tracker state rather than saving only paths.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningPlaybackWiringTest' '*QueueEditingWiringTest'`. Wiring guards pass.

### Task 17: Drive the progress tracker from service playback events (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/ListeningPlaybackWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`

**Test first:**

Extend the wiring guard to require tracker handling for media transitions, `onIsPlayingChanged`, position discontinuities, playback-state changes, and a one-second active tick. Assert tracking remains in the service, not `PlaybackConnection` or a Composable.

**Implementation:**

Instantiate `PlaybackProgressTracker` in the service with `SystemClock.elapsedRealtime`. Feed it the current occurrence/path and runtime duration, falling back to `QUEUE_DURATION_MS`. Run a cancellable one-second tick only while playing, close intervals on pause/buffer/seek/transition, and import/export tracker state with snapshots.

**Verify:** Run the focused wiring guard and all tracker unit tests.

### Task 18: Persist qualifications safely and clear completed Resume state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/ListeningPlaybackWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`

**Test first:**

Require a service path that records emitted qualifications through `ListeningRepository`, prevents parallel retries for the same qualification ID, marks progress qualified only after success, and retries after failure. Require `Player.STATE_ENDED` to save `completed = true` and prevent restoration as Resume.

**Implementation:**

Maintain an in-flight qualification-ID set. Write on the IO service scope using occurrence ID as `qualificationId`; on success mark that occurrence qualified and save state, and on failure remove it from in-flight so a later tick can retry. On end-of-queue, finalize the tracker, persist any valid unknown-duration completion, then mark the snapshot completed. Preserve playback even if the local write fails.

**Verify:** Run the wiring guard, tracker tests, and `PlaybackStateRepositoryTest`.

### Task 19: Expose a pure Resume presentation state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/ResumeProjectionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningUiModels.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Test Resume projection boundaries: hidden at 9,999 ms, visible at 10,000 ms, hidden while playing, hidden when completed/no current track, disabled when source access is lost, and populated with title, artist, position, duration, and current path when valid.

**Implementation:**

Extend `PlaybackUiState` with the minimal completion/restoration fields required by the pure projection. Have `PlaybackConnection.refresh` copy Media3 playback state and queue presence. Define `ResumeUiState` and `resumeState(playback, sourceAccessible)`; do not read SharedPreferences from Compose.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ResumeProjectionTest'` and existing playback tests.

### Task 20: Define deterministic landing and full-view projections (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/ListeningProjectionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningUiModels.kt`

**Test first:**

Build oversized favorite/recent fixtures and assert the landing contains exactly three favorite albums, three favorite tracks, and five distinct recent tracks, retaining repository order. Assert full Favorites keeps all albums before all tracks, album totals default to zero, and history rows retain repeats and missing/disabled status.

**Implementation:**

Add Android-free `ListeningUiState`, `FavoriteAlbumUi`, `ListeningTrackUi`, and `HistoryUi` projections. Centralize the approved limits as private constants; do not expose a setting or add recommendations.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningProjectionTest'`. Projection tests pass.

### Task 21: Implement optimistic favorite state and failure rollback (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/OptimisticFavoritesTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/OptimisticFavorites.kt`

**Test first:**

Test immediate desired-state overlay for track and album toggles, removal of an overlay after repository confirmation, rollback after failure, independence of album and track keys, and a concise uppercase failure notice.

**Implementation:**

Create a pure optimistic overlay that combines persisted favorite-ID sets with pending desired values. It must support multiple distinct in-flight keys and return the previous persisted state on failure. Avoid a generic event/reducer framework.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*OptimisticFavoritesTest'`. Optimistic-state tests pass.

### Task 22: Build the listening ViewModel and injectable factory (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/ListeningViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningViewModel.kt`

**Test first:**

Use a fake `ListeningStore` and coroutine test dispatcher. Assert repository flows project into `ListeningUiState`, toggles update immediately then settle, a failed toggle rolls back and publishes a status message, and `clearHistoryAndCounts` preserves favorite state while reporting success/failure.

**Implementation:**

Create `ListeningViewModel(store)` plus an Android `Factory` that builds `ListeningRepository(application.database.listeningDao())`. Combine repository flows with the Task 20 projector and Task 21 overlay. Expose only toggle, clear-listening-data, and dismiss/consume-message actions needed by UI.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningViewModelTest'`. ViewModel tests pass.

### Task 23: Add reusable terminal favorite and counted-track controls (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/ListeningControlsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/FavoriteToggle.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/QueueTrackActions.kt`

**Test first:**

Compose-test selected/unselected favorite semantics, minimum touch target, content descriptions containing the item title, and callbacks. Extend the queue action test so a favorite control is independent from Play Next/Add Queue and displays a supplied play count.

**Implementation:**

Add a bracket-style `FAV` favorite control using existing terminal primitives. Extend `QueueTrackActions` with optional favorite state/callback and count text so existing call sites remain source-compatible until updated. Do not use Material `Button`, `Card`, or shaped controls directly.

**Verify:** Run the `ListeningControlsTest` instrumentation class and `TerminalUiSourceGuardTest`.

### Task 24: Render the Resume section on the Library landing (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Compose-test that eligible Resume appears first with track, artist, formatted position/duration, and a Resume callback; it is omitted when absent and disabled with an access-lost notice when inaccessible.

**Implementation:**

Create a `LazyVerticalGrid(GridCells.Fixed(2))` landing screen using full-span section items. Render a compact terminal `RESUME` frame only when the projection exists. Keep it above all favorites, recent items, and albums.

**Verify:** Run the `LibraryLandingScreenTest` instrumentation class. Resume cases pass.

### Task 25: Add the exact favorite previews and View All action (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Pass more than the preview limits and assert only three favorite albums and three favorite tracks render, album items precede track items, album/track selection callbacks are distinct, favorite toggles work, and one `VIEW ALL FAVORITES` callback is exposed. Assert the whole Favorites section is omitted when both lists are empty.

**Implementation:**

Add a full-span Favorites heading/action, compact album preview cards, and counted favorite-track rows using Task 23 controls. Use the already-limited projection rather than slicing again in Compose.

**Verify:** Run the landing Compose tests. Favorite preview cases pass.

### Task 26: Add five distinct recent rows and the complete album collection (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Assert exactly five projected recent tracks render with a `VIEW ALL HISTORY` callback, are omitted when empty, and appear before an `ALBUM LIBRARY` heading. Assert every catalog album still renders in the two-column grid and clicking an album works after scrolling.

**Implementation:**

Render recent rows as full-span items and catalog albums as the final two-column grid items in the same `LazyVerticalGrid`. Preserve the supplied `LazyGridState` so nested-view round trips restore the user's position. Avoid nested vertical scrolling containers.

**Verify:** Run the landing Compose tests and the adapted `LibraryScreenTest` scroll test.

### Task 27: Build the full Favorites nested view (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/FavoritesScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/FavoritesScreen.kt`

**Test first:**

Assert a Back action, all favorite albums before all favorite tracks, derived album totals, track counts, album/track selection, unfavorite callbacks, and an explicit empty notice when neither collection has entries.

**Implementation:**

Create a terminal-styled full Favorites screen with two labeled sections in one scrolling surface. Reuse album/track presentation components; do not add a tab, toggle, or bottom destination.

**Verify:** Run the `FavoritesScreenTest` instrumentation class.

### Task 28: Build the full newest-first History view (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningHistoryScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningHistoryScreen.kt`

**Test first:**

Assert Back, newest-first repeated events, timestamp and current lifetime count text, playable-row playback, favorite toggling, disabled missing rows with retained metadata/path fallback, and an empty-history notice.

**Implementation:**

Create one `LazyColumn` of retained history events. Format local timestamps deterministically through an injected/default formatter so tests use fixed labels. Missing rows remain visible but expose no playback click. Do not add per-event deletion.

**Verify:** Run the `ListeningHistoryScreenTest` instrumentation class.

### Task 29: Add favorite state and album totals to library album cards (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/LibraryScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumGridScreen.kt`

**Test first:**

Add a Compose test that an album card displays its total plays, exposes selected favorite semantics, toggles without opening detail, and still opens detail when its body is tapped.

**Implementation:**

Extend `AlbumGridScreen`/its card with count lookup, favorite-ID set, and callbacks, using defaults only where needed by previews/tests. Route the landing's catalog album rendering through the same card implementation to avoid duplicate visual behavior.

**Verify:** Run `LibraryScreenTest` and the landing tests.

### Task 30: Add favorites and counts to album detail track rows (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`

**Test first:**

Assert album total/favorite control, per-track counts/favorite controls, independent callbacks, and that favorite taps do not trigger playback or queue actions. Retain existing playable/missing and playlist behavior tests.

**Implementation:**

Add album favorite/count inputs and track count/favorite inputs. Place album favorite in the album frame and use the extended `QueueTrackActions` for playable track rows; missing tracks can be favorited/unfavorited only if already represented by the screen but remain unplayable.

**Verify:** Run `AlbumDetailScreenTest`.

### Task 31: Add favorites and counts to Search results (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/SearchScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt`

**Test first:**

Add tests for album and track favorite controls, album totals, track counts, selected semantics, and callback separation from opening/playing/queueing. Keep artist results unchanged.

**Implementation:**

Extend Search inputs with favorite sets, count maps, and toggle callbacks. Render favorite/count metadata only for album and track results; do not add favorite artists.

**Verify:** Run `SearchScreenTest`.

### Task 32: Add current-track favorite state and count to Now Playing (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingQueueTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Add a current track/path fixture and assert Now Playing displays its lifetime count, selected favorite control, and toggle callback without affecting play/pause or queue controls. Assert no favorite control when no track is selected.

**Implementation:**

Add current favorite/count inputs and a toggle callback. Place the terminal favorite control beside shuffle/repeat or track metadata without changing the visualizer and queue summary behavior.

**Verify:** Run `NowPlayingQueueTest` and existing visualizer Compose tests.

### Task 33: Add confirmed history/count clearing to Settings (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/ListeningSettingsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`

**Test first:**

Assert `CLEAR HISTORY + COUNTS` first enters inline confirmation, Cancel changes nothing, Confirm invokes the callback once, favorites are described as preserved, and the existing folder/rescan/effects actions remain usable.

**Implementation:**

Follow the existing Queue Editor inline confirmation pattern with `rememberSaveable`. Add callbacks/state for clearing and concise success/failure notice display supplied by `ListeningViewModel`. Do not add retention-limit settings or a clear-favorites action.

**Verify:** Run `ListeningSettingsTest` and the existing Settings cases in `LibraryScreenTest`.

### Task 34: Require confirmation before replacing a library source (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/SourceChangePolicyTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/LibrarySourceChangeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`

**Test first:**

Unit-test a pure policy: no current source selects immediately, same URI selects immediately, and a different URI becomes pending. Compose-test the pending Settings state: it warns that favorites/history/counts/Resume will be cleared, Cancel discards it, and Confirm invokes the selected-source callback once.

**Implementation:**

Split folder selection into `requestFolder`, `confirmSourceChange`, and `cancelSourceChange`. Retain the pending URI/display name only in ViewModel state. Persist URI permission and perform Task 9's ordered replacement only after confirmation. Initial setup and same-source reauthorization remain immediate.

**Verify:** Run `SourceChangePolicyTest` and `LibrarySourceChangeTest`.

### Task 35: Wire landing, nested views, actions, and status into the app root (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/ListeningAppWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Add a source-wiring guard requiring `ListeningViewModel.Factory`, `LibraryLandingScreen`, `FavoritesScreen`, `ListeningHistoryScreen`, favorite callbacks in Album Detail/Search/Now Playing, `playback::toggle` for Resume, and Settings clear/source-confirm callbacks. Require nested Library state to be saveable and Back to return to landing.

**Implementation:**

Collect listening state once in `NocturneLApp`. Replace `LibraryScreen`'s album-only body with `LibraryLandingScreen`, preserving its `LazyGridState`. Add saveable `LANDING`, `FAVORITES`, and `HISTORY` subview state under the Library destination; clear subview selection only when switching bottom destinations. Pass favorite/count maps everywhere, route recent/favorite track selection through `playback.play`, and merge listening failures into scaffold status without hiding queue/scan notices.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*ListeningAppWiringTest'` and compile Android tests.

### Task 36: Preserve Library scroll position across Favorites and History (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/LibraryScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Adapt the existing scroll-restoration harness to open Favorites and History from a scrolled landing, press Back, and assert the same `LazyGridState.firstVisibleItemIndex` is retained. Also retain the album-detail round-trip assertion.

**Implementation:**

Hoist one remembered `LazyGridState` for the landing and never recreate it when changing the nested Library view. Extend `BackHandler` priority so confirmations/pickers/queue editor remain first, then album/artist detail, then Favorites/History back to landing.

**Verify:** Run `LibraryScreenTest`.

### Task 37: Cover access loss, empty sections, and persistence failures (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningEdgeCasesTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/FavoritesScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningHistoryScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Compose-test that empty landing sections are omitted, full empty views show notices, lost access disables Resume/playback but retains displayed listening data, missing history is disabled, and an injected optimistic-write failure rolls the favorite indicator back while surfacing a concise status.

**Implementation:**

Finish conditional/disabled rendering and status propagation using existing `TerminalNotice` severity conventions. Do not delete data on access loss and do not stop current playback solely because a favorite/history write fails.

**Verify:** Run `ListeningEdgeCasesTest`, `TerminalUiSourceGuardTest`, and focused listening ViewModel tests.

### Task 38: Add deterministic fixtures and screenshot references (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/UiFixtures.kt`, `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, generated references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/`

**Test first:**

Add Pixel 7-sized populated Landing, Favorites, History, clear-confirmation, and source-change-confirmation previews using fixed timestamps/counts and no runtime data. Run screenshot validation and confirm new/mutated previews fail because references are absent or stale.

**Implementation:**

Extend shared fixtures with deterministic listening UI rows. Update affected Album Grid, Root, Album Detail, Search, Now Playing, and Settings preview signatures. Run `updateDebugScreenshotTest`, inspect every changed image for terminal hierarchy, exact 3/3/5 landing limits, readable counts, minimum touch targets, and clipping; retain only approved reference changes.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All screenshot references pass.

### Task 39: Run the complete quality gate and inspect scope (2–5 min)

**Files:** All files changed by Tasks 1–38; no additional files unless a failing approved test requires a targeted correction.

**Test first:**

Run the full checks before making cleanup changes:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

If an emulator/device is available, also run `.\gradlew.bat connectedDebugAndroidTest`. Record any failure and correct only the owning planned task; do not weaken assertions.

**Implementation:**

Inspect `git status --short` and `git diff --check`. Confirm generated Room schemas are versions 1 and 2 only, migration is registered, history retention is 200, landing limits are 3/3/5, no network/account dependency was added, and no unrelated files changed. Review source-change order: Room commit first, SharedPreferences clear second, restore-time validation as crash-gap protection.

**Verify:** Re-run every previously failing command. All applicable checks pass and the diff contains only this feature, its approved design/plan, tests, schemas, and screenshot references.

## Definition of Done

- [ ] All tasks completed in order using test-first development.
- [ ] Favorite albums and tracks remain independent and work from every approved context.
- [ ] Qualified plays use actual listening time, are idempotent per occurrence, and never count seeks.
- [ ] Full history retains the newest 200 events while lifetime counts remain complete.
- [ ] Album totals equal the sum of track counts.
- [ ] Resume appears only for a paused, accessible, incomplete queue with at least 10 seconds of meaningful progress.
- [ ] Fresh-process restoration is paused; same-process service recovery may continue playback.
- [ ] Same-source rescans preserve listening data; confirmed source replacement clears source-bound data in the approved order.
- [ ] Settings clearing removes history/counts but preserves favorites and Resume.
- [ ] Landing previews contain at most 3 favorite albums, 3 favorite tracks, and 5 distinct recent tracks.
- [ ] Full Favorites and History views, Back behavior, empty states, missing items, and access-loss states match the design.
- [ ] Room v1-to-v2 migration preserves the existing catalog and playlists.
- [ ] Unit tests, Android-test assembly/instrumentation, screenshot validation, lint, and debug assembly pass.
- [ ] No unplanned files or online/sync capabilities were added.
- [ ] Feature behaves exactly as described in the approved design document.
