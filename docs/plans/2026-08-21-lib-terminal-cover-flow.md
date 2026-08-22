# LIB Terminal Cover Flow Implementation Plan

**Date:** 2026-08-21  
**Design doc:** docs/specs/2026-08-21-lib-terminal-cover-flow-design.md  
**Status:** Ready for review

## Overview

Add a persisted `GRID`/`FLOW` mode to the populated LIB screen while retaining the existing grid as the default. The new flow screen will use a bounded snapping `LazyRow`, pure geometry and reconciliation helpers, terminal-styled selected-album metadata, existing artwork/favorite components, independent saveable scroll state, and restrained effects that honor the effective-effects setting. Work proceeds test-first through the mode contract, preference and view-model state, selection logic, Compose behavior, app wiring, and screenshot/regression coverage.

## Tasks

### Task 1: Define the library view-mode contract (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/listening/LibraryViewModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryViewMode.kt`

**Test first:**

Create `LibraryViewModeTest.kt`. Assert that `DEFAULT` is `GRID`, `GRID.next()` is `FLOW`, `FLOW.next()` is `GRID`, persisted names restore their matching values, and null or unknown strings fall back to `GRID`:

```kotlin
assertEquals(LibraryViewMode.GRID, LibraryViewMode.DEFAULT)
assertEquals(LibraryViewMode.FLOW, LibraryViewMode.GRID.next())
assertEquals(LibraryViewMode.GRID, LibraryViewMode.FLOW.next())
assertEquals(LibraryViewMode.FLOW, LibraryViewMode.fromPersisted("FLOW"))
assertEquals(LibraryViewMode.GRID, LibraryViewMode.fromPersisted(null))
assertEquals(LibraryViewMode.GRID, LibraryViewMode.fromPersisted("UNKNOWN"))
```

Run the focused test and confirm it fails because the type does not exist.

**Implementation:**

Create `LibraryViewMode.kt` in `ca.stewark.nocturnel.ui.listening`. Define `GRID("GRID")` and `FLOW("FLOW")` in toggle order, implement `next()` with enum-entry wraparound, expose `DEFAULT = GRID`, and parse only exact enum names in `fromPersisted(String?)`, falling back safely to `DEFAULT`.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*LibraryViewModeTest'`. All tests pass.

---

### Task 2: Persist the selected library view (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`

**Test first:**

Add tests using unique, cleared preference names. Assert that a new repository exposes `LibraryViewMode.GRID`; `setLibraryViewMode(FLOW)` updates the current repository flow immediately; and a newly constructed repository restores `FLOW`. Write an unknown string and then an integer under `library_view_mode`; each malformed case must construct successfully and expose `GRID`.

Run the focused instrumentation class and confirm the tests fail because the repository API is absent.

**Implementation:**

Add `LIBRARY_VIEW_MODE = "library_view_mode"`, a private `MutableStateFlow` initialized from `LibraryViewMode.fromPersisted(runCatching { preferences.getString(...) }.getOrNull())`, a public `StateFlow<LibraryViewMode>`, and `setLibraryViewMode(mode)` that stores `mode.name` and updates the flow immediately. Do not alter existing preference keys or defaults.

**Verify:** On an attached emulator/device, run `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest`. Otherwise run `.\gradlew.bat assembleDebugAndroidTest` and confirm the source compiles.

---

### Task 3: Expose persisted view mode through settings state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`

**Test first:**

Add a test that clears `terminal_preferences`, creates `SettingsViewModel`, asserts `GRID`, calls `toggleLibraryViewMode()`, asserts `FLOW`, calls it again, and asserts `GRID`. Toggle once more and assert that a new view model restores `FLOW`; clear the shared preference in `finally`. Extend an existing effects or visualizer test to assert those unrelated state changes do not reset `libraryViewMode`.

**Implementation:**

Add `libraryViewMode: LibraryViewMode = LibraryViewMode.DEFAULT` to `TerminalSettingsState`. Include `repository.libraryViewMode.value` in initial resolution and every later `resolve` call. Add `toggleLibraryViewMode()` that obtains `state.libraryViewMode.next()`, persists it, and resolves state with the new value. Keep this preference out of `SettingsScreen`; it is controlled from LIB.

