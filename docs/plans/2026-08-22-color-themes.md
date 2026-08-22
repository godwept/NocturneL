# Color Themes Implementation Plan

**Date:** 2026-08-22  
**Design doc:** `docs/specs/2026-08-22-color-themes-design.md`  
**Status:** Ready for review

## Overview

Add five bundled semantic color themes—Green Terminal, Amber Terminal, Blue Terminal, '80s Synthwave, and '90s Neon—to the Android app. The selected theme will cycle from Settings, apply immediately, persist independently from font and effects preferences, drive Material and custom-drawn UI colors, preserve album artwork pixels, and give '90s Neon an effects-controlled border glow. Green Terminal remains byte-for-byte equivalent to the current palette, and the website Settings manual will document the complete behavior.

## Approved Initial Palette

Use these ARGB values as the initial implementation. Screenshot review may make small aesthetic adjustments, but changes must retain the contrast tests and Green Terminal values exactly.

| Role | Green Terminal | Amber Terminal | Blue Terminal | '80s Synthwave | '90s Neon |
|---|---:|---:|---:|---:|---:|
| background | `FF000000` | `FF000000` | `FF00040A` | `FF090019` | `FF000B12` |
| panel | `FF050805` | `FF0A0700` | `FF020B14` | `FF16062B` | `FF061722` |
| textPrimary | `FF00FF41` | `FFFFB000` | `FF5CC8FF` | `FFF6EEFF` | `FFE9FDFF` |
| textSecondary | `FF00B32D` | `FFD18D00` | `FF2D9FD6` | `FF54E7FF` | `FF36F1CD` |
| textMuted | `FF008020` | `FF9A6800` | `FF21749E` | `FFA57BC7` | `FF568F9A` |
| border | `FF00B32D` | `FFD18D00` | `FF2D9FD6` | `FF9D4EDD` | `FF167C91` |
| borderEmphasis | `FF00FF41` | `FFFFB000` | `FF5CC8FF` | `FFFF4FD8` | `FF28D7FE` |
| accentPrimary | `FFFFB000` | `FFFFE082` | `FFA9E7FF` | `FFFF4FD8` | `FFFF2BD6` |
| accentSecondary | `FF39FF7C` | `FFFFD166` | `FF80D8FF` | `FF54E7FF` | `FFB6FF00` |
| selection | `FFFFB000` | `FFFFE082` | `FFA9E7FF` | `FFFFB347` | `FFB6FF00` |
| warning | `FFFFB000` | `FFFFD166` | `FFFFB000` | `FFFFC857` | `FFFFB000` |
| error | `FFFF3030` | `FFFF4040` | `FFFF405A` | `FFFF4D6D` | `FFFF3864` |
| visualizerPrimary | `FF00FF41` | `FFFFB000` | `FF5CC8FF` | `FFFF4FD8` | `FF28D7FE` |
| visualizerSecondary | `FF008020` | `FF9A6800` | `FF21749E` | `FF9D4EDD` | `FFFF2BD6` |
| visualizerPeak | `FF39FF7C` | `FFFFE082` | `FFA9E7FF` | `FFFFB347` | `FFB6FF00` |
| scanlineTint | `FF008020` | `FF9A6800` | `FF21749E` | `FF7B3FB2` | `FF166A78` |
| glowColor | `FF00FF41` | `FFFFB000` | `FF5CC8FF` | `FFFF4FD8` | `FFFF2BD6` |
| glowStrength | `0.00` | `0.00` | `0.00` | `0.00` | `0.24` |

Use `scanlineShadowAlpha = 0.48` and `scanlineTintAlpha = 0.12` for all five initial palettes. Placeholder accent lists are:

```text
Green:     00E676, 00BFA5, 76FF03, 64DD17
Amber:     FFC107, FF9800, FFD54F, FFB300
Blue:      29B6F6, 00ACC1, 40C4FF, 2979FF
Synthwave: FF4FD8, 54E7FF, 9D4EDD, FFB347
Neon:      28D7FE, FF2BD6, B6FF00, 36F1CD
```

