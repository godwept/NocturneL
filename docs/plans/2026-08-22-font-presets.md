# Font Presets Implementation Plan

**Date:** 2026-08-22  
**Design doc:** `docs/specs/2026-08-22-font-presets-design.md`  
**Status:** Ready for review

## Overview

Add four offline font presets—Classic, Mainframe, Pixel, and Modern—to the Android app. A compact Settings control will cycle and immediately persist the selected display/body pairing, the root Compose theme will update every screen, Settings will remain scrollable on constrained displays, font resources and OFL notices will ship in the APK, deterministic screenshot coverage will exercise every preset, and only the website's Settings manual will be updated.

## Tasks

### Task 1: Define the stable font-preset model (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/FontPresetTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/FontPreset.kt`

**Test first:**

Create `FontPresetTest` and assert:

```kotlin
assertEquals(listOf("classic", "mainframe", "pixel", "modern"), FontPreset.entries.map { it.persistedValue })
assertEquals(listOf("CLASSIC", "MAINFRAME", "PIXEL", "MODERN"), FontPreset.entries.map { it.label })
assertEquals(FontPreset.MAINFRAME, FontPreset.CLASSIC.next())
assertEquals(FontPreset.PIXEL, FontPreset.MAINFRAME.next())
assertEquals(FontPreset.MODERN, FontPreset.PIXEL.next())
assertEquals(FontPreset.CLASSIC, FontPreset.MODERN.next())
assertEquals(FontPreset.CLASSIC, FontPreset.fromPersisted(null))
assertEquals(FontPreset.CLASSIC, FontPreset.fromPersisted("unknown"))
assertEquals(FontPreset.MODERN, FontPreset.fromPersisted("modern"))
```

Run the focused test and confirm compilation fails because `FontPreset` does not exist.

**Implementation:**

Add an enum in the theme package with entries in the authoritative cycle order: `CLASSIC("classic", "CLASSIC")`, `MAINFRAME("mainframe", "MAINFRAME")`, `PIXEL("pixel", "PIXEL")`, and `MODERN("modern", "MODERN")`. Add `next()`, `DEFAULT = CLASSIC`, and `fromPersisted(value)` using exact stable-value matching rather than enum names or ordinals.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.theme.FontPresetTest"`. All assertions pass.

---

### Task 2: Add a failing bundled-resource and license contract (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/FontResourceContractTest.kt`

**Test first:**

Create a JVM file contract that expects these non-empty resources under `src/main/res`:

```text
font/oxanium_variable.ttf
font/ibm_plex_mono_regular.ttf
font/press_start_2p_regular.ttf
font/space_mono_regular.ttf
font/space_mono_bold.ttf
raw/ofl_oxanium.txt
raw/ofl_ibm_plex_mono.txt
raw/ofl_press_start_2p.txt
raw/ofl_space_mono.txt
```

For each TTF, assert `isFile` and a length greater than 1,000 bytes. For each notice, assert `isFile` and that its text contains `SIL OPEN FONT LICENSE Version 1.1`. Retain the existing VT323 and Share Tech Mono files and include them in the same existence/license assertions. Run the test and confirm it fails only for the nine new resources.

**Implementation:** None in this task; leave the red test in place for Task 3.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.theme.FontResourceContractTest"` and record the expected missing-resource failures.

---

### Task 3: Bundle the approved font binaries and OFL notices (2–5 min)

**Files:** `app/src/main/res/font/oxanium_variable.ttf`, `app/src/main/res/font/ibm_plex_mono_regular.ttf`, `app/src/main/res/font/press_start_2p_regular.ttf`, `app/src/main/res/font/space_mono_regular.ttf`, `app/src/main/res/font/space_mono_bold.ttf`, `app/src/main/res/raw/ofl_oxanium.txt`, `app/src/main/res/raw/ofl_ibm_plex_mono.txt`, `app/src/main/res/raw/ofl_press_start_2p.txt`, `app/src/main/res/raw/ofl_space_mono.txt`

**Test first:** Use the failing `FontResourceContractTest` from Task 2.

**Implementation:**

Download the unmodified files from the official Google Fonts repository and save them under the exact Android-safe destination names above:

```text
https://raw.githubusercontent.com/google/fonts/main/ofl/oxanium/Oxanium%5Bwght%5D.ttf
https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/IBMPlexMono-Regular.ttf
https://raw.githubusercontent.com/google/fonts/main/ofl/pressstart2p/PressStart2P-Regular.ttf
https://raw.githubusercontent.com/google/fonts/main/ofl/spacemono/SpaceMono-Regular.ttf
https://raw.githubusercontent.com/google/fonts/main/ofl/spacemono/SpaceMono-Bold.ttf
```

Copy each family's unmodified `OFL.txt` from its corresponding `ofl/<family>/OFL.txt` directory into the named `res/raw` file. Do not add italic or unused weights and do not modify or rename the internal font family names.

**Verify:** Re-run the focused resource contract, then run `./gradlew.bat assembleDebug` to confirm Android resource packaging accepts every filename and font.

---

### Task 4: Build complete typography mappings for all four presets (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/TypographyTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Typography.kt`

