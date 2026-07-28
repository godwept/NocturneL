# NocturneL Local Player Implementation Plan

**Date:** 2026-07-28  
**Design doc:** `docs/specs/2026-07-27-nocturnel-local-player-design.md`  
**Status:** Ready for review

## Overview

Build NocturneL as an offline Kotlin/Jetpack Compose Android application with application ID `ca.stewark.nocturnel`. It will target the user's Pixel 7 with `minSdk = 33`, use Android's Storage Access Framework for a user-selected music tree, persist only a local catalog, and use Media3 for playback and Android media controls. GitHub Actions will run the build/tests remotely and publish a debug APK artifact for installation on the physical phone.

## Tasks

### Task 1: Create the Gradle project shell

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `.gitignore`

**Test first:**

```text
No source exists yet. First add the Gradle wrapper verification command to CI in Task 3;
the first check is that `./gradlew.bat tasks` completes from the repository root.
```

**Implementation:**

- Create a single `:app` Kotlin Android application using package and namespace `ca.stewark.nocturnel`.
- Set `minSdk = 33`, `compileSdk = 36`, and `targetSdk = 36`.
- Require JDK 17, Kotlin, Jetpack Compose, Room, Media3, Coil, and AndroidX test dependencies through `gradle/libs.versions.toml`.
- Configure Compose, unit tests, instrumented tests, and a debug build type. Do not configure release signing.
- Ignore `.gradle/`, `build/`, `local.properties`, `*.jks`, and generated APK/AAB output.

**Verify:** Run `./gradlew.bat tasks` with JDK 17; the `app` project and Android test tasks are listed.

---

### Task 2: Add the Android application entry point

**Files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/src/main/java/ca/stewark/nocturnel/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/java/ca/stewark/nocturnel/MainActivityTest.kt`

**Test first:**

```kotlin
// MainActivityTest.kt
@Test fun application_uses_nocturnel_package_name() {
    assertEquals("ca.stewark.nocturnel", BuildConfig.APPLICATION_ID)
}
```

**Implementation:**

- Declare `NocturneLApplication`, a single exported launcher `MainActivity`, and the label `NocturneL`.
- Add only the permissions and components required at this point; defer the media playback service declaration to Task 20.
- Make `MainActivity` host the root Compose UI.

**Verify:** Run `./gradlew.bat testDebugUnitTest` and install the empty launcher activity on a device when available.

---

### Task 3: Add cloud build and test automation

**Files:** `.github/workflows/android.yml`, `README.md`

**Test first:**

```yaml
# The workflow must contain separate Gradle steps for:
# 1. testDebugUnitTest
# 2. assembleDebug
```

**Implementation:**

- Create a GitHub Actions workflow triggered by pushes and pull requests.
- Set up JDK 17, cache Gradle dependencies, run `./gradlew testDebugUnitTest`, and run `./gradlew assembleDebug`.
- Upload `app/build/outputs/apk/debug/app-debug.apk` as a retained workflow artifact named `nocturnel-debug-apk`.
- Document that the repository must be private, how to download the APK artifact, and how to enable developer installation on the Pixel 7. Do not add credentials or signing keys.

**Verify:** Push a branch and confirm the workflow succeeds and exposes the APK artifact.

---

### Task 4: Define catalog domain models

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/model/LibraryModels.kt`, `app/src/test/java/ca/stewark/nocturnel/library/model/LibraryModelsTest.kt`

**Test first:**

```kotlin
@Test fun track_statuses_distinguish_playable_missing_and_unsupported() {
    assertEquals(4, TrackStatus.entries.size)
}
```

**Implementation:**

- Define immutable domain types for `LibrarySource`, `Album`, `Track`, `Playlist`, `PlaylistEntry`, `ScanReport`, `ScanIssue`, and `TrackStatus`.
- Include document URI, root-relative path, metadata fallback fields, artwork source, and playback-relevant duration/track ordering fields as specified in the design.
- Represent a missing playlist entry without deleting its saved reference.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*LibraryModelsTest'`.

---

### Task 5: Define filename and folder metadata fallbacks

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/MetadataFallbacks.kt`, `app/src/test/java/ca/stewark/nocturnel/library/MetadataFallbacksTest.kt`

**Test first:**

```kotlin
@Test fun derives_track_number_and_title_from_numbered_filename() {
    assertEquals("Title", metadataFor("01 - Title.mp3").title)
}
```

**Implementation:**

- Parse common numbered filename forms such as `01 - Title.ext` and fall back to the filename stem without an extension.
- Derive album from the track's parent folder and artist from the parent-of-album folder only when embedded metadata is absent.
- Preserve embedded nonblank metadata; never overwrite it with a path-derived value.

**Verify:** Run the focused fallback unit test class.

---

### Task 6: Define supported-format policy

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/SupportedAudioFormats.kt`, `app/src/test/java/ca/stewark/nocturnel/library/SupportedAudioFormatsTest.kt`