## Tasks

### Task 1: Define the stable theme-preset model (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/ColorThemePresetTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/ColorThemePreset.kt`

**Test first:**

Create `ColorThemePresetTest` and assert the authoritative entries, stable values, labels, cycle order, and fallback:

```kotlin
assertEquals(
    listOf("green_terminal", "amber_terminal", "blue_terminal", "80s_synthwave", "90s_neon"),
    ColorThemePreset.entries.map { it.persistedValue },
)
assertEquals(
    listOf("GREEN TERMINAL", "AMBER TERMINAL", "BLUE TERMINAL", "'80S SYNTHWAVE", "'90S NEON"),
    ColorThemePreset.entries.map { it.label },
)
assertEquals(ColorThemePreset.AMBER_TERMINAL, ColorThemePreset.GREEN_TERMINAL.next())
assertEquals(ColorThemePreset.BLUE_TERMINAL, ColorThemePreset.AMBER_TERMINAL.next())
assertEquals(ColorThemePreset.SYNTHWAVE_80S, ColorThemePreset.BLUE_TERMINAL.next())
assertEquals(ColorThemePreset.NEON_90S, ColorThemePreset.SYNTHWAVE_80S.next())
assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.NEON_90S.next())
assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.fromPersisted(null))
assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.fromPersisted("unknown"))
assertEquals(ColorThemePreset.NEON_90S, ColorThemePreset.fromPersisted("90s_neon"))
```

Run the test and confirm compilation fails because the model does not exist.

**Implementation:**

Create the enum in the exact tested order with `persistedValue` and `label` constructor fields. Add `next()`, `DEFAULT = GREEN_TERMINAL`, and exact-string `fromPersisted`; do not persist enum names or ordinals.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.theme.ColorThemePresetTest"`.

---

### Task 2: Define complete semantic palettes and contrast contracts (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/theme/Color.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/theme/TerminalTokensTest.kt`

**Test first:**

Replace the fixed-token assertions with tests for `TerminalPalette` and `paletteFor(ColorThemePreset)`. Assert every ARGB value, placeholder list, scanline alpha, and glow strength from the approved table. Explicitly assert Green Terminal retains the current values for background, panel, primary/secondary/muted text, emphasis, selection, warning, error, visualizer colors, scanlines, and placeholders.

Add a local WCAG relative-luminance/contrast helper and require:

```text
textPrimary vs background and panel: >= 4.5
textSecondary vs background and panel: >= 4.5
textMuted vs background and panel: >= 3.0
warning and error vs background and panel: >= 4.5
selection vs background and panel: >= 3.0
```

Assert each placeholder list has exactly four colors and that only `NEON_90S` has a non-zero glow strength.

**Implementation:**

In `Color.kt`, introduce an immutable `TerminalPalette` data class with every role from the design plus `scanlineShadowAlpha`, `scanlineTintAlpha`, `glowColor`, and `glowStrength`. Add one complete private palette per preset using the approved values and an exhaustive `internal fun paletteFor(preset)` mapping. Keep the legacy globals temporarily so production sources compile until Task 13 removes the final imports.

**Verify:** Run the focused `TerminalTokensTest` and `ColorThemePresetTest`.

---

### Task 3: Make the Compose theme expose semantic tokens (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/theme/ThemeWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt`

**Test first:**

Extend `ThemeWiringTest` to require a pure `materialColorsFor(palette)` mapping and assert these Material roles: primary/textPrimary, secondary/textSecondary, tertiary/accentPrimary, background/background, surface and surfaceVariant/panel, on-background and on-surface/textPrimary, on-surface-variant/textSecondary, error/error, outline/borderEmphasis, and outlineVariant/border. Retain the typography assertions. Add source-contract assertions for defaulted `colorTheme` and `effectsEnabled` parameters and for providing both palette and effective-effects composition locals.