**Test first:**

Create `TypographyTest` for an internal `typographyFor(FontPreset)` function. For every preset, assert that all `display*`, `headline*`, and `title*` roles share its expected display `FontFamily`, while all `body*` and `label*` roles—including `labelSmall`—share its expected body family. Assert the exact resources/weights:

```text
Classic:   vt323_regular / share_tech_mono_regular
Mainframe: oxanium_variable Regular / ibm_plex_mono_regular
Pixel:     press_start_2p_regular / space_mono_regular
Modern:    space_mono_bold Bold / ibm_plex_mono_regular
```

Also assert that Classic preserves the current explicit sizes, line heights, and letter spacing for `displayLarge`, `headlineLarge`, `titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium`, `labelLarge`, and `labelMedium`. Confirm the test fails before changing the fixed typography object.

**Implementation:**

Replace the singleton mapping with `internal fun typographyFor(preset: FontPreset): Typography`. Build the five new `FontFamily` values from `R.font`; mark Space Mono Bold with `FontWeight.Bold`. Start from `Typography()` defaults, replace the font family on every one of its 15 roles according to the display/body split, and then apply the existing eight explicit Classic metrics uniformly to the corresponding roles for every preset. Do not tune per-preset metrics yet; screenshot review is the approved point for narrowly targeted clipping fixes.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.theme.TypographyTest"` and the existing `TerminalTokensTest`.

---

### Task 5: Make the Compose theme preset-aware (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/ThemeWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt`

**Test first:**

Add a source-contract test that reads `Theme.kt` and requires both `fontPreset: FontPreset = FontPreset.DEFAULT` and `typography = typographyFor(fontPreset)`. Run it and confirm it fails against the fixed `NocturneLTypography` wiring.

**Implementation:**

Change `NocturneLTheme` to accept `fontPreset` before `content`, defaulting to Classic for existing previews and tests. Pass `typographyFor(fontPreset)` to `MaterialTheme`; leave colors and `LocalContentColor` unchanged.

**Verify:** Run the focused theme and typography JVM tests. Existing no-argument `NocturneLTheme { ... }` call sites still compile with Classic.

---

### Task 6: Persist and restore font presets (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`

**Test first:**

Add instrumentation cases using unique preference names that prove:

- a missing `font_preset` key returns `FontPreset.CLASSIC`;
- `setFontPreset(FontPreset.PIXEL)` updates the repository flow and a newly constructed repository restores Pixel;
- the raw stored value is exactly `"pixel"`;
- an unknown string and a wrong-typed integer both fall back to Classic without altering unrelated keys.

Run the focused connected test when a device/emulator is available, or at minimum compile it with `assembleDebugAndroidTest`; it must fail before repository support exists.

**Implementation:**

Add a `_fontPreset` `MutableStateFlow`, public read-only `fontPreset`, `setFontPreset(preset)`, and private key `FONT_PRESET = "font_preset"`. Read strings inside `runCatching`, resolve through `FontPreset.fromPersisted`, persist only `preset.persistedValue`, and update the in-memory flow after `apply()`.

**Verify:** Run `./gradlew.bat assembleDebugAndroidTest`. If a device is attached, run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest`.

---

### Task 7: Expose cycling through Settings state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`

**Test first:**

Add a test that clears `terminal_preferences`, constructs `SettingsViewModel`, asserts Classic, calls `cycleFontPreset()` four times, and observes Mainframe, Pixel, Modern, then Classic. Cycle once more and construct a fresh ViewModel to assert Mainframe restoration. Keep the existing effects, visualizer, sort, and view assertions intact.

**Implementation:**

Add `fontPreset: FontPreset = FontPreset.DEFAULT` to `TerminalSettingsState`. Thread `repository.fontPreset.value` through every `resolve` call and return it in the state. Add `cycleFontPreset()` that calculates `state.value.fontPreset.next()`, persists it, and resolves a new state while preserving every unrelated field.

**Verify:** Run `./gradlew.bat assembleDebugAndroidTest`; when available, run the focused connected `SettingsViewModelTest` and confirm all old and new cases pass.

---

### Task 8: Add the compact cycling control and scrollable Settings layout (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`

**Test first:**

Add Compose tests that:

- render `FontPreset.PIXEL` and assert `FONT PRESET: PIXEL` and `[ NEXT ]` are displayed;
- click `[ NEXT ]` and assert the callback runs exactly once;
- constrain the screen height, locate `[ NEXT ]` with `performScrollTo()`, and then assert it is displayed and clickable, proving a vertical scrolling ancestor exists.

Run the focused test on a device/emulator and confirm the selector tests fail before implementation.

**Implementation:**

Add `onCycleFontPreset: () -> Unit` to `SettingsScreen`. Apply `rememberScrollState()` and `verticalScroll()` to the padded Settings `Column`. Immediately after `CRT EFFECTS`, add a compact `Row` containing `Text("FONT PRESET: ${state.fontPreset.label}")` and `BracketButton("NEXT", onCycleFontPreset)`. Keep the existing terminal control components, ordering, confirmations, and notices unchanged.