**Verify:** Run the focused device test when available, or `.\gradlew.bat assembleDebugAndroidTest`. Confirm all `SettingsViewModelTest` cases compile and pass on device.

---

### Task 4: Specify centered-item geometry (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/CoverFlowSelectionTest.kt`

**Test first:**

Create tests for a future pure `nearestCoverIndex(viewportStart, viewportEnd, visibleItems)` helper using small `CoverFlowItemBounds(index, offset, size)` fixtures. Cover an exactly centered item, unequal left/right candidates, partially visible first and last items, and a rapid-scroll layout with several visible items. Assert that the item whose center has the smallest absolute distance from the viewport center is selected, with the lower index as the deterministic tie-break; an empty visible list returns `null`.

Run the focused test and confirm it fails because the geometry API does not exist.

**Implementation:** None in this task; it establishes the red geometry contract.

**Verify:** `.\gradlew.bat testDebugUnitTest --tests '*CoverFlowSelectionTest'` fails only for the missing helper/types.

---

### Task 5: Implement centered-item geometry (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/CoverFlowSelection.kt`

**Test first:** Use the failing geometry cases from Task 4 without weakening them.

**Implementation:**

Create internal `CoverFlowItemBounds(index: Int, offset: Int, size: Int)` and `nearestCoverIndex(...)`. Compute each item center as `offset + size / 2f`, compare it with `(viewportStart + viewportEnd) / 2f`, and return the closest item's index. Use index ascending as the only tie-break. Keep this file free of Compose dependencies so it remains a fast JVM unit-test seam.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*CoverFlowSelectionTest'`. Geometry tests pass.

---

### Task 6: Specify selection reconciliation after catalog changes (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/CoverFlowSelectionTest.kt`

**Test first:**

Add tests for a future `reconcileCoverFlowSelection(previousAlbumId, previousIndex, albumIds)` helper. Assert that it:

- preserves the same album by ID after sort reordering;
- uses the previous numeric index when the selected album disappears;
- clamps that numeric index to the new last item after shrinkage;
- selects index zero for a first non-empty list; and
- returns a null ID/index result for an empty list.

Run the focused test and confirm only the new reconciliation cases fail.

**Implementation:** None in this task; it establishes the red catalog-change contract.

**Verify:** The focused test fails only for missing reconciliation behavior.

---

### Task 7: Implement selection reconciliation (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/CoverFlowSelection.kt`

**Test first:** Use the failing reconciliation tests from Task 6.

**Implementation:**

Add an internal result containing nullable `albumId` and `index`. For an empty list return null/null. Otherwise, prefer the index of `previousAlbumId`; when absent, clamp `previousIndex` into the current indices, defaulting to zero. Return the ID at the resolved index. Do not introduce catalog, database, or sorting dependencies.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*CoverFlowSelectionTest'`. All geometry and reconciliation tests pass.

---

### Task 8: Establish the terminal reel layout and metadata (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Create `AlbumCoverFlowScreenTest` with a three-album fixture and an injected `rememberLazyListState()`. Render the future screen at a fixed phone-sized surface and assert:

- a `cover-flow-reel` node exists;
- the initial centered cover exposes `Selected <title>, 1 of 3`;
- `> CURRENT_`, `01 / 03`, uppercased title, artist, `12×`, and the existing favorite description are displayed;
- long title/artist fixtures remain present as semantics without overlapping the favorite target; and
- one-album and two-album fixtures show correct `01 / 01` and bounded position text.

Run the focused instrumentation test and confirm it fails because the composable does not exist.

**Implementation:**

Create `AlbumCoverFlowScreen.kt`. Its required inputs are the ordered albums, `LazyListState`, favorite IDs, play counts, `effectsEnabled`, selected album ID/change callback, album-open callback, and favorite callback. Build a vertical terminal readout containing a tagged horizontal `LazyRow`, `RetroArtwork`, a bright selected `AsciiFrame`, dimmer side covers, `> CURRENT_`, zero-padded one-based position, one-line ellipsized title/artist, play count, and `FavoriteToggle`. Size the selected cover prominently and side covers at a fixed smaller scale; use `BoxWithConstraints` to calculate symmetric horizontal content padding so the first and last covers can center. Add stable tags/content descriptions only where they express behavior under test.

