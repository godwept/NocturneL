# LIB Album Collection Implementation Plan

**Date:** 2026-08-21  
**Design doc:** docs/specs/2026-08-21-library-album-collection-design.md  
**Status:** Ready for review

## Overview

Replace the mixed LIB landing page with one continuous album grid. The presentation layer will derive a stable favorite-first, case-insensitive alphabetical order, reuse the existing album-grid component, immediately reorder after favorite changes, remove the LIB-only Resume/Favorites/History UI and routing, and preserve all underlying listening-history, play-count, favorite, and playback persistence.

## Tasks

### Task 1: Define the album ordering projection (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/LibraryAlbumOrderingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryAlbumOrdering.kt`

**Test first:**

Create `LibraryAlbumOrderingTest` with focused JVM tests using `AlbumEntity` fixtures. Write the tests before the projection and confirm they fail to compile because `orderLibraryAlbums` does not exist.

```kotlin
class LibraryAlbumOrderingTest {
    @Test fun `favorites lead and both groups sort by title ignoring case`() {
        val albums = listOf(album("z", "zebra"), album("b", "Beta"), album("a", "alpha"), album("g", "Gamma"))

        assertEquals(
            listOf("b", "g", "a", "z"),
            orderLibraryAlbums(albums, setOf("g", "b")).map { it.id },
        )
    }

    @Test fun `equal titles retain source order`() {
        val albums = listOf(album("first", "Signal"), album("second", "signal"))

        assertEquals(listOf("first", "second"), orderLibraryAlbums(albums, emptySet()).map { it.id })
    }
}
```

Use a private fixture helper that supplies all required `AlbumEntity` fields. Implement one internal pure function with a stable comparator: favorite membership first (`false` for a favorite before `true` for a non-favorite), then `title` with `String.CASE_INSENSITIVE_ORDER`. Do not query Room or mutate the input list.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAlbumOrderingTest'`. Both ordering tests pass.

### Task 2: Reduce the LIB landing screen to the reusable album grid (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningScreensTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Replace `landingOrdersResumeFavoritesRecentAndLibrary` with `landingShowsOnlyTheOrderedAlbumCollection`. Render several albums with one favorite, assert every album title and favorite control is displayed, and assert `RESUME`, `+--[ FAVORITES ]`, `+--[ RECENTLY PLAYED ]`, `VIEW ALL FAVORITES`, `VIEW ALL HISTORY`, and `+--[ ALBUM LIBRARY ]` do not exist. Update the empty test to pass an empty album list and assert the existing `No playable albums yet. Rescan after adding music.` notice.

Add a reactive test before changing production code:

```kotlin
@Test fun favoriteToggleImmediatelyMovesTheAlbum() {
    // Render Alpha, Beta, Gamma with Gamma initially favorite.
    // Capture title bounds to prove Gamma is first.
    // Click "Remove Gamma from favorites" while the host updates a remembered favorite-ID set.
    // Capture bounds again and prove the order is Alpha, Beta, Gamma.
}
```

Compare each title's `fetchSemanticsNode().boundsInRoot` as `(top, left)` so the assertion follows the two-column grid's visual order. Delete the obsolete resume-title ellipsis and `ListeningTrackRow` tests from this file; keep `settingsConfirmsBothDestructiveActions` unchanged.

Narrow `LibraryLandingScreen` to these parameters only:

```kotlin
fun LibraryLandingScreen(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
    albumPlayCounts: Map<String, Long>,
    state: LazyGridState,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
)
```

Its body should delegate to `AlbumGridScreen`, passing `orderLibraryAlbums(albums, favoriteAlbumIds)`, the existing grid state, favorite IDs, play counts, album selection, and `{ onFavoriteAlbum(it.id) }`. Remove the Resume frame, daily sections, track rows, section headings, and their now-unused imports. This keeps the existing empty notice, card visuals, counts, and favorite controls in one shared implementation.

**Verify:** Run `\.\gradlew.bat assembleDebugAndroidTest`. The Android test sources compile with the new landing contract.

### Task 3: Remove LIB subview routing from the app shell (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/LibraryAppWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Add a JVM source-wiring guard that reads `NocturneLApp.kt` and requires the narrowed album inputs while forbidding the removed routing tokens:

```kotlin
@Test fun `library wires one album collection without listening subviews`() {
    val app = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()
    assertTrue("favoriteAlbumIds = listening.favoriteAlbumIds" in app)
    assertTrue("albumPlayCounts = listening.albumPlayCounts" in app)
    assertFalse("librarySubview" in app)
    assertFalse("FavoritesScreen" in app)
    assertFalse("ListeningHistoryScreen" in app)
    assertFalse("resumeState(" in app)
}
```

