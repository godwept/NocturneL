# Album Detail Metadata Actions Row Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-album-detail-metadata-actions-row-design.md`  
**Status:** Ready for review

## Overview

Condense the Album Detail metadata controls by leaving the album play count on a dedicated line and placing `FAV`, `SET COVER`, and `ADD TO PLAYLIST` together in one natural-width row. Remove the Album Detail `CLEAR` action and its callback wiring without altering stored manual artwork, artwork replacement, favorite behavior, playlist-picker behavior, track rows, or the top album action row. The work must preserve all existing uncommitted queue-layout and visualizer changes.

## Tasks

### Task 1: Capture the metadata-row contract in Compose tests (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`

**Test first:**

Add a test that renders a 412dp-wide Album Detail with a manual artwork URI, a nonzero play count, playable tracks, and counters for the three retained actions. Keep the current `onClearArtwork` argument temporarily so the test compiles against the pre-change screen contract.

```kotlin
@Test fun albumMetadataActionsShareOneNaturalWidthRowWithoutClear() {
    var favoriteToggles = 0
    var coverSelections = 0
    var playlistToggles = 0
    compose.setContent {
        NocturneLTheme {
            Box(Modifier.width(412.dp)) {
                AlbumDetailScreen(
                    album = sampleAlbum.copy(manualArtworkUri = "content://manual"),
                    tracks = sampleTracks,
                    onBack = {},
                    onPlay = {},
                    onPlayAlbum = {},
                    onChooseArtwork = { coverSelections++ },
                    onClearArtwork = {},
                    albumPlayCount = 7,
                    onToggleAlbumFavorite = { favoriteToggles++ },
                    onTogglePlaylistPicker = { playlistToggles++ },
                )
            }
        }
    }

    compose.onNodeWithText("[ CLEAR ]").assertDoesNotExist()
    val playCount = compose.onNodeWithText("7 PLAY(S)").fetchSemanticsNode().boundsInRoot
    val controls = listOf(
        compose.onNodeWithContentDescription("Add Red Horizon to favorites"),
        compose.onNodeWithText("[ SET COVER ]"),
        compose.onNodeWithText("[ ADD TO PLAYLIST ]"),
    )
    val controlBounds = controls.map { it.fetchSemanticsNode().boundsInRoot }
    assertTrue(playCount.bottom < controlBounds.minOf { it.top })
    assertTrue(controlBounds.maxOf { it.top } - controlBounds.minOf { it.top } <= 1f)

    controls.forEach {
        assertEquals(1, it.textLayoutResult().lineCount)
        assertFalse(it.textLayoutResult().hasVisualOverflow)
    }
    controls.forEach { it.performClick() }
    assertEquals(1, favoriteToggles)
    assertEquals(1, coverSelections)
    assertEquals(1, playlistToggles)
}
```

If the favorite semantics node does not expose `GetTextLayoutResult` because the button wrapper owns the content description, collect text layouts separately from `[ FAV ]`, `[ SET COVER ]`, and `[ ADD TO PLAYLIST ]`, and use the semantics node only for the favorite click. Reuse the test file's existing `textLayoutResult` helper.

Run the focused connected test before implementation when a device is available. It must fail because `CLEAR` is present for manual artwork and the three retained controls occupy different rows. If no device is attached, compile the test with `assembleDebugAndroidTest` and record runtime red/green execution as pending; the pre-change Album Detail screenshot is the deterministic visual reproduction.

**Implementation:**

Do not change production code in this task. Keep the new assertions focused on vertical ordering, one-row alignment, unwrapped labels, and retained callbacks.

**Verify:** Run `\.\gradlew.bat assembleDebugAndroidTest`. With a device, run the focused `AlbumDetailScreenTest` and confirm the new test fails for the expected layout and `CLEAR` assertions.

### Task 2: Reorganize Album Detail metadata controls and remove clear wiring (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`, `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

Use the failing test from Task 1. Before changing the layout, run:

```powershell
rg -n 'onClearArtwork|BracketButton\("CLEAR"' app/src/main app/src/androidTest app/src/screenshotTest
```

It must find the screen parameter, the conditional `CLEAR` button, app wiring, Android-test arguments, and the two Album Detail preview arguments.

**Implementation:**

