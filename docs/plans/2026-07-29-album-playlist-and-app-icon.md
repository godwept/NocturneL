# Album-to-Playlist and App Icon Implementation Plan

**Date:** 2026-07-29  
**Design doc:** `docs/specs/2026-07-29-album-playlist-and-app-icon-design.md`  
**Status:** Ready for review

## Overview

Add an inline single-playlist picker to album detail, append only missing playable album tracks in display order, support create-and-add when no playlists exist, and provide terminal-styled result feedback. Generate a sharp CRT/pixel-`N` launcher mark, package it as Android adaptive and legacy launcher resources, and extend unit, Compose, screenshot, lint, and build coverage without changing the Room schema or playlist file format.

## Tasks

### Task 1: Define bulk append behavior in the pure playlist editor (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistEditor.kt`, `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistEditorTest.kt`

**Test first:**

Add tests for a new `PlaylistEditor.appendDistinct(existing, candidates)` result:

```kotlin
@Test
fun appendsOnlyMissingTracksInAlbumOrder() {
    val result = PlaylistEditor.appendDistinct(
        existing = listOf("old.flac", "02.flac"),
        candidates = listOf("01.flac", "02.flac", "03.flac"),
    )

    assertEquals(listOf("old.flac", "02.flac", "01.flac", "03.flac"), result.paths)
    assertEquals(2, result.added)
    assertEquals(1, result.skipped)
}
```

Also test complete overlap, empty candidates, and repeated candidate paths. Confirm the focused test fails because the API is absent.

**Implementation:**

Add `AppendDistinctResult(paths: List<String>, added: Int, skipped: Int)` beside `PlaylistEditor`. Implement `appendDistinct` by preserving `existing`, tracking membership with a mutable set, and accepting each candidate only once in encounter order. Set `skipped` to `candidates.size - added`.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.playlist.PlaylistEditorTest
```

### Task 2: Add the repository bulk append result (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistRepository.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`

**Test first:**

Extend `NocturneLDatabaseTest.kt` with an in-memory database test that:

1. Creates a playlist and seeds one existing entry.
2. Calls `PlaylistRepository.appendAlbum(playlistId, listOf("01.flac", existingPath, "03.flac"))`.
3. Asserts the stored entry order is existing entries followed by `01.flac`, then `03.flac`.
4. Asserts `added == 2` and `skipped == 1`.

Add a second test that calls the method with a nonexistent playlist ID and expects `PlaylistNotFoundException`.

**Implementation:**

In `PlaylistRepository.kt`:

- Add `data class AppendAlbumResult(val added: Int, val skipped: Int)`.
- Add `class PlaylistNotFoundException(playlistId: Long)`.
- Add `suspend fun appendAlbum(playlistId: Long, orderedPaths: List<String>): AppendAlbumResult`.
- Check `dao.playlist(playlistId)` before reading entries.
- Delegate ordering/deduplication to `PlaylistEditor.appendDistinct`.
- Call `replaceEntries` once only when `added > 0`.

Do not modify entities, database version, or migrations.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run the focused database test on an attached device when available.

### Task 3: Define album-to-playlist presentation states (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playlist/AlbumPlaylistUiState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playlist/AlbumPlaylistUiStateTest.kt`

**Test first:**

Create tests asserting:

- `AppendAlbumResult(3, 0)` maps to success text `ADDED 3 TRACK(S) TO <name>`.
- `AppendAlbumResult(2, 1)` includes both added and skipped counts.
- `AppendAlbumResult(0, 4)` maps to `ALBUM ALREADY IN PLAYLIST`.
- `PlaylistNotFoundException` maps to a warning.
- Other failures map to a red error state.

**Implementation:**

Create:

```kotlin
sealed interface AlbumPlaylistUiState {
    data object Idle : AlbumPlaylistUiState
    data object Working : AlbumPlaylistUiState
    data class Success(val message: String) : AlbumPlaylistUiState
    data class AlreadyPresent(val message: String) : AlbumPlaylistUiState
    data class Warning(val message: String) : AlbumPlaylistUiState
    data class Error(val message: String) : AlbumPlaylistUiState
}
```

Add pure mapping helpers from repository result/failure to the state. Keep user-facing strings uppercase and omit the `::` prefix because `TerminalNotice` supplies it.

**Verify:** Run the focused `AlbumPlaylistUiStateTest`.

### Task 4: Add album append commands to the ViewModel (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModel.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playlist/AlbumPlaylistCommandTest.kt`