**Verify:** Run the focused device test, or `.\gradlew.bat assembleDebugAndroidTest` when no device is available.

---

### Task 9: Add bounded snapping and centered-selection updates (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add Compose tests that swipe the tagged reel left, wait for idle, and assert exactly one next cover has the selected content description and updated `02 / 03` metadata. Repeat to reach `03 / 03`, swipe again, and assert it remains there; swipe back repeatedly and assert it stops at `01 / 03`. Include a rapid long swipe and assert one valid album is selected after settling.

**Implementation:**

Attach Compose Foundation's `rememberSnapFlingBehavior(lazyListState = state)` to the `LazyRow`; do not use an infinite or duplicated list. Observe `state.layoutInfo` with `snapshotFlow`, adapt visible items to `CoverFlowItemBounds`, call `nearestCoverIndex`, and emit the album ID through the selected-ID callback only when it changes. The symmetric content padding from Task 8 allows true centering at both bounded ends.

**Verify:** Run the focused `AlbumCoverFlowScreenTest` on device/emulator. Swiping tests pass without timing retries.

---

### Task 10: Implement side-cover and center-cover tap behavior (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Render three albums with the first selected and capture opened IDs. Tap the third tagged cover and assert it becomes selected while the opened list stays empty; tap it again and assert its ID is opened exactly once. Also tap the initially centered cover in a separate case and assert immediate opening.

**Implementation:**

Give each cover one click handler. If its ID is selected, invoke `onAlbumSelected(album)`. Otherwise, scroll it to the center: use `animateScrollToItem(index)` when `effectsEnabled` is true and `scrollToItem(index)` when false. Let the layout observer update selected ID; do not open a side cover during the centering tap and do not add an `[ OPEN ]` button.

**Verify:** Run the focused device test. Side taps center only; the second/center tap opens.

---

### Task 11: Reconcile the reel and honor the effects policy (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add a stateful Compose test that centers album B, reorders the input list, and asserts B remains selected at its new position. Remove B and assert the nearest valid index becomes selected; then empty the list and assert the screen does not retain stale selected metadata. Add effects-on/off cases that assert tagged animated/static reel presentation while both remain swipeable and clickable.

**Implementation:**

On album-ID-list changes, call `reconcileCoverFlowSelection` with the current selected ID and index, update the selected ID, and move the `LazyListState` to the resolved index. Use an effect-enabled animated scale/alpha/glow transition for selected-versus-side presentation; when false, apply the final selected/side values directly and use immediate programmatic scrolling. Do not disable user scrolling or snapping when effects are off. The parent empty-library branch remains responsible for the normal empty notice.

**Verify:** Run the focused device test and `.\gradlew.bat testDebugUnitTest --tests '*CoverFlowSelectionTest'`. Both pass.

---

### Task 12: Toggle grid/flow within LIB and retain independent state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/listening/ListeningScreensTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/LibraryLandingScreen.kt`

**Test first:**

Update existing calls for the future arguments. Add tests that:

- render populated LIB in `GRID`, assert `[ VIEW: GRID ]`, click it, and assert the flow reel plus `[ VIEW: FLOW ]`;
- click again and assert the grid returns;
- assert both sort and view controls are absent for an empty library;
- favorite the centered flow album and verify its favorite state and favorite-first ordering update;
- cycle sort in flow mode and assert the selected album is preserved by ID; and
- move the grid away from its start, switch to flow and move that independently, then toggle each view back and assert each retains its own position.

Run the focused instrumentation test and confirm it fails against the old signature/UI.

**Implementation:**

Change `LibraryLandingScreen` to require `viewMode`, `flowState: LazyListState`, `effectsEnabled`, and `onToggleView`. Keep the existing grid `LazyGridState` parameter. Retain a `rememberSaveable` selected flow album ID outside the conditional view branch. Render a compact `Row` with `SORT: <mode>` and `VIEW: <mode>` bracket controls, then render either `AlbumGridScreen` or `AlbumCoverFlowScreen` in the remaining height using the same single `orderLibraryAlbums(...)` result. Keep the current early empty return so neither control appears when empty. Do not synchronize or reset the two scroll states when toggling.