**Test first:**

```kotlin
@Test fun recognizes_common_local_audio_extensions_case_insensitively() {
    assertTrue(isCandidateAudioFile("Track.MP3"))
    assertFalse(isCandidateAudioFile("cover.jpg"))
}
```

**Implementation:**

- Classify MP3, M4A/AAC, OGG/Opus, WAV, and FLAC as scan candidates.
- Keep the classification separate from actual decoder validation, so a candidate that fails metadata/decode probing becomes an `unsupported` or issue-report entry rather than a playable track.

**Verify:** Run the focused format-policy unit test class.

---

### Task 7: Persist the Room catalog schema

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`, `app/src/main/java/ca/stewark/nocturnel/data/entity/LibraryEntities.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/LibraryDao.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`

**Test first:**

```kotlin
@Test fun persists_tracks_and_retains_missing_playlist_entries() = runTest {
    // Insert a playlist entry for a missing track reference and assert it remains queryable.
}
```

**Implementation:**

- Add Room entities and DAOs for the selected library source, albums, tracks, playlists, playlist entries, scan reports/issues, manual artwork overrides, and playback preferences.
- Use root-relative track paths as the catalog identity used by playlists and rescan reconciliation; retain the document URI needed to open the current file.
- Add transactions needed to replace a completed scan atomically while preserving user playlists and manual cover overrides.

**Verify:** Run `./gradlew connectedDebugAndroidTest` on the Pixel 7 after installing the debug build.

---

### Task 8: Add repository scan reconciliation

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/main/java/ca/stewark/nocturnel/data/ScanReconciler.kt`, `app/src/test/java/ca/stewark/nocturnel/data/ScanReconcilerTest.kt`

**Test first:**

```kotlin
@Test fun marks_previously_known_unseen_tracks_missing_after_completed_scan() { }

@Test fun cancelled_scan_keeps_previous_catalog_intact() { }
```

**Implementation:**

- Reconcile a completed scan by adding/updating seen tracks and marking previously cataloged but unseen tracks as missing.
- Do not mutate the catalog on canceled or failed scans.
- Return count-based scan results for new, changed, missing, skipped, and unsupported entries.

**Verify:** Run the focused reconciliation unit test class.

---

### Task 9: Implement selected-folder access

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryTreeAccess.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryTreeAccessTest.kt`

**Test first:**

```kotlin
@Test fun revoked_tree_permission_maps_to_access_lost_state() { }
```

**Implementation:**

- Launch Android's `ACTION_OPEN_DOCUMENT_TREE`, request persisted read permission, and save only the returned tree URI/display name in the catalog.
- Revalidate persisted access before scans and playback; expose explicit `NoLibrary`, `Ready`, and `AccessLost` states.
- Do not request broad storage permissions or scan any location outside the selected tree.

**Verify:** Select a music folder on the Pixel 7, restart the app, and confirm the saved source remains available; revoke access in system settings and confirm the recovery state appears.

---

### Task 10: Traverse the selected tree on an explicit rescan

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/DocumentTreeWalker.kt`, `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/DocumentTreeWalkerTest.kt`

**Test first:**

```kotlin
@Test fun walks_nested_music_folders_and_ignores_non_audio_files() { }
```

**Implementation:**

- Recursively enumerate only descendants of the persisted tree URI using the Storage Access Framework.
- Produce root-relative paths, identify candidate files with `SupportedAudioFormats`, and cooperate with cancellation/progress reporting.
- Keep tree walking off the main thread and make it callable only by a user-triggered rescan action.

**Verify:** Use a fixture tree containing nested album folders and confirm every candidate is found once with the expected relative path.

---

