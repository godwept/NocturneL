# Prominent Main Navigation Implementation Plan

**Date:** 2026-08-23
**Design doc:** docs/specs/2026-08-23-prominent-main-navigation-design.md
**Status:** Ready for review

## Overview

Move Settings from the six-item text navigation into an accessible gear button at the right edge of the `NOCTURNEL` header. Render the remaining `LIB`, `SEA`, `ART`, `PLY`, and `NOW` tabs as equal-width targets using the active theme's prominent 14sp `labelLarge` typography, preserve destination and animation behavior, cover constrained layouts and all font presets, update the manual, and commit and sync the verified change to `origin/main`.

## Tasks

### Task 1: Make the five primary tabs equal and font-aware

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalComponentsTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalNavigation.kt`

**Test first:**

- Replace `pixelFontKeepsEveryMainNavigationTabVisibleAt320Dp` with a small helper plus four tests, one for each `FontPreset`, that render `TerminalNavigation` inside a 320dp-wide box. For every preset, assert that `[LIB]`, `[SEA]`, `[ART]`, `[PLY]`, and `[NOW]` are displayed and clickable, and assert that `[ SET ]` does not exist.
- Add a constrained Pixel test using `CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, 1.3f))`; render the same 320dp navigation and assert all five primary labels remain displayed.
- Update the source-guard test to require a primary-destination collection excluding `NocturneLDestination.SETTINGS`, a full-width non-inset row, `Modifier.weight(1f)` on every tab, centered tab content, compact brackets, and the exact unmodified `MaterialTheme.typography.labelLarge`. Require that `.fontSize`, `.letterSpacing`, `.widthIn`, `Arrangement.SpaceBetween`, and `.horizontalScroll(` are absent from the navigation tab implementation.
- Run the focused JVM source guard and `assembleDebugAndroidTest`. Confirm the new assertions fail against the six-tab, forced-10sp implementation before changing production code.

**Implementation:**

- In `TerminalNavigation.kt`, define a private ordered primary-destination list by filtering `NocturneLDestination.entries` to exclude `SETTINGS`; do not change the enum or its persisted names.
- Render only that list. Let the row use the full width so enlarged Pixel labels fit, give each `BracketButton` `Modifier.weight(1f)`, center its content, use compact `[LIB]`-style brackets without decorative internal spaces, retain the minimum 48dp height supplied by `BracketButton`, and pass `MaterialTheme.typography.labelLarge` directly.
- Remove the fixed 10sp size, fixed zero letter spacing, minimum-width override, start alignment, and `Arrangement.SpaceBetween`. Preserve the dashed divider, selected color, destination callback, effects flag, and active-tab alpha pulse.
- Extract the existing 0.62-to-1.0, 700ms reversing active-navigation pulse into an `internal @Composable` helper in this file so the header Settings control can reuse exactly the same behavior in Task 3.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.TerminalUiSourceGuardTest"` and `./gradlew.bat assembleDebugAndroidTest`. If an emulator/device is available, run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.components.TerminalComponentsTest`.

---

### Task 2: Add the accessible terminal icon button

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalComponentsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalButton.kt`, `app/src/main/res/drawable/ic_settings.xml`

**Test first:**

- Add a Compose test that renders the new icon button with `R.drawable.ic_settings`, content description `Settings`, and `selected = true`.
- Locate it by content description, assert it is displayed, selected, and clickable, click it, and assert the callback ran. Add a second assertion or render state proving `selected = false` is semantically not selected.
- Run `assembleDebugAndroidTest` and confirm compilation fails only because the icon resource and icon-button API do not exist yet.

**Implementation:**

- Add `ic_settings.xml` as a 24dp by 24dp vector drawable with a 24 by 24 viewport and a single conventional sharp gear path. Give the source path an opaque neutral fill because Compose will apply the active semantic tint; do not add Material Icons or any other dependency.
- Add `TerminalIconButton` beside the existing bracket controls. Accept a `@DrawableRes` icon resource, content description, callback, modifier, enabled flag, and selected flag.
- Match `BracketButton` behavior: use `TerminalTheme.palette.textSecondary` at 50% alpha when disabled, `palette.selection` when selected, and `palette.textPrimary` otherwise; use a no-indication `Role.Button` click target; expose both the supplied content description and `selected` semantics; enforce at least `TerminalDimensions.minimumTouchTarget` in both dimensions.
- Render the vector through `Icon(painterResource(...), contentDescription = null, tint = resolvedColor)` at 24dp so semantics occur once on the parent and the visible glyph stays compact inside the 48dp target.

**Verify:** Run `./gradlew.bat assembleDebugAndroidTest`; when a device/emulator is available, run the focused `TerminalComponentsTest` and confirm both selected and click behavior pass.

---

### Task 3: Put Settings in the application header

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalComponentsTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalScaffold.kt`

**Test first:**

- Add a Compose test that renders `TerminalScaffold` at 320dp with `selected = SETTINGS`, locates `Settings` by content description, asserts it is displayed/selected/clickable, clicks it, and verifies the callback receives `NocturneLDestination.SETTINGS`.
- Add a second render/test with `selected = LIBRARY` and assert the Settings control is not selected while `[ LIB ]` is displayed.
- Extend the source guard to require a full-width header `Row`, `Alignment.CenterVertically`, `Arrangement.SpaceBetween`, the `NOCTURNEL` title, `TerminalIconButton` using `R.drawable.ic_settings`, and the Settings description. Require the icon modifier to apply the shared navigation pulse only when `selected == SETTINGS`.
- Run the focused JVM guard and `assembleDebugAndroidTest`; confirm failure before editing the scaffold.

**Implementation:**

- Replace the separately padded title with a `Row(Modifier.fillMaxWidth().padding(horizontal = TerminalDimensions.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween)`.
- Keep `NOCTURNEL` at the left with its current `titleLarge` style and primary color. Place `TerminalIconButton(R.drawable.ic_settings, "Settings", ...)` at the right; its callback must invoke `onSelected(NocturneLDestination.SETTINGS)`.
- Set the icon's selected flag from `selected == NocturneLDestination.SETTINGS`. Apply `graphicsLayer` alpha using the shared navigation pulse only while Settings is selected; leave the icon fully opaque otherwise. This supplies the same animated selection when effects are enabled and the same steady selection when disabled.
- Leave the `TerminalScaffold` public signature, content/notice placement, safe-drawing padding, background, scanlines, and destination callback contract unchanged.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.TerminalUiSourceGuardTest"` and `./gradlew.bat assembleDebugAndroidTest`; run the focused connected Compose test when a device/emulator is available.

---

### Task 4: Update the navigation manual contract and copy

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/getting-started.md`

**Test first:**

- Change the getting-started `ManualContract` requirements from `SET` to phrases that prove the new structure: `five labels`, `Settings`, and `gear` alongside the unchanged `LIB`, `SEA`, `ART`, `PLY`, and `NOW` tokens.
- Add an assertion in the manual accuracy test that the getting-started source no longer contains `six labels` or a `**SET** —` navigation entry.
- Run the focused contract test and confirm it fails against the current manual.

**Implementation:**

- Rewrite only the "Learn the main navigation" passage. Describe the five abbreviated primary tabs below the header, retain their existing destination descriptions, and explain that the gear at the top right opens Settings for rescanning, appearance, privacy, and related preferences.
- Replace the inaccurate "compact bottom navigation" wording. Keep the existing statement that selecting a destination closes nested album, artist, playlist-picker, or queue-editor state.
- Do not change other manual chapters or the website's own global navigation.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"`.

---

### Task 5: Add deterministic navigation-header screenshot previews

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

- Replace the existing standalone `NavigationPixelFontPreview` with a private header-preview helper that renders an empty `TerminalScaffold` at a deterministic destination with effects disabled.
- Add four `@PreviewTest` functions at 320dp by 112dp for Classic, Mainframe, Pixel, and Modern, passing the matching `FontPreset`. Set the Pixel preview's `fontScale` to 1.3 to cover the most constrained approved case.
- Add a fifth 320dp by 112dp preview with `selected = SETTINGS` so the gear's static selected treatment is captured.
- Run `./gradlew.bat validateDebugScreenshotTest`. Confirm the report contains the five missing navigation-header references plus expected mismatches for existing scaffold-based previews (`Root effects off`, `Cover flow`, and `Cover flow effects off`) and no unrelated differences.

**Implementation:** The preview declarations are the test fixtures. Adjust only preview height if the 112dp frame demonstrably clips safe-drawing/header/navigation content; if changed, use the same smallest sufficient height for all five previews and document that choice in the commit diff.

**Verify:** Run `./gradlew.bat compileDebugScreenshotTestKotlin` if available; otherwise run `./gradlew.bat updateDebugScreenshotTest --dry-run` to prove the preview source compiles and the task resolves.

---

### Task 6: Regenerate and inspect only affected screenshot references

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:** Run `./gradlew.bat validateDebugScreenshotTest` again immediately before regeneration. Record the exact expected missing/stale filenames and stop if any unrelated reference differs.

**Implementation:**

- Run `./gradlew.bat updateDebugScreenshotTest`.
- Inspect the five new navigation-header images and the updated `Root effects off`, `Cover flow`, and `Cover flow effects off` images. Confirm the gear is at the far right of the title row; all five abbreviated tabs are larger, centered, evenly spaced, and unclipped; the Pixel 1.3x preview remains readable; Settings selection is visible; and screen content below the navigation has no overlap.
- Remove only the superseded standalone `Navigation Pixel font 320dp` reference after resolving its exact generated path. Retain only references produced by the approved preview changes and the three existing scaffold previews affected by the deliberate shared-header change.

**Verify:** Run `./gradlew.bat validateDebugScreenshotTest`; all screenshot references must pass with zero unexpected differences.

---

### Task 7: Run the complete quality gate

**Files:** All files changed in Tasks 1–6, plus `docs/specs/2026-08-23-prominent-main-navigation-design.md` and `docs/plans/2026-08-23-prominent-main-navigation.md`

**Test first:** Run `git status --short`, `git diff --check`, and `git diff --stat`. Confirm the diff contains only the approved design/plan, navigation components, local settings vector, focused tests, getting-started documentation, preview source, and intended reference PNGs.

**Implementation:** Fix only failures introduced by this feature. Do not alter navigation state, screen content, typography presets, themes, unrelated controls, dependencies, or unrelated screenshot references.

**Verify:** Run:

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebugAndroidTest
./gradlew.bat validateDebugScreenshotTest
./gradlew.bat lintRelease
./gradlew.bat assembleRelease
```

If `adb devices` shows an available device/emulator, also run `./gradlew.bat connectedDebugAndroidTest`. Finish with `git diff --check` and a final visual review of the affected screenshots.

---

### Task 8: Commit and sync the completed feature

**Files:** The exact reviewed files from Tasks 1–7

**Test first:** Run `git status --short`, `git diff --check`, and `git diff --stat` once more. Confirm no build output, local configuration, unrelated user work, or unexpected screenshots will be staged.

**Implementation:**

- Stage only the approved design, plan, source, resource, test, documentation, and screenshot-reference paths.
- Commit with the message `Improve primary navigation prominence`.
- Fetch `origin` and confirm the local branch is `main`. If `origin/main` advanced, rebase the feature commit onto it and rerun the complete quality gate after resolving only in-scope conflicts.
- Push the verified `main` branch to `origin`.

**Verify:** Run `git status --short --branch` and confirm the worktree is clean and `main` is synchronized with `origin/main`. Compare `git rev-parse HEAD` with `git ls-remote origin refs/heads/main`; the commit IDs must match.

## Definition of Done

- [ ] All tasks completed in order with tests written before their corresponding production/documentation changes
- [ ] `LIB`, `SEA`, `ART`, `PLY`, and `NOW` are the only main tabs and each receives equal width
- [ ] Tabs use prominent 14sp theme `labelLarge` typography and fit every font preset at 320dp, including the 1.3x constrained case
- [ ] The top-right Settings gear has a 48dp target, correct semantics, navigation behavior, theme colors, and selected pulse/static treatment
- [ ] Existing navigation state, nested-state clearing, content, themes, and effects behavior remain unchanged
- [ ] Getting-started documentation describes the five tabs and Settings gear accurately
- [ ] Focused tests, complete JVM tests, Android-test compilation, screenshot validation, release lint, and release assembly pass
- [ ] Connected tests pass when a device/emulator is available
- [ ] Only planned files and reviewed screenshot references are committed
- [ ] `main` is committed and synchronized with `origin/main`