**Verify:** Run the focused connected `SettingsScreenTest`; also run `assembleDebugAndroidTest` to compile every call site that will be updated next.

---

### Task 9: Wire live app-wide theme updates (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/FontPresetWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/MainActivity.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

Add a source-contract test that requires:

```text
MainActivity: obtains SettingsViewModel inside setContent, collects state, calls NocturneLTheme(settings.fontPreset), and passes the same ViewModel to NocturneLApp
NocturneLApp: passes settingsViewModel::cycleFontPreset to SettingsScreen
TerminalPreview: accepts a FontPreset argument defaulting to Classic and passes it to NocturneLTheme
```

Run the test and confirm it fails on all three missing connections.

**Implementation:**

In `MainActivity.setContent`, obtain `SettingsViewModel` with the existing lifecycle Compose `viewModel()` API, collect its state, wrap the app with `NocturneLTheme(settings.fontPreset)`, and inject that same ViewModel into `NocturneLApp`. This places the settings observation above `MaterialTheme` without duplicating a repository. In the Settings destination, pass `settingsViewModel::cycleFontPreset`. Update `TerminalPreview(fontPreset: FontPreset = FontPreset.DEFAULT, ...)` and all direct `SettingsScreen` preview calls for the new callback while preserving Classic as the default.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.FontPresetWiringTest"` followed by `./gradlew.bat assembleDebug assembleDebugAndroidTest`.

---

### Task 10: Document the selector on the Settings manual only (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/settings.md`

**Test first:**

Extend the `settings.md` `ManualContract` requirements with `FONT PRESET`, `CLASSIC`, `MAINFRAME`, `PIXEL`, `MODERN`, `immediately`, and `offline`. Run `ProductSiteContractTest` and confirm it fails against the current manual.

**Implementation:**

In “Appearance and motion,” add one concise paragraph explaining that `FONT PRESET` cycles Classic, Mainframe, Pixel, and Modern; the selection changes the entire app immediately, persists automatically, and uses fonts bundled for offline operation. Do not edit `docs/index.md`, homepage assets, or website screenshots.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"`.

---

### Task 11: Add deterministic screenshot coverage for every preset (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, new references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/`

**Test first:**

Add four `@PreviewTest` Settings previews at 412 x 915 dp named `Settings font Classic`, `Settings font Mainframe`, `Settings font Pixel`, and `Settings font Modern`. Pass the matching preset both to `TerminalPreview` and `TerminalSettingsState` so the control label and active theme agree. Add one Pixel `QueueEditorScreen` preview using the existing long-title fixtures, and one Pixel Settings preview at 320 x 568 dp with `fontScale = 1.3f` to cover constrained scrolling/readability. Run screenshot validation and confirm it reports only the new missing references.

**Implementation:**

Run `updateDebugScreenshotTest`, then inspect each generated image. Confirm preset identity, readable labels, no clipping or overlap, ellipsis on dense queue text, and reachability of Settings content. If a font clips, make the smallest preset-specific line-height or letter-spacing adjustment in `Typography.kt`, add the matching assertion to `TypographyTest`, and regenerate only affected new references. Do not update existing Classic references unless the deliberate `labelSmall` family change affects one; inspect and retain only that approved difference.

**Verify:** Run `./gradlew.bat updateDebugScreenshotTest`, inspect the new PNGs, then run `./gradlew.bat validateDebugScreenshotTest`.

---

### Task 12: Run the complete regression gate and audit the diff (2–5 min)

**Files:** All files changed by Tasks 1–11; no new implementation files

**Test first:** No new test. This task executes the full accumulated suite after all focused red/green slices.

**Implementation:**

Run:

```powershell
./gradlew.bat testDebugUnitTest assembleDebugAndroidTest validateDebugScreenshotTest lintRelease assembleDebug
```

If a device or emulator is attached, also run `./gradlew.bat connectedDebugAndroidTest`. Fix only failures caused by the font-preset feature. Use `git status --short` and `git diff --stat` to confirm the diff contains only the approved design/plan, font model and resources, theme/settings wiring, focused tests and screenshot references, and `docs/_manual/settings.md`.

**Verify:** Every applicable command passes; Classic remains the default; cycling updates the whole visible app immediately and wraps in the approved order; relaunch restores the last preset; airplane-mode use needs no font download; no homepage or unrelated files changed.

## Definition of Done

- [ ] All tasks completed in order with tests written before their implementations.
- [ ] Four presets cycle in the order Classic → Mainframe → Pixel → Modern → Classic.
- [ ] The selected pairing updates every Material typography role immediately and persists across restarts.
- [ ] Missing, malformed, and wrong-typed preferences safely resolve to Classic.
- [ ] All required TTF files and unmodified OFL notices ship locally; no runtime network access is introduced.
- [ ] Settings remains usable on constrained screens and at enlarged Android font scale.
- [ ] The Settings manual, and no other website page, documents the feature.
- [ ] Unit tests, Android-test assembly/instrumentation, screenshot validation, lint, and debug assembly pass.
- [ ] No unplanned or unrelated files are modified.
- [ ] The implementation does exactly what the approved design specifies and nothing more.