**Verify:** Run the focused `ListeningScreensTest` on device/emulator, or compile it with `.\gradlew.bat assembleDebugAndroidTest`.

---

### Task 13: Wire persisted mode, effects, and flow state through the app shell (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/LibraryAppWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Extend `LibraryAppWiringTest` to assert one root `rememberLazyListState()` is retained for LIB flow and exactly two occurrences of each argument appear, covering scan-active and normal LIB branches:

```kotlin
viewMode = settings.libraryViewMode
flowState = libraryFlowState
effectsEnabled = settings.effectiveEffectsEnabled
onToggleView = settingsViewModel::toggleLibraryViewMode
```

Retain the existing two-occurrence sort assertions. Run the focused test and confirm it fails for the absent wiring.

**Implementation:**

Create `libraryFlowState = rememberLazyListState()` beside `libraryGridState`. Pass the persisted mode, independent list state, effective-effects value, and toggle callback to both `LibraryLandingScreen` calls: the scan-status branch and normal LIB destination. Do not create branch-local mode or list state.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests '*LibraryAppWiringTest'` and `.\gradlew.bat compileDebugKotlin`. Both pass.

---

### Task 14: Add deterministic screenshot coverage (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Update `RootPreview` with `viewMode = GRID`, a remembered list state, `effectsEnabled = false`, and the no-op toggle. Add two `@PreviewTest` functions at the Pixel 7 viewport: a populated `FLOW` reel with effects enabled and a populated `FLOW` reel with effects disabled. Use fixed albums, favorites, play counts, initial selected index, and no-op callbacks. Run `.\gradlew.bat validateDebugScreenshotTest`; confirm only the default LIB preview and two missing new references are reported.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest`. Inspect every changed/generated image. Retain only the updated Root golden and the two new cover-flow goldens. Confirm the current cover is dominant and framed, neighboring covers are smaller/dimmer, metadata and favorite state are legible, the controls read `[ SORT: ARTIST ]` and `[ VIEW: FLOW ]`, and effects-off retains the same usable structure without animated decoration. Resolve generated filenames explicitly before removing any superseded reference.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All screenshot references pass.

---

### Task 15: Run focused and full regression checks (2–5 min setup)

**Files:** All files changed in Tasks 1–14; no additional production files.

**Test first:** Review `git diff --check` and `git status --short`. Confirm the change set contains only the approved design, this plan, the listed feature/test files, and intended screenshot references; preserve unrelated user changes.

**Implementation:** No new behavior. Fix only regressions caused by this feature without broadening scope.

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
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest,ca.stewark.nocturnel.ui.settings.SettingsViewModelTest,ca.stewark.nocturnel.ui.listening.ListeningScreensTest,ca.stewark.nocturnel.ui.library.AlbumCoverFlowScreenTest
```

Manually verify on LIB that grid is the first-run fallback, the persisted view survives restart, the reel stops at both ends, side taps center, center taps open, sort/favorite changes preserve valid selection, grid and flow retain independent positions, and effects-off/reduced-motion behavior remains fully operable.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] All new behavior was introduced test-first.
- [ ] Grid remains the fresh-install and malformed-preference fallback.
- [ ] The populated LIB screen toggles between grid and terminal flow and persists that choice.
- [ ] The bounded reel snaps exactly one album to center and implements the approved side/center tap behavior.
- [ ] Selected metadata, play count, favorite state, sort behavior, and catalog reconciliation are correct.
- [ ] Grid and flow retain independent saveable session positions.
- [ ] Effects-off and reduced-motion modes retain function without decorative animation.
- [ ] Unit tests, Android-test compilation, focused device tests when available, screenshot validation, lint, and debug assembly pass.
- [ ] Only intended screenshot references change.
- [ ] No unplanned or unrelated files are modified.
- [ ] The feature behaves exactly as described in the approved design document.