### Task 11: Extract audio metadata and embedded artwork

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/AndroidMediaMetadataReader.kt`, `app/src/main/java/ca/stewark/nocturnel/library/TrackMetadataReader.kt`, `app/src/test/java/ca/stewark/nocturnel/library/TrackMetadataReaderTest.kt`

**Test first:**

```kotlin
@Test fun unreadable_metadata_returns_issue_without_crashing_scan() { }
```

**Implementation:**

- Read title, artist, album, year, track/disc number, duration, and embedded artwork from each candidate through Android media APIs.
- Combine usable values with `MetadataFallbacks`; return a structured issue for unreadable/corrupt files.
- Save embedded artwork only as a resolvable source/cached display payload, not by copying the audio file into the app database.

**Verify:** Scan one tagged track and one damaged/untagged fixture; confirm the tagged values and the fallback/issue behavior.

---

### Task 12: Resolve album artwork deterministically

**Files:** `app/src/main/java/ca/stewark/nocturnel/artwork/ArtworkResolver.kt`, `app/src/main/java/ca/stewark/nocturnel/artwork/TerminalArtworkPlaceholder.kt`, `app/src/test/java/ca/stewark/nocturnel/artwork/ArtworkResolverTest.kt`

**Test first:**

```kotlin
@Test fun artwork_precedence_is_manual_then_embedded_then_folder_cover_then_placeholder() { }
```

**Implementation:**

- Resolve manual assignment first, then embedded artwork, then sibling `cover.jpg`, `folder.jpg`, `albumart.jpg`, or `front.jpg` case-insensitively, then a generated terminal placeholder.
- Skip unreadable image sources and continue through the fallback chain.
- Make placeholder output stable for a given album so album grids do not visually change between launches.

**Verify:** Run the resolver tests and inspect four sample albums covering each fallback case.

---

### Task 13: Add manual album-cover assignment

**Files:** `app/src/main/java/ca/stewark/nocturnel/artwork/ManualArtworkRepository.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/album/AlbumArtworkViewModel.kt`, `app/src/test/java/ca/stewark/nocturnel/artwork/ManualArtworkRepositoryTest.kt`

**Test first:**

```kotlin
@Test fun manual_cover_override_wins_over_embedded_artwork() { }
```

**Implementation:**

- Launch Android's document picker for an image selected by the user, retain read permission when available, and persist its URI against the album.
- Offer clear/remove override actions; removal immediately returns artwork resolution to the normal fallback chain.

**Verify:** Assign a photo to an album, relaunch the app, then clear it and confirm the regular cover returns.

---

### Task 14: Complete the manual rescan flow

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/RescanUiState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModelTest.kt`

**Test first:**

```kotlin
@Test fun rescan_exposes_progress_then_completed_count_summary() = runTest { }

@Test fun cancelling_rescan_preserves_previous_catalog() = runTest { }
```

**Implementation:**

- Expose idle, running/progress, completed/report, canceled, and access-lost UI states.
- Wire the ViewModel to `LibraryScanner` and `CatalogRepository`; prevent concurrent scans.
- Keep scanning user-triggered; do not schedule monitoring/background jobs.

**Verify:** Start, cancel, and complete a scan on the Pixel 7; compare catalog and report states after each action.

---

### Task 15: Implement playlist domain operations

**Files:** `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistRepository.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistEditor.kt`, `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistEditorTest.kt`

**Test first:**

```kotlin
@Test fun adds_tracks_in_requested_order_and_keeps_missing_entries() { }

@Test fun removes_only_the_selected_playlist_entry() { }
```

**Implementation:**

- Create, rename, delete, reorder, add-to, and remove-from local playlists using ordered root-relative track references.
- Keep missing tracks visible in playlist data and let playback skip them later.

**Verify:** Run the focused playlist editor test class.

---

### Task 16: Implement `.m3u8` import parsing

**Files:** `app/src/main/java/ca/stewark/nocturnel/playlist/M3u8Codec.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/M3u8ImportService.kt`, `app/src/test/java/ca/stewark/nocturnel/playlist/M3u8CodecTest.kt`

**Test first:**

```kotlin
@Test fun imports_relative_entries_and_reports_absolute_or_unknown_entries() { }
```

**Implementation:**

- Parse UTF-8 `.m3u8` text, ignore comments/blank lines, normalize safe relative paths, and resolve them against the selected library root.
- Create a playlist from valid entries and return skipped-entry details for malformed, absolute, outside-root, or unknown paths.
- Use Android's document picker for selecting the import file.

**Verify:** Import a fixture with valid relative, missing, comment, and invalid lines; confirm the playlist and report match expectations.

---

### Task 17: Implement portable `.m3u8` export

**Files:** `app/src/main/java/ca/stewark/nocturnel/playlist/M3u8ExportService.kt`, `app/src/test/java/ca/stewark/nocturnel/playlist/M3u8ExportServiceTest.kt`

**Test first:**

```kotlin
@Test fun exports_utf8_relative_paths_in_playlist_order() { }
```