**Test first:**

Create a coroutine test around a small internal `AlbumPlaylistCommand` class that accepts suspend lambdas for `appendAlbum` and `createPlaylist`. Verify:

- Only tracks with `status == "PLAYABLE"` are forwarded.
- Forwarded paths retain input order.
- Existing-playlist success returns the mapped success state.
- Repository failure returns warning/error state.

This command object keeps the business flow JVM-testable without constructing `AndroidViewModel`.

**Implementation:**

Create the internal command class in `PlaylistViewModel.kt`, then add:

- `_albumPlaylistState = MutableStateFlow<AlbumPlaylistUiState>(Idle)`.
- `val albumPlaylistState: StateFlow<AlbumPlaylistUiState>`.
- `fun addAlbum(playlistId: Long, playlistName: String, tracks: List<TrackEntity>)`.
- `fun clearAlbumPlaylistState()`.

Set `Working` before executing and map the final result through the pure helpers.

**Verify:** Run `AlbumPlaylistCommandTest` and the existing playlist unit tests.

### Task 5: Add create-and-append command behavior (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModel.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playlist/AlbumPlaylistCommandTest.kt`

**Test first:**

Add tests asserting:

- A trimmed nonblank name is passed to playlist creation.
- The returned playlist ID is passed to bulk append.
- Blank names do not invoke either operation and return an error state.
- Append failure after creation is reported as an error.

**Implementation:**

Add `createAndAddAlbum(name, tracks)` to `AlbumPlaylistCommand` and expose `PlaylistViewModel.createAndAddAlbum(name, tracks)`. Run creation and append sequentially in one `viewModelScope.launch`; do not add a Room transaction or rollback behavior not required by the design.

**Verify:** Run the focused command test.

### Task 6: Build the populated inline playlist picker (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumPlaylistPicker.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumPlaylistPickerTest.kt`

**Test first:**

Create a Compose test with two `PlaylistEntity` items. Assert:

- The `AsciiFrame` title is `ADD ALBUM TO PLAYLIST`.
- Both playlist names appear.
- Each row exposes a click action and at least a 48 dp height.
- Clicking one row calls `onPlaylistSelected` once with that playlist.
- `Working` disables repeated selection.

**Implementation:**

Create `AlbumPlaylistPicker` with explicit inputs:

```kotlin
playlists: List<PlaylistEntity>
state: AlbumPlaylistUiState
onPlaylistSelected: (PlaylistEntity) -> Unit
onCreateAndAdd: (String) -> Unit
```

For a populated list, render full-width terminal rows inside `AsciiFrame`. Use `BracketButton` or a square clickable row with `TerminalDimensions.minimumTouchTarget`; do not use Material dialogs, cards, or buttons.

**Verify:** Run `assembleDebugAndroidTest`; execute the focused Compose test on a device when available.

### Task 7: Build the empty-state create-and-add form (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumPlaylistPicker.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumPlaylistPickerTest.kt`

**Test first:**

Add Compose tests for an empty playlist list:

- `TerminalTextField` and `[ CREATE + ADD ]` appear.
- The action is disabled for blank or whitespace-only input.
- Entering `Night Run` enables the action.
- Clicking calls `onCreateAndAdd("Night Run")`.
- The form remains visible for warning/error states.

**Implementation:**

Add a `rememberSaveable` playlist-name value inside `AlbumPlaylistPicker`. Show the creation form only when `playlists.isEmpty()`. Trim the name before invoking the callback. Render warning/error feedback with `TerminalNotice` and the corresponding severity.

**Verify:** Compile and, when available, run `AlbumPlaylistPickerTest`.

### Task 8: Add picker controls to album detail (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`

**Test first:**

Create or extend the album-detail Compose test to assert:

- `[ ADD TO PLAYLIST ]` is disabled when no track is `PLAYABLE`.
- It is enabled when at least one playable track exists.
- Clicking invokes `onTogglePlaylistPicker`.
- The picker is rendered only when `playlistPickerExpanded` is true.
- Success text is rendered as a terminal notice.

**Implementation:**

Add parameters with safe defaults so existing previews remain easy to call:

```kotlin
playlists: List<PlaylistEntity> = emptyList()
playlistPickerExpanded: Boolean = false
albumPlaylistState: AlbumPlaylistUiState = Idle
onTogglePlaylistPicker: () -> Unit = {}
onAddAlbumToPlaylist: (PlaylistEntity) -> Unit = {}
onCreatePlaylistAndAdd: (String) -> Unit = {}
```