- Remove `onClearArtwork: () -> Unit` from `AlbumDetailScreen`.
- Replace the existing play-count/favorite row, cover row, and standalone playlist button with:
  - a dedicated `Text("$albumPlayCount PLAY(S)", ...)` line;
  - one natural-width `Row` containing, in order:
    1. `FavoriteToggle(album.title, albumFavorite, { onToggleAlbumFavorite(album) })`;
    2. `BracketButton("SET COVER", onChooseArtwork)`;
    3. the existing `BracketButton("ADD TO PLAYLIST", ...)` with its playable-track enablement and `playlistPickerExpanded` selected state unchanged.
- Delete the conditional `CLEAR` button entirely. Do not clear, migrate, or otherwise mutate `album.manualArtworkUri`.
- Remove `onClearArtwork = { viewModel.setManualArtwork(selectedAlbum.id, null) }` from `NocturneLApp.kt`. Keep the artwork picker and `setManualArtwork` replacement path unchanged.
- Remove `onClearArtwork` arguments from named Android-test and screenshot-preview calls.
- Update positional `AlbumDetailScreen` calls in `AlbumDetailScreenTest.kt` by removing only the obsolete clear callback placeholder. Prefer named arguments in the new layout test so its callback intent remains explicit.
- Do not alter track-row spacing, the top `BACK`/`PLAY`/`SHUFFLE`/`ADD QUEUE` row, `AlbumPlaylistPicker`, or shared button sizing.

**Verify:** Run `\.\gradlew.bat assembleDebugAndroidTest` and:

```powershell
rg -n 'onClearArtwork|BracketButton\("CLEAR"' app/src/main app/src/androidTest app/src/screenshotTest
```

The build passes and the source scan returns no Album Detail clear-action matches. With a device, the focused `AlbumDetailScreenTest` passes.

### Task 3: Update and inspect Album Detail screenshots (2–5 min)

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/AlbumDetailPreview_Album detail_e79e2a4d_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/AlbumDetailEmptyPlaylistPreview_Album detail empty playlist_5746484a_0.png`

**Test first:**

Run `\.\gradlew.bat validateDebugScreenshotTest` after Task 2. It must fail only for the Album Detail references whose metadata controls moved.

**Implementation:**

- Run `\.\gradlew.bat updateDebugScreenshotTest`.
- Inspect both changed 412dp references.
- Confirm the play count is above `FAV`, `SET COVER`, and `ADD TO PLAYLIST`; all three controls share one row; every label is complete and single-line; `CLEAR` is absent; and the open empty/populated playlist-picker states remain intact.
- Retain only the intended Album Detail reference updates. The screenshot source and references contain existing uncommitted visualizer and queue-layout work; do not regenerate, restore, or modify unrelated references.

**Verify:** Run `\.\gradlew.bat validateDebugScreenshotTest`. It passes with no unexpected reference changes.

### Task 4: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–3

**Test first:**

Run the final source checks:

```powershell
rg -n 'onClearArtwork|BracketButton\("CLEAR"' app/src/main app/src/androidTest app/src/screenshotTest
rg -n 'FavoriteToggle\(album.title|BracketButton\("SET COVER"|"ADD TO PLAYLIST"' app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt
```

The first command returns no matches. Inspect the second command's results to confirm all three retained controls remain in the Album Detail metadata block.

**Implementation:**

Fix only failures caused by the Album Detail metadata-row change. Do not alter artwork persistence, playlist selection, favorite state, queue behavior, track-row density, visualizers, dependencies, or unrelated screenshot references.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebugAndroidTest validateDebugScreenshotTest lintDebug assembleDebug
git diff --check
git status --short
```

If a device or emulator is attached, also run `\.\gradlew.bat connectedDebugAndroidTest`. Report connected tests as pending when no device is available. Confirm the final diff contains only this approved change and the pre-existing uncommitted queue-layout and visualizer work.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] The Album Detail play count appears on its own line.
- [ ] `FAV`, `SET COVER`, and `ADD TO PLAYLIST` appear in order on one natural-width row at 412dp.
- [ ] All three labels remain complete, single-line, unclipped, and retain minimum touch targets.
- [ ] `ADD TO PLAYLIST` retains playable-track enablement and expanded-picker selected state.
- [ ] Album favorite, cover selection, and playlist-picker callbacks remain functional.
- [ ] `CLEAR` and `onClearArtwork` are absent from Album Detail UI, contract, callers, tests, and previews.
- [ ] Existing manual artwork is neither cleared nor migrated by this UI change.
- [ ] Track rows, top album actions, playlist-picker contents, queue behavior, and visualizer behavior remain unchanged.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected tests pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
- [ ] Existing uncommitted queue-layout and visualizer changes remain intact.
