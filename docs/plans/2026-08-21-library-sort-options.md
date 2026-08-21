# LIB Sort Options Implementation Plan

**Date:** 2026-08-21  
**Design doc:** docs/specs/2026-08-21-library-sort-options-design.md  
**Status:** Ready for review

## Overview

Add a naturally sized, left-aligned bracket button above the populated LIB grid that cycles through Artist, Title, Year, and Most Played ordering. Store the selected mode in the existing terminal `SharedPreferences`, default safely to Artist, and keep favorites as the first comparison group in every mode. Work proceeds test-first through the sort-mode contract, album ordering, preference persistence, view-model state, Compose behavior, app-shell wiring, and screenshot coverage.

## Tasks

### Task 1: Define and test the sort-mode cycle (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/LibrarySortModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibrarySortMode.kt`

**Test first:**

Create `LibrarySortModeTest.kt` with tests that assert:

```kotlin
assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.DEFAULT)
assertEquals(LibrarySortMode.TITLE, LibrarySortMode.ARTIST.next())
assertEquals(LibrarySortMode.YEAR, LibrarySortMode.TITLE.next())
assertEquals(LibrarySortMode.MOST_PLAYED, LibrarySortMode.YEAR.next())
assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.MOST_PLAYED.next())
assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.fromPersisted(null))
assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.fromPersisted("UNKNOWN"))
assertEquals(LibrarySortMode.MOST_PLAYED, LibrarySortMode.fromPersisted("MOST_PLAYED"))
```

Run the focused test and confirm it fails because `LibrarySortMode` does not exist.

**Implementation:**

Create `LibrarySortMode.kt` in `ca.stewark.nocturnel.ui.listening`. Define enum entries in the stable cycle order `ARTIST`, `TITLE`, `YEAR`, `MOST_PLAYED`; expose display labels `ARTIST`, `TITLE`, `YEAR`, and `MOST PLAYED`; implement `next()` by advancing through `entries` with wraparound; and provide `DEFAULT = ARTIST` plus `fromPersisted(String?)`, which matches exact enum names and falls back to `DEFAULT` for null or unknown values. Do not add ascending/descending state.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibrarySortModeTest'`. All sort-mode tests pass.

---

### Task 2: Specify all favorite-first album comparators (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/LibraryAlbumOrderingTest.kt`

**Test first:**

Replace the current title-only expectations with focused tests that call the future signature:

```kotlin
orderLibraryAlbums(
    albums = albums,
    favoriteAlbumIds = favorites,
    sortMode = mode,
    albumPlayCounts = counts,
)
```

Cover these exact contracts with small `AlbumEntity` fixtures:

- Every enum mode keeps all favorite IDs before all non-favorite IDs.
- Artist orders case-insensitively by `artist`, then `title`.
- Title orders case-insensitively by `title`, then `artist`.
- Year parses trimmed integer values, orders valid years newest-first, puts null/blank/non-numeric values last, then uses artist/title tie-breaks.
- Most Played orders descending, treats a missing map entry as zero, then uses artist/title tie-breaks.
- Two rows equal under every active key retain their source order.

Run the focused test and confirm it fails to compile against the old two-argument ordering function.

**Implementation:** None in this task; it establishes the red ordering contract.

**Verify:** `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAlbumOrderingTest'` fails only for the missing new ordering API or the newly asserted behavior.

---

### Task 3: Implement deterministic album ordering (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryAlbumOrdering.kt`

**Test first:** Use the failing `LibraryAlbumOrderingTest` cases from Task 2 without weakening them.

**Implementation:**

Change `orderLibraryAlbums` to accept `sortMode: LibrarySortMode` and `albumPlayCounts: Map<String, Long>`. Build one comparator whose first key is `album.id !in favoriteAlbumIds`, then append the mode comparator:

```kotlin
ARTIST      -> artist A-Z, title A-Z
TITLE       -> title A-Z, artist A-Z
YEAR        -> invalid-year flag, parsed year descending, artist A-Z, title A-Z
MOST_PLAYED -> (albumPlayCounts[id] ?: 0L) descending, artist A-Z, title A-Z
```

Use `String.CASE_INSENSITIVE_ORDER` for text keys. Parse years with `album.year?.trim()?.toIntOrNull()`. Do not append album ID or source index: Kotlin's stable list sort must preserve input order when all comparator keys are equal.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAlbumOrderingTest' --tests '*LibrarySortModeTest'`. All tests pass.

---

### Task 4: Persist the selected sort mode (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`