**Implementation:**

Replace the singleton `NocturneLColors` with `internal fun materialColorsFor(palette: TerminalPalette) = darkColorScheme(...)`. Add private composition locals that fail clearly if used outside the theme, and expose them through `TerminalTheme.palette` and `TerminalTheme.effectsEnabled` read-only composable properties. Keep `fontPreset` as the first parameter for source compatibility, then add `colorTheme: ColorThemePreset = DEFAULT` and `effectsEnabled: Boolean = true`. Resolve the palette once, supply it and the effects flag, set `LocalContentColor` to `textPrimary`, and keep `typographyFor(fontPreset)`.

**Verify:** Run `ThemeWiringTest`, `TerminalTokensTest`, and `TypographyTest`.

---

### Task 4: Add a layout-neutral themed border and glow primitive (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/components/TerminalBorderStyleTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalBorder.kt`

**Test first:**

Test a pure `terminalBorderStyle(palette, effectsEnabled, emphasized)` resolver. Require no glow when effects are off or `glowStrength == 0`, Neon normal borders to use half strength (`0.12`), Neon emphasized borders to use full strength (`0.24`), and core border color to remain unchanged in all cases.

**Implementation:**

Add a composable `Modifier.terminalBorder(color, emphasized = false, width = TerminalDimensions.border)` that reads `TerminalTheme`. Draw glow inside the existing bounds with two rectangular strokes (6 px at half the resolved alpha, then 3 px at full resolved alpha) followed by the crisp requested core border. Do not add padding, elevation, blur APIs, animation, or text rendering; the modifier must leave measurement and touch targets unchanged.

**Verify:** Run `TerminalBorderStyleTest` and `./gradlew.bat assembleDebug`.

---

### Task 5: Persist and restore the selected color theme (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`

**Test first:**

Add instrumentation cases with unique preference names proving that a missing `color_theme` returns Green Terminal, setting Neon updates the flow and stores exactly `"90s_neon"`, a fresh repository restores Neon, and unknown or wrong-typed values fall back to Green without changing `font_preset` or `effects_enabled`.

**Implementation:**

Add `_colorTheme`, public `colorTheme`, `setColorTheme`, and `COLOR_THEME = "color_theme"`, following the existing `FontPreset` pattern. Read the string inside `runCatching`, resolve with `ColorThemePreset.fromPersisted`, persist only the stable value, and update the in-memory flow after `apply()`.

**Verify:** Run `./gradlew.bat assembleDebugAndroidTest`; with a device, run the focused `TerminalPreferencesRepositoryTest`.

---

### Task 6: Expose theme cycling through Settings state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`

**Test first:**

Add a test that clears `terminal_preferences`, asserts Green, cycles through Amber, Blue, Synthwave, Neon, and back to Green, then cycles once more and constructs a fresh ViewModel to verify Amber restoration. Set a non-default font before cycling and assert every theme change preserves the font, effects flag, library modes, and visualizer offset.

**Implementation:**

Add `colorTheme: ColorThemePreset = DEFAULT` to `TerminalSettingsState`. Thread `repository.colorTheme.value` through every `resolve` invocation and copy it into the result. Add `cycleColorTheme()` using `next()`, repository persistence, and a full state resolution that preserves all unrelated fields.

**Verify:** Compile instrumentation tests, then run the focused connected `SettingsViewModelTest` when a device is available.

---

### Task 7: Add the compact Settings theme selector (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`

**Test first:**

Render a Synthwave state and assert `COLOR THEME: '80S SYNTHWAVE` plus its own `[ NEXT ]` action are displayed. Use indexed node selection or a dedicated semantic content description so the theme button can be clicked independently of the font button, and assert `onCycleColorTheme` fires exactly once. In the constrained-height test, scroll to and click both selectors.

**Implementation:**