Run it once and confirm it fails. Then remove the Favorites, History, and resume imports; remove `librarySubview` saveable state and its BackHandler/reset branches; and replace the nested LIB `when` with one `LibraryLandingScreen` call. Update both the normal LIB branch and the active-scan LIB branch to pass albums, `listening.favoriteAlbumIds`, `listening.albumPlayCounts`, the existing `libraryGridState`, album selection, and `listeningViewModel::toggleAlbum`. Do not change album/artist detail handling, destination switching, scan progress, playback controls, or listening persistence.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAppWiringTest'` and `\.\gradlew.bat compileDebugKotlin`. The wiring guard and app compilation pass.

### Task 4: Delete the unreachable LIB-only UI (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/LibraryAppWiringTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/listening/ResumeProjectionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/FavoritesScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningHistoryScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningRows.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningUiModels.kt`

**Test first:**

Extend `LibraryAppWiringTest` with a source cleanup guard asserting the three removed UI files no longer exist and `ListeningUiModels.kt` no longer contains `ResumeUiState`, `resumeState`, or the three `previewFavoriteAlbums` / `previewFavoriteTracks` / `previewRecentTracks` properties. Run it and confirm it fails.

Delete the now-unreachable Favorites and History composables and their shared `ListeningRows` helper. Remove only the LIB-specific `ResumeUiState`, `resumeState`, and preview slicing properties from `ListeningUiModels.kt`, and delete their obsolete `ResumeProjectionTest`. Keep `ListeningUiState`'s persisted-data projections, `ListeningViewModel`, repository/DAO history flows, favorite IDs, track favorites, counts, clear-history behavior, and playback state unchanged.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAppWiringTest'` and `\.\gradlew.bat compileDebugKotlin`. Then run `rg -n 'FavoritesScreen|ListeningHistoryScreen|ListeningTrackRow|ResumeUiState|resumeState|previewRecentTracks' app/src/main app/src/test app/src/androidTest`; it returns no matches.

### Task 5: Update screenshot previews and references (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/RootPreview_Root effects off_fdcefa6f_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/FavoritesPreview_Favorites_854b7020_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/HistoryPreview_History_eb110c6d_0.png`

**Test first:**

Update `RootPreview` to call the narrowed `LibraryLandingScreen` with deterministic favorites and counts, choosing only `red` as favorite so the preview proves a favorite can lead albums whose normal alphabetical order would be Amber, Blue, Red. Remove the Favorites and History preview functions plus their imports and history fixtures. Run `\.\gradlew.bat validateDebugScreenshotTest` and confirm the Root reference is stale before updating it; investigate any unrelated mismatch.

Delete exactly the obsolete Favorites and History reference PNGs after resolving and confirming their paths under `app/src/screenshotTestDebug/reference`. Run `\.\gradlew.bat updateDebugScreenshotTest`, inspect the new Root image for a single uninterrupted grid ordered Red, Amber, Blue with no Resume/Favorites/Recently Played headers, and retain only the intended Root reference change and the two intentional reference deletions.

**Verify:** Run `\.\gradlew.bat validateDebugScreenshotTest`. All retained screenshot references pass.

### Task 6: Run focused behavior checks (2–5 min)

**Files:** Files changed in Tasks 1–5; no additional files unless a failing approved test requires a targeted correction.

**Test first:**

Run the focused checks before cleanup:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*LibraryAlbumOrderingTest' --tests '*LibraryAppWiringTest'
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
```

If an emulator or device is available, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.listening.ListeningScreensTest
```

Correct only failures owned by this plan. Do not weaken ordering, absence, empty-state, or immediate-reordering assertions.

**Implementation:**

No feature work is planned in this task. Apply only minimal fixes required by the focused checks, then rerun the failed command.

**Verify:** All available focused checks pass; any unavailable device-only check is explicitly recorded for handoff.

### Task 7: Run the complete quality gate and inspect scope (2–5 min)

**Files:** All files changed by Tasks 1–6; no unrelated files.

**Test first:**

Run the complete project checks:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

If an emulator/device is available, also run `\.\gradlew.bat connectedDebugAndroidTest`.

**Implementation:**

Make only targeted corrections for failures caused by this feature. Inspect `git diff --` for the exact files named in this plan and confirm the pre-existing release-workflow, Play Store, documentation, privacy, and release-guide edits remain untouched.

**Verify:** All available checks pass. `git status --short` shows only this plan's files plus the user's pre-existing changes, and the final diff matches the approved design without database, repository, playback, Search, playlist, queue, Now Playing, or album-detail changes.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every new production behavior was introduced test-first
- [ ] Favorited albums lead the LIB grid and both groups sort case-insensitively by title
- [ ] Favorite toggles immediately reorder the grid
- [ ] Resume, Favorites, Recently Played, and their LIB subviews are absent
- [ ] Listening history, favorites, play counts, and playback persistence remain intact
- [ ] Unit, Android-test compilation, screenshot validation, lint, and debug assembly pass
- [ ] Available device/emulator tests pass, or their unavailability is recorded
- [ ] Only planned files were modified; pre-existing user changes were preserved
- [ ] The feature behaves exactly as described in the approved design document