Place `[ ADD TO PLAYLIST ]` with the album commands. Put `AlbumPlaylistPicker` in a `LazyColumn.item` directly after the album frame. Pass only playable tracks through the root callbacks; do not change track playback behavior.

**Verify:** Run `assembleDebugAndroidTest`.

### Task 9: Share playlist state with album detail (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Extend `app/src/androidTest/java/ca/stewark/nocturnel/ui/NocturneLAppTest.kt` or create it if absent. With injectable fake screen state, assert that selecting an album and expanding the picker displays the current playlist names.

**Implementation:**

Add `playlistViewModel: PlaylistViewModel = viewModel()` to `NocturneLApp`. Collect `playlists` and `albumPlaylistState` once at the root. Pass the same ViewModel to `PlaylistsScreen(viewModel = playlistViewModel, playback = playback)` so playlist changes are shared consistently.

Add `rememberSaveable` picker-expanded state scoped to the selected album. Clear it and call `clearAlbumPlaylistState()` when the selected album changes or album detail closes.

**Verify:** Run `assembleDebugAndroidTest`.

### Task 10: Wire add, create, collapse, and feedback behavior (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/NocturneLAppTest.kt`

**Test first:**

Add root interaction tests asserting:

- Choosing one playlist sends its ID/name and the album tracks to `addAlbum`.
- Create-and-add sends the entered name and album tracks.
- A `Success` state collapses the picker but leaves the success notice visible.
- `AlreadyPresent`, `Warning`, and `Error` leave the picker open.
- A playlist deletion warning is visible while the current playlist flow refreshes.

**Implementation:**

Wire album-detail callbacks to `PlaylistViewModel`. Add a `LaunchedEffect(albumPlaylistState)` that collapses only on `Success`. Keep no-op and failures expanded. Do not navigate to the Playlists destination after the operation.

**Verify:** Compile the Android tests and run them on an attached device when available.

### Task 11: Add the failing launcher-icon resource test (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/LauncherIconTest.kt`, `app/src/main/AndroidManifest.xml`

**Test first:**

Create an API 33+ instrumented test:

```kotlin
@Test
fun applicationUsesAdaptiveLauncherIcon() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNotEquals(0, context.applicationInfo.icon)
    assertTrue(context.getDrawable(context.applicationInfo.icon) is AdaptiveIconDrawable)
}
```

Run `assembleDebugAndroidTest`, then confirm the resource expectation is not yet met because the manifest has no launcher icon.

**Implementation:**

Do not add icon resources in this task. The failure establishes the resource contract before generation.

**Verify:** The test compiles and is expected to fail when executed before Tasks 12–15.

### Task 12: Generate and approve the CRT/pixel-N master (2–5 min)

**Files:** `docs/assets/nocturnel-icon-source.png`

**Test first:**

Before generation, record the visual acceptance checklist in the task notes:

- Square composition.
- Sharp retro CRT silhouette.
- Pixel-art `N` readable at thumbnail size.
- Terminal-black body, phosphor-green screen/glow, restrained amber accent.
- No words, music notes, rounded consumer-app styling, watermark, or soft-focus geometry.
- Foreground stays within the central adaptive-icon safe area.

**Implementation:**

Use the built-in `imagegen` workflow with this production prompt:

```text
Use case: logo-brand
Asset type: Android adaptive launcher icon source
Primary request: a sharp retro CRT terminal containing a bold glowing pixel-art letter N
Style/medium: crisp pixel-oriented logo mark, vector-friendly geometry, high contrast
Composition/framing: centered square icon, generous safe padding for Android adaptive masks
Color palette: terminal black CRT body, phosphor green screen and glow, restrained amber accent on the N
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background
Constraints: no words beyond the single N monogram; no music notes; no rounded app-tile container; crisp edges; no shadow on the background; no watermark
```

Inspect the result at launcher-thumbnail size. Save the selected unmodified source to `docs/assets/nocturnel-icon-source.png`; do not overwrite any unrelated asset.

**Verify:** Use `view_image` to confirm the checklist before continuing.

### Task 13: Produce and validate the transparent adaptive foreground (2–5 min)

**Files:** `docs/assets/nocturnel-icon-foreground.png`, `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png`

**Test first:**

Validate the generated source has a uniform magenta border and the CRT does not use the key color. If either assertion fails visually, regenerate once before post-processing.

**Implementation:**