**Implementation:**

- Serialize a playlist as UTF-8 `.m3u8` using root-relative paths and the saved entry order.
- Include missing entries as their preserved relative path so moving the playlist with the restored library can resolve them later.
- Use Android's create-document flow; surface a cancellation/failure message without changing the playlist.

**Verify:** Export a playlist, re-import it, and confirm entry order/path preservation.

---

### Task 18: Define playback queue behavior

**Files:** `app/src/main/java/ca/stewark/nocturnel/playback/QueueController.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackModels.kt`, `app/src/test/java/ca/stewark/nocturnel/playback/QueueControllerTest.kt`

**Test first:**

```kotlin
@Test fun repeat_all_wraps_at_end_of_queue() { }

@Test fun next_skips_missing_or_unplayable_tracks() { }

@Test fun shuffle_keeps_current_track_until_advance() { }
```

**Implementation:**

- Implement pure queue operations for play-from-album/playlist/track, next, previous, seek target, shuffle, repeat-off/repeat-one/repeat-all, and skipping unavailable tracks.
- Keep queue state serializable for restoration.

**Verify:** Run the focused queue unit test class.

---

### Task 19: Define safe gapless eligibility

**Files:** `app/src/main/java/ca/stewark/nocturnel/playback/GaplessPolicy.kt`, `app/src/test/java/ca/stewark/nocturnel/playback/GaplessPolicyTest.kt`

**Test first:**

```kotlin
@Test fun unknown_or_absent_gapless_metadata_uses_normal_transition() { }

@Test fun confirmed_supported_metadata_allows_gapless_transition() { }
```

**Implementation:**

- Model the decision as opt-in: only a confirmed supported codec/metadata path is eligible.
- Never inspect audio silence or trim user audio to infer gaplessness.
- Expose the decision to the playback layer for device validation.

**Verify:** Run the policy unit test class and retain test tracks with known intended transitions for Task 21 device testing.

---

### Task 20: Add the Media3 playback service and media session

**Files:** `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/Media3PlaybackController.kt`, `app/src/main/AndroidManifest.xml`, `app/src/test/java/ca/stewark/nocturnel/playback/Media3PlaybackControllerTest.kt`

**Test first:**

```kotlin
@Test fun controller_translates_queue_commands_to_player_commands() { }
```

**Implementation:**

- Create a Media3 `MediaSessionService` backed by an ExoPlayer instance and document-URI media items.
- Route play/pause, seek, previous/next, queue metadata, shuffle, and repeat through the controller.
- Declare the foreground media-playback service and media-session intent handling required for Android notification, lock-screen, headset, and Bluetooth controls.
- Release player/session resources correctly and retain serializable playback state.

**Verify:** Install on the Pixel 7; start playback, lock the phone, and confirm notification/lock-screen previous, next, and pause controls work.

---

### Task 21: Handle audio focus, interruptions, and restore

**Files:** `app/src/main/java/ca/stewark/nocturnel/playback/AudioFocusHandler.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackStateRepository.kt`, `app/src/test/java/ca/stewark/nocturnel/playback/AudioFocusHandlerTest.kt`, `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackStateRepositoryTest.kt`

**Test first:**

```kotlin
@Test fun transient_focus_loss_pauses_and_gain_restores_only_if_user_was_playing() { }

@Test fun saved_state_restores_queue_position_and_repeat_mode() { }
```

**Implementation:**

- Apply Android audio focus rules for transient interruption, ducking, noisy headphone/Bluetooth route changes, and permanent focus loss.
- Persist queue, current track reference, position, shuffle/repeat state, and whether playback was active; restore safely only if access and track availability remain valid.

**Verify:** Test an interruption/route-change scenario and a full app-process restart on the Pixel 7.

---

### Task 22: Create the terminal design system

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/theme/Color.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Typography.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalFrame.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/theme/ThemeTest.kt`

**Test first:**

```kotlin
@Test fun reduced_motion_preference_disables_animated_terminal_effects() { }
```

**Implementation:**

- Define the phosphor-green-on-black palette, readable type scale, focus/pressed states, terminal frame/chrome, and optional restrained scanline/glow effects.
- Store a reduced-motion/effects preference and make effects absent when disabled.
- Favor contrast and touch target size over decorative CRT effects.

**Verify:** Run the focused theme test and inspect the root screen on the Pixel 7 in both effects states.

---

### Task 23: Build library setup and rescan screens

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySetupScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/RescanScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/LibrarySetupScreenTest.kt`