**Test first:**

Add an instrumentation test using its own cleared preference name. Assert that a new repository exposes `LibrarySortMode.ARTIST`, calling `setLibrarySortMode(LibrarySortMode.YEAR)` updates the state immediately, and recreating the repository restores `YEAR`. Add malformed cases by writing both an unknown string and a wrong-typed value under `library_sort_mode`; each new repository must fall back to `ARTIST` without throwing.

Run the focused instrumentation class and confirm the new test fails because the repository API is absent.

**Implementation:**

In `TerminalPreferencesRepository`:

- Add `LIBRARY_SORT_MODE = "library_sort_mode"`.
- Initialize a private `MutableStateFlow` by wrapping `preferences.getString(...)` in `runCatching`, passing the result to `LibrarySortMode.fromPersisted`, and defaulting to `LibrarySortMode.DEFAULT` on any type/read failure.
- Expose it as `StateFlow<LibrarySortMode>`.
- Add `setLibrarySortMode(mode)` that writes `mode.name` with `apply()` and immediately updates the flow.

Leave the existing effects and visualizer preferences unchanged.

**Verify:** On an attached emulator/device, run `\.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest`. Otherwise run `\.\gradlew.bat assembleDebugAndroidTest` and verify the instrumentation source compiles.

---

### Task 5: Expose cycling through settings state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`

**Test first:**

Add a separate test that clears `terminal_preferences`, constructs `SettingsViewModel`, and asserts:

```kotlin
assertEquals(LibrarySortMode.ARTIST, viewModel.state.value.librarySortMode)
viewModel.cycleLibrarySortMode()
assertEquals(LibrarySortMode.TITLE, viewModel.state.value.librarySortMode)
viewModel.cycleLibrarySortMode()
assertEquals(LibrarySortMode.YEAR, viewModel.state.value.librarySortMode)
viewModel.cycleLibrarySortMode()
assertEquals(LibrarySortMode.MOST_PLAYED, viewModel.state.value.librarySortMode)
viewModel.cycleLibrarySortMode()
assertEquals(LibrarySortMode.ARTIST, viewModel.state.value.librarySortMode)
```

Cycle once more and assert a newly constructed `SettingsViewModel` restores `TITLE`. Clear the shared preference in `finally`.

**Implementation:**

- Add `librarySortMode: LibrarySortMode = LibrarySortMode.DEFAULT` to `TerminalSettingsState`.
- Include `repository.librarySortMode.value` in every state resolution path so changing effects or visualizer offset cannot reset the sort mode.
- Add `cycleLibrarySortMode()`: compute `_state.value.librarySortMode.next()`, persist it through the repository, and copy or resolve state with the new value.
- Keep `SettingsScreen` behavior unchanged; the new state property is consumed by LIB through the app shell, not rendered in Settings.

**Verify:** On an attached emulator/device, run `\.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.SettingsViewModelTest`. Otherwise run `\.\gradlew.bat assembleDebugAndroidTest`.

---

### Task 6: Add the cycling control to the LIB screen (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningScreensTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Update every existing `LibraryLandingScreen` call to provide `sortMode` and `onCycleSort`. Use `ARTIST` for existing ordering tests and adjust fixture artists so their expected order is explicit. Add tests that:

- Render local `var mode by remember { mutableStateOf(ARTIST) }`, pass `onCycleSort = { mode = mode.next() }`, assert `[ SORT: ARTIST ]`, tap it four times, and assert the labels advance through Title, Year, Most Played, and back to Artist.
- Use albums whose artist, title, year, and play-count orders differ; verify tapping the control immediately changes the grid order.
- Assert the sort control is absent when `albums` is empty.
- Retain and adapt the favorite-toggle test so moving into/out of the favorite group still works under Artist mode.

Run the focused instrumentation test and confirm it fails against the old screen signature/UI.

**Implementation:**

In `LibraryLandingScreen`:

- Add required `sortMode: LibrarySortMode` and `onCycleSort: () -> Unit` parameters.
- Preserve the current early empty-state return so no sort control appears without albums.
- For a populated library, render a `Column(Modifier.fillMaxSize())`.
- Place `BracketButton(label = "SORT: ${sortMode.label}", onClick = onCycleSort, modifier = Modifier.padding(horizontal = TerminalDimensions.sm))` first. Default Column alignment keeps it naturally sized and left-aligned.
- Place `AlbumGridScreen` below it inside a `Box(Modifier.weight(1f))` so the grid takes only the remaining height.
- Call `orderLibraryAlbums(albums, favoriteAlbumIds, sortMode, albumPlayCounts)` and retain existing keyed grid/favorite callbacks.

Do not add a menu, direction toggle, full-width modifier, or automatic scroll reset.

**Verify:** On an attached emulator/device, run `\.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.listening.ListeningScreensTest`. Otherwise run `\.\gradlew.bat assembleDebugAndroidTest`.

---

### Task 7: Wire preference state through both LIB branches (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/LibraryAppWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Extend `LibraryAppWiringTest` to assert the app source contains exactly two occurrences of each required argument, matching the scan-active and normal LIB branches:

```kotlin
sortMode = settings.librarySortMode
onCycleSort = settingsViewModel::cycleLibrarySortMode
```

Keep the existing album/favorite/count and removed-subview assertions. Run the focused unit test and confirm it fails because neither argument is wired yet.

**Implementation:**

Pass `settings.librarySortMode` and `settingsViewModel::cycleLibrarySortMode` to `LibraryLandingScreen` in both places in `NocturneLApp`: the branch below `LibraryScanStatus` and the normal `NocturneLDestination.LIBRARY` branch. Do not introduce local duplicate sort state. This ensures the control stays visible below scan status and uses the same persisted selection in both branches.

**Verify:** Run `\.\gradlew.bat testDebugUnitTest --tests '*LibraryAppWiringTest'` and `\.\gradlew.bat compileDebugKotlin`. Both pass.

---

### Task 8: Update deterministic screenshot coverage (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/RootPreview_Root effects off_fdcefa6f_0.png`

**Test first:**

Update `RootPreview` to pass `sortMode = LibrarySortMode.ARTIST` and `onCycleSort = {}`. Keep `red` as the favorite so the image proves favorite-first grouping, and retain deterministic counts. Run `\.\gradlew.bat validateDebugScreenshotTest`; confirm the Root preview is the only expected mismatch because the new sort button occupies space above the grid. Investigate any unrelated mismatch before updating references.

**Implementation:**

Import `LibrarySortMode`, compile the preview, then run `\.\gradlew.bat updateDebugScreenshotTest`. Inspect every changed image and retain only the Root reference update. Confirm visually that `[ SORT: ARTIST ]` is naturally sized, left-aligned above the album cards, that Red remains first as the favorite, and that the rest of the preview remains unchanged. If the screenshot tool changes the generated filename, resolve the exact old and new Root paths before removing only the superseded Root reference; never rename generated images manually.

**Verify:** Run `\.\gradlew.bat validateDebugScreenshotTest`. All screenshot references pass.

---

### Task 9: Run focused and full regression checks (2–5 min setup)

**Files:** All files changed in Tasks 1–8; no additional production files.

**Test first:** Review `git diff --check` and `git status --short`. Confirm the change set contains only the approved design, this plan, the listed sort feature files/tests, and the single intended Root screenshot reference update; preserve any pre-existing unrelated user changes.

**Implementation:** No new behavior. Fix only failures caused by this feature, without broadening scope.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

If an emulator/device is available, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest,ca.stewark.nocturnel.ui.settings.SettingsViewModelTest,ca.stewark.nocturnel.ui.listening.ListeningScreensTest
```

Manually confirm on LIB that the initial button reads `[ SORT: ARTIST ]`, each tap follows the approved cycle, favorites remain first in every mode, and the last selection survives an app restart.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] All new behavior was introduced test-first.
- [ ] Artist is the fresh-install and malformed-preference fallback.
- [ ] The button is naturally sized and left-aligned, and cycles in the approved order.
- [ ] Favorites remain first under Artist, Title, Year, and Most Played.
- [ ] The selected mode persists across navigation, rescans, source changes, and restart.
- [ ] Unit tests, Android-test compilation, focused device tests when available, screenshot validation, lint, and debug assembly pass.
- [ ] Only the intended Root screenshot reference changes.
- [ ] No unplanned or unrelated files are modified.
- [ ] The feature behaves exactly as described in the approved design document.