Run the imagegen skill’s installed `remove_chroma_key.py` helper with border auto-keying, soft matte, despill, and `#ff00ff` source. Save the archival transparent result to `docs/assets/nocturnel-icon-foreground.png`.

Resize it to 432×432 while preserving aspect ratio and transparent padding, and save the packaged copy as `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png`. Keep the CRT inside the central 264×264 safe region.

**Verify:** Inspect the foreground with `view_image`; confirm transparent corners, crisp pixel edges, no magenta fringe, and readable `N`.

### Task 14: Add adaptive icon XML resources (2–5 min)

**Files:** `app/src/main/res/values/colors.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

**Test first:**

Keep the failing `LauncherIconTest` from Task 11 as the end-to-end contract. Before adding XML, run:

```powershell
Test-Path app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
Test-Path app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

Confirm both are `False`.

**Implementation:**

- Add `launcher_icon_background` as `#000000` in `colors.xml`.
- Create both adaptive icon XML files using `<background android:drawable="@color/launcher_icon_background"/>` and `<foreground android:drawable="@mipmap/ic_launcher_foreground"/>`.
- Do not add monochrome artwork unless separately requested.

**Verify:** Run Android resource processing with `.\gradlew.bat processDebugResources`. The standalone adaptive resources compile; the manifest is wired after base legacy resources exist in Task 15.

### Task 15: Generate legacy density fallbacks and wire the manifest (2–5 min)

**Files:** `app/src/main/res/mipmap-mdpi/ic_launcher.png`, `app/src/main/res/mipmap-hdpi/ic_launcher.png`, `app/src/main/res/mipmap-xhdpi/ic_launcher.png`, `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`, `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`, matching `ic_launcher_round.png` files, `app/src/main/AndroidManifest.xml`

**Test first:**

Keep `LauncherIconTest` as the contract. Confirm `assembleDebugAndroidTest` resolves the test but resource compilation remains incomplete until every manifest icon reference is present.

**Implementation:**

Composite the transparent foreground over terminal black and resize with nearest-neighbor or high-quality hard-edge preservation:

- mdpi: 48×48
- hdpi: 72×72
- xhdpi: 96×96
- xxhdpi: 144×144
- xxxhdpi: 192×192

Write both `ic_launcher.png` and `ic_launcher_round.png` at each density; use the same square source because launchers apply their own legacy/round mask. Update `<application>` with:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
```

Execute `LauncherIconTest` on a device when available.

### Task 16: Update album-detail screenshot coverage (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, generated references under `app/src/screenshotTestDebug/reference/`

**Test first:**

Change `AlbumDetailPreview` to pass two playlists with `playlistPickerExpanded = true`. Run:

```powershell
.\gradlew.bat validateDebugScreenshotTest
```

Confirm the existing reference fails because it lacks the new picker.

**Implementation:**

Keep the updated populated-picker preview and add a second `@PreviewTest` for the zero-playlist create-and-add form. Use deterministic data and no running animation.

Regenerate only after visually inspecting that the picker is inline, terminal-styled, readable, and does not obscure album commands.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
```

### Task 17: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–16

**Test first:**

Run the source/scope searches before the build:

```powershell
rg -n "material3\.(Button|OutlinedTextField|Card)|RoundedCornerShape" app/src/main/java/ca/stewark/nocturnel/ui
rg -n "RoomDatabase|version =" app/src/main/java/ca/stewark/nocturnel/data
```

The first should find no new Material-shaped controls; the second should show no database-version change from this feature.

**Implementation:**

Fix only failures introduced by this feature. Do not change Room entities, playlist codecs, playback behavior, or unrelated UI.

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

If a device is attached, also run `.\gradlew.bat connectedDebugAndroidTest`. Confirm generated icon sources and all packaged density/adaptive resources are present in the final change set.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Bulk append preserves existing order, album order, and skips duplicates.
- [ ] Album detail offers one inline playlist picker and an empty-state create-and-add flow.
- [ ] Success collapses the picker; no-op and failures retain it with correct terminal feedback.
- [ ] No Room schema or playlist format changes were made.
- [ ] The generated CRT/pixel-`N` icon passes visual inspection at full and launcher-thumbnail sizes.
- [ ] Adaptive and legacy icon resources compile and are referenced by the manifest.
- [ ] Album-detail populated and empty picker screenshot references are approved.
- [ ] Unit tests, Android-test compilation, screenshot validation, lint, and debug assembly pass.
- [ ] No unplanned files were modified.