Add `onCycleColorTheme: () -> Unit`. Immediately after CRT Effects and before Font Preset, add the same responsive `BoxWithConstraints` row pattern used by the font selector. Display `COLOR THEME: ${state.colorTheme.label}` and a compact `BracketButton("NEXT", ..., contentDescription = "Next color theme")`; do not add previews, a new screen, or an Apply action.

**Verify:** Run `assembleDebugAndroidTest`; with a device, run the focused `SettingsScreenTest`.

---

### Task 8: Wire one shared theme state above the entire app (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/FontPresetWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/MainActivity.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Rename or extend the wiring contract to require named arguments for `fontPreset = settings.fontPreset`, `colorTheme = settings.colorTheme`, and `effectsEnabled = settings.effectiveEffectsEnabled` in the root theme, continued reuse of the same `SettingsViewModel` in `NocturneLApp`, and `onCycleColorTheme = settingsViewModel::cycleColorTheme` in Settings. Require that the playback connection remains created with `remember(context)` inside `NocturneLApp`, guarding against theme switches recreating playback state.

**Implementation:**

Pass all three settings values into `NocturneLTheme` with named arguments in `MainActivity`. Pass the new callback into `SettingsScreen`. Do not key `NocturneLApp`, playback, navigation, list state, or ViewModels by the theme.

**Verify:** Run the focused wiring test and `MainActivityTest`, then `assembleDebug`.

---

### Task 9: Theme buttons and notices (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalComponentsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalButton.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalNotice.kt`

**Test first:**

Add pure internal color resolvers where needed and test under Neon that warning/error notices return distinct `warning`/`error` roles and selected/disabled buttons use `selection` and faded `textSecondary`. Retain the existing click, toggle, uppercase-label, and disabled-action assertions.

**Implementation:**

Read `TerminalTheme.palette` once at each composable boundary. Map button selection to `selection`, disabled buttons to `textSecondary.copy(alpha = .5f)`, and notice severity to `textPrimary`/`warning`/`error`. Preserve labels, semantics, touch behavior, and layout.

**Verify:** Compile/run `TerminalComponentsTest` and run `assembleDebug`.

---

### Task 10: Theme navigation, seek bar, and scanlines (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/components/ScanlineStyleTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalNavigation.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalSeekBar.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/Scanlines.kt`

**Test first:**

Change `scanlineStyle` to accept a palette and assert it returns the active `scanlineTint`, `scanlineShadowAlpha`, and `scanlineTintAlpha` only when effects are enabled. Add pure color-mapping assertions where useful for the navigation divider and seek track/progress.

**Implementation:**

Read `TerminalTheme.palette` before each `Canvas`. Map navigation dashes and the seek track to `textSecondary`, seek progress/thumb to `borderEmphasis`, and scanline shadow/tint to palette background and `scanlineTint`. Preserve geometry, line widths, gesture handling, animation timing, semantics, and the enabled gate exactly.

**Verify:** Run `ScanlineStyleTest` and `assembleDebug`.

---

### Task 11: Theme frames, drag selections, and cover-flow emphasis (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalComponentsTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalFrame.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/components/DragReorderRow.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add stable test tags to one normal frame and the selected cover/drag state as needed. Under Neon with effects on, assert the elements remain displayed and retain their existing semantics; toggle effects off and assert no node size or position changes. Keep existing cover-flow selection and drag accessibility tests passing.

**Implementation:**

Replace direct `.border` calls with `terminalBorder`: frames use `palette.border`; dragging rows use `palette.selection` with `emphasized = true`; unselected covers use `palette.border`; selected covers use `palette.selection`, their existing 2 dp core width, and `emphasized = true`. Replace selected cover-flow text with `selection`. Preserve backgrounds, padding, scale, z-index, and gestures.

**Verify:** Run the focused component and cover-flow instrumentation tests, or at minimum `assembleDebugAndroidTest`.

---

