# Library Frame Removal Implementation Plan

**Date:** 2026-07-29  
**Design doc:** `docs/specs/2026-07-29-library-frame-removal-design.md`  
**Status:** Ready for review

## Overview

Remove the complete source-folder and rescan frame from the Library destination so the album grid starts immediately at the top of the content area. Keep all scan behavior and shared scaffold status messaging intact, retain the existing Rescan Library action in Settings, and update Compose and screenshot coverage without changing the library ViewModel, scanning logic, navigation, or album-grid styling.

## Tasks

### Task 1: Add the Library-frame removal UI contract (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/LibraryScreenTest.kt`

**Test first:**

Create a Compose test in package `ca.stewark.nocturnel.ui` that calls the desired simplified `LibraryScreen(albums, onAlbumSelected)` API:

```kotlin
package ca.stewark.nocturnel.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun libraryStartsWithAlbumsAndHasNoRescanFrame() {
        compose.setContent {
            NocturneLTheme {
                LibraryScreen(listOf(sampleAlbum)) {}
            }
        }

        compose.onNodeWithText(sampleAlbum.title).assertIsDisplayed()
        compose.onAllNodesWithText("[ RESCAN ]").assertCountEquals(0)
        compose.onAllNodesWithText("[ CANCEL ]").assertCountEquals(0)
    }

    @Test fun settingsRetainsRescanAction() {
        var rescanned = false
        compose.setContent {
            NocturneLTheme {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = { rescanned = true },
                    state = TerminalSettingsState(),
                    onEffectsChanged = {},
                )
            }
        }

        compose.onNodeWithText("[ RESCAN LIBRARY ]").assertIsDisplayed().performClick()
        assertTrue(rescanned)
    }
}
```

Run Android-test compilation and confirm it fails because the current `LibraryScreen` is private and still requires `LibrarySourceViewModel`.

**Implementation:**

Do not change production code in this task. The compile failure establishes both contracts: the simplified Library API and the retained Settings action.

**Verify:** Run:

```powershell
.\gradlew.bat compileDebugAndroidTestKotlin
```

Confirm the expected failure points to `LibraryScreen` visibility/signature rather than unrelated test errors.

### Task 2: Remove the Library frame and unused ViewModel parameter (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Keep `LibraryScreenTest` failing from Task 1 while making the smallest production change that satisfies it.

**Implementation:**

Change the Library destination call from:

```kotlin
LibraryScreen(
    albums,
    viewModel,
    onAlbumSelected = { selectedAlbumId = it.id },
)
```

to:

```kotlin
LibraryScreen(albums) { selectedAlbumId = it.id }
```

Replace the existing private function with:

```kotlin
@Composable
internal fun LibraryScreen(
    albums: List<AlbumEntity>,
    onAlbumSelected: (AlbumEntity) -> Unit,
) {
    AlbumGridScreen(albums, onAlbumSelected)
}
```

Remove imports that become unused only because the frame is gone:

- `Column`
- `Row`
- `fillMaxSize`
- `padding`
- `Modifier`
- `AsciiFrame`
- `BracketButton`
- `TerminalNotice`
- `TerminalDimensions`

Do not alter `TerminalScaffold(status = viewModel.scanState.message)`, `SettingsScreen(onRescan = viewModel::rescan)`, `LibrarySetupScreen`, `LibrarySourceViewModel`, or any scanning methods.

**Verify:** Run:

```powershell
.\gradlew.bat compileDebugAndroidTestKotlin
```

The new Library and Settings Compose tests compile.

### Task 3: Point root screenshot coverage at the real Library screen (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/RootPreview_Root effects off_fdcefa6f_0.png`

**Test first:**

Change only the content of `RootPreview` from:

```kotlin
AlbumGridScreen(previewAlbums) {}
```

to:

```kotlin
LibraryScreen(previewAlbums) {}
```

This makes the root screenshot exercise the actual Library wrapper instead of bypassing it.

**Implementation:**

Run screenshot validation. The reference is expected to remain pixel-identical because the approved Library implementation now delegates directly to `AlbumGridScreen`. If validation reports a difference, inspect the rendered image and confirm:

- the first row of album covers begins at the same top content position;
- no folder frame is present;
- no Rescan, Cancel, or scan-summary content is present;
- the terminal scaffold status remains visible.

Only if the inspected rendering correctly reflects the approved design, regenerate the affected root reference with `updateDebugScreenshotTest`. Do not update unrelated references.

**Verify:** Run:

```powershell
.\gradlew.bat validateDebugScreenshotTest
```

If a reviewed reference update was necessary, run validation again and confirm it passes.

### Task 4: Run regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–3

**Test first:**

Run targeted source checks before the final build:

```powershell
rg -n '"RESCAN"|"CANCEL"|scanState\.report|source\?\.displayName' app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt
rg -n 'RESCAN LIBRARY|onRescan' app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt
rg -n 'status = viewModel\.scanState\.message' app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt
```

The first command should return no Library-frame remnants. The second and third commands must confirm the Settings action and shared scan status remain wired.

**Implementation:**

Fix only failures introduced by this feature. Do not modify scanning logic, ViewModel state, library setup, navigation, album-grid layout, Settings behavior, data models, or repositories.

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

If an Android device is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Confirm only the planned Library UI, tests, screenshot coverage, approved design, and this plan are changed.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] The Library destination renders the album grid directly.
- [ ] The source-folder frame, Rescan, Cancel, and scan-summary content are absent from Library.
- [ ] Settings still exposes and invokes Rescan Library.
- [ ] Shared scaffold scan messages remain wired.
- [ ] Scanning behavior, setup, navigation, and album-grid styling are unchanged.
- [ ] Compose tests compile and pass when a device is available.
- [ ] Screenshot validation, unit tests, Android-test assembly, lint, and debug assembly pass.
- [ ] No unplanned files are modified.