**Test first:**

```kotlin
@Test fun no_library_state_exposes_choose_music_folder_action() { }

@Test fun completed_rescan_displays_each_report_count() { }
```

**Implementation:**

- Present the initial chosen-folder action, current source name/access status, explicit rescan button, progress/cancel state, and completed scan report.
- Provide an access-lost recovery action and a clear explanation that only the chosen folder is scanned.

**Verify:** Run the connected Compose tests and manually select/reselect a folder on the Pixel 7.

---

### Task 24: Build the album-first library and search UI

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumGridScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumGridScreenTest.kt`

**Test first:**

```kotlin
@Test fun album_grid_opens_selected_album_and_displays_track_order() { }

@Test fun search_matches_album_artist_and_track_fallback_text() { }
```

**Implementation:**

- Show large resolved cover art in an album-first grid, with album details listing tracks in disc/track order.
- Add artist grouping, a track search that includes fallback metadata, and clear states for no music/unsupported files.
- Add album actions to play, shuffle, queue, and assign/clear cover art.

**Verify:** Run the connected Compose tests and manually browse a multi-album phone library.

---

### Task 25: Build playlist and queue screens

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/QueueScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreenTest.kt`

**Test first:**

```kotlin
@Test fun creates_playlist_adds_track_and_starts_playlist_queue() { }

@Test fun missing_playlist_entry_is_visible_and_not_play_actionable() { }
```

**Implementation:**

- Provide playlist creation/editing, track add/remove/reorder, `.m3u8` import/export actions, play/shuffle actions, and a current queue view.
- Visually retain but distinguish missing/unplayable entries; play actions skip them through `QueueController`.

**Verify:** Run the connected Compose tests, then complete an `.m3u8` export/import round trip on the Pixel 7.

---

### Task 26: Build the now-playing screen and app navigation

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLNavHost.kt`, `app/src/main/java/ca/stewark/nocturnel/MainActivity.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreenTest.kt`

**Test first:**

```kotlin
@Test fun now_playing_controls_dispatch_play_pause_seek_previous_and_next() { }
```

**Implementation:**

- Add the persistent navigation for albums, artists/search, playlists, queue, and settings.
- Render large artwork, title/artist/album, progress/seek, previous/play-pause/next, shuffle, repeat, and an explicit unavailable-track state.
- Connect UI controls only to the playback controller; do not access file APIs from composables.

**Verify:** Run the connected Compose test and manually control playback while switching screens and locking the device.

---

### Task 27: Add preferences and final offline/error UX

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/common/ErrorNotice.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsScreenTest.kt`

**Test first:**

```kotlin
@Test fun effects_toggle_persists_after_recreating_screen() { }

@Test fun access_lost_notice_offers_folder_reselection() { }
```

**Implementation:**

- Add effects/reduced-motion controls and library-source/rescan access from settings.
- Standardize clear error notices for revoked access, corrupt files, failed scans, failed document imports/exports, and unavailable playback items.
- Do not add network permissions, online artwork, streaming, or any out-of-scope feature.

**Verify:** Run connected Compose tests with airplane mode enabled and confirm all core library/playback flows remain available.

---

### Task 28: Perform release-candidate validation and document installation

**Files:** `README.md`, `docs/testing/pixel-7-release-checklist.md`

**Test first:**

```markdown
- [ ] Fresh install opens without a library.
- [ ] Chosen-folder permission survives restart.
- [ ] Explicit rescan produces a correct report.
- [ ] Playback controls work from notification and lock screen.
- [ ] M3U8 export/import round trip succeeds.
```

**Implementation:**

- Document GitHub Actions APK download/install steps and the Pixel 7 developer-installation prerequisite.
- Add the manual real-device checklist for folder permission recovery, scans, malformed media, cover fallbacks, playlists, offline operation, headset/Bluetooth controls, and known intended gapless transitions.
- Record the exact Android version and device used for the acceptance pass.

**Verify:** Complete the checklist from a fresh debug APK produced by GitHub Actions; attach/report any failures before declaring a release candidate ready.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] All unit tests pass through GitHub Actions (`./gradlew testDebugUnitTest`).
- [ ] All required connected tests pass on the Pixel 7 (`./gradlew connectedDebugAndroidTest`).
- [ ] GitHub Actions successfully publishes a debug APK artifact.
- [ ] The Pixel 7 manual acceptance checklist passes offline with a real selected music folder.
- [ ] No unplanned files are modified.
- [ ] NocturneL behaves as described in the approved design document.