### Task 12: Theme artwork framing and placeholders without tinting covers (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/artwork/TerminalArtworkPlaceholderTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/artwork/TerminalArtworkPlaceholder.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/artwork/RetroArtwork.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/artwork/CrtArtwork.kt`

**Test first:**

Test `accentFor(albumId, colors)` for deterministic selection, index safety, and different palette lists. Extend the source guard to reject `colorFilter`, `ColorMatrix`, or theme-dependent image transformations in `RetroArtwork`/`AsyncImage`. Require `CrtArtwork` to use semantic accent roles rather than fixed red/blue overlay colors.

**Implementation:**

Make `TerminalArtworkPlaceholder.accentFor` accept the active palette's non-empty placeholder list. In `RetroArtwork`, use that list only for the existing placeholder background and glyph; do not pass theme data into `ImageRequest` or modify decoded artwork. In `CrtArtwork`, use an emphasized `borderEmphasis` terminal border and replace red/blue overlay lines with `accentPrimary`/`accentSecondary` at the existing `.18f` alpha. Preserve cache keys and artwork fallback behavior.

**Verify:** Run the new placeholder test, `TerminalUiSourceGuardTest`, artwork JVM tests, and `assembleDebug`.

---

### Task 13: Apply semantic colors to all visualizer layers (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Update source contracts to require palette parameters in the private radar draw functions and forbid legacy fixed-color identifiers. Add a Compose test that switches the `colorTheme` state while an active visualizer remains composed, then asserts the same visualizer tag remains present, scanline behavior is unchanged, and no unavailable state appears.

**Implementation:**

Capture the active palette before entering `Canvas` and pass it into draw helpers. Map frame background/background, frame border/borderEmphasis, unavailable text/textSecondary, grid/visualizerSecondary, energy rings/textSecondary, spokes/live bars/ghosts/trails/visualizerPrimary, and peaks/transients/visualizerPeak. Use the themed border primitive for the visualizer frame. Preserve geometry, afterglow state, frame-clock lifecycle, bloom widths/alphas, and animation eligibility exactly.

**Verify:** Run visualizer JVM tests, compile/run `TerminalVisualizersTest`, and confirm the afterglow source guards still pass.

---

### Task 14: Remove legacy green globals and enforce semantic-only UI colors (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/theme/Color.kt`, all Kotlin files under `app/src/main/java/ca/stewark/nocturnel/ui/`

**Test first:**

Add a source guard that scans UI production Kotlin outside `ui/theme/Color.kt` and fails on imports or identifier usage of `TerminalBlack`, `TerminalBlackAlt`, `Phosphor`, `PhosphorDim`, `PhosphorMuted`, `PhosphorBright`, `AlertAmber`, `TerminalError`, `TerminalText`, and `TerminalPanel`. Also reject direct `Color(...)` and `Color.Red/Blue/Green/Yellow/Black/White` usage in UI production sources, while allowing colors constructed only in the palette catalog.

**Implementation:**

Run `rg` for every forbidden symbol, migrate any remaining call site to `MaterialTheme.colorScheme` or `TerminalTheme.palette`, then delete the legacy global aliases/constants from `Color.kt`. Do not alter colors inside decoded album artwork or non-UI pixel-processing algorithms.

**Verify:** Run `TerminalUiSourceGuardTest`, `TerminalTokensTest`, and `rg -n` for the forbidden names; there must be no production UI matches outside the palette definition.

---

### Task 15: Extend the screenshot harness and cover terminal themes (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, generated references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/`

**Test first:**

Extend `TerminalPreview` with named `colorTheme` and `effectsEnabled` parameters. Keep every existing preview/reference name stable, then add failing previews for:

```text
Settings Green Terminal
Settings Amber Terminal
Settings Blue Terminal
```

Ensure each preview's `TerminalSettingsState.colorTheme` matches its theme argument.

**Implementation:**

Generate and inspect the three new references. Confirm Green existing references do not change and the Amber/Blue labels, text hierarchy, warnings, and controls remain legible.

**Verify:** Run `./gradlew.bat validateDebugScreenshotTest` and review the generated diff report with zero unexpected differences.

---

### Task 16: Cover Synthwave and Neon effect states in screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, generated references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/`

**Test first:**

Add failing previews for `Settings '80s Synthwave`, `Settings '90s Neon effects on`, and `Settings '90s Neon effects off`. Supply matching settings state and theme/effects arguments.

**Implementation:**

Generate and inspect the three references. Confirm Neon glow stays inside borders and never touches text, effects-off Neon keeps identical layout while removing glow/scanlines/bloom, and both multicolor labels and semantic states are readable. Make only small non-Green palette/glow adjustments allowed by the plan preface and update exact palette tests with any accepted change.

**Verify:** Run `validateDebugScreenshotTest` and compare the two Neon images for layout stability.

---

### Task 17: Cover dense screens and themed visualizers in screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, generated references under `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/`

**Test first:**

Add failing previews for `Now playing '80s Synthwave`, `Visualizer bands '80s Synthwave`, `Visualizer radar '90s Neon`, and `Album grid '90s Neon with Pixel font`.

**Implementation:**

Generate and inspect the four references. Confirm visualizer layers use multiple semantic colors without new flashing behavior, Pixel text remains unclipped, Neon emphasized borders glow without text glow, and album artwork/placeholder behavior remains recognizable.

**Verify:** Run `validateDebugScreenshotTest` and review the generated diff report with zero unexpected differences.

---

### Task 18: Document themes in the product manual (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/settings.md`

**Test first:**

Extend the Settings manual contract to require `COLOR THEME`, all five theme names, immediate application, automatic persistence, font independence, preserved artwork colors, and the statement that CRT Effects controls Neon glow. Run the focused test and confirm it fails on the current manual.

**Implementation:**

Add one concise paragraph under Appearance and Motion describing the selector and fixed cycle order. State that themes recolor the complete app UI and visualizers, do not recolor album covers or change fonts, save automatically, work offline, and retain core colors when CRT Effects disables scanlines/bloom/glow. Update the page description only if needed to mention appearance; do not change other manual pages or homepage copy.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"` and perform the existing narrow/desktop manual checklist review.

---

### Task 19: Run the complete regression suite and audit scope (2–5 min)

**Files:** No planned source changes; inspect the working tree only.

**Test first:** No new test in this task; this is the final verification gate after all test-first tasks are green.

**Implementation:**

Run the complete JVM suite, Android compilation, screenshot validation, and release assembly. If a device/emulator is available, run all connected tests and manually cycle all themes during active playback, navigation, and Settings interaction with CRT Effects both on and off.

**Verify:**

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebugAndroidTest
./gradlew.bat validateDebugScreenshotTest
./gradlew.bat assembleRelease
./gradlew.bat connectedDebugAndroidTest
git diff --check
git status --short
```

All available checks pass; connected tests are explicitly recorded as skipped only when no device is available. Confirm only files named by this plan plus generated screenshot references and the approved design/plan documents changed.

## Definition of Done

- [ ] All tasks completed in order with each new test observed failing before its implementation.
- [ ] All five presets cycle in the approved order, apply immediately, and restore by stable persisted identifier.
- [ ] Theme and font settings remain independent, and theme changes do not reset playback or navigation.
- [ ] Every Material and custom-drawn UI color comes from the active semantic palette.
- [ ] '90s Neon glow affects borders/emphasized elements only and disappears with effective CRT Effects.
- [ ] Album artwork pixels and cache behavior remain theme-independent.
- [ ] Green Terminal existing screenshots remain unchanged; all new screenshot references pass review.
- [ ] Website Settings documentation and its contract test describe the feature accurately.
- [ ] All available JVM, instrumentation, screenshot, debug, and release checks pass.
- [ ] No unplanned files were modified.
