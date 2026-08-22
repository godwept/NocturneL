# Color Themes Design

**Date:** 2026-08-22
**Status:** Approved

## Goal

Add five complete, persistent visual themes that recolor NocturneL's entire interface while preserving album-art colors and remaining independent from font presets.

## Success Criteria

- [ ] Settings provides a compact cycling theme control.
- [ ] Theme changes apply immediately across every screen.
- [ ] The chosen theme survives app restarts.
- [ ] Green Terminal remains the safe default for existing and new installations.
- [ ] Text, controls, borders, navigation, placeholders, visualizers, scanlines, and CRT treatments consistently use the active semantic palette.
- [ ] Amber Terminal and Blue Terminal remain restrained monochrome terminal styles.
- [ ] '80s Synthwave and '90s Neon use coordinated multicolor palettes.
- [ ] '90s Neon adds border and selected-element glow only while CRT Effects are enabled.
- [ ] Disabling CRT Effects leaves each theme's core colors intact while removing glow, scanlines, bloom, and related decoration.
- [ ] Warning and error states remain distinguishable and readable in every theme.
- [ ] Existing album-art colors and retro quantization remain unchanged.
- [ ] The website manual documents all five themes and their behavior.

## Scope

**In scope:**

- Five bundled themes: Green Terminal, Amber Terminal, Blue Terminal, '80s Synthwave, and '90s Neon.
- A compact `COLOR THEME: <NAME>  [NEXT]` control in Settings.
- Immediate app-wide application and automatic persistence.
- Semantic colors for backgrounds, panels, text levels, borders, accents, selection, warnings, errors, visualizers, scanlines, placeholders, and effects.
- Theme-aware CRT overlays and visualizer colors.
- Border and selected-element glow for '90s Neon when CRT Effects are enabled.
- Safe fallback to Green Terminal for missing or invalid saved values.
- Independence between color themes and font presets.
- Automated palette, persistence, wiring, UI, and screenshot coverage.
- Website Settings manual updates.

**Out of scope:**

- User-created or imported themes.
- A color picker or individual color customization.
- Downloadable themes.
- Automatic theme selection from album artwork or Android wallpaper.
- Automatically changing the selected font.
- Recoloring or tinting album covers.
- Theme-specific layouts, navigation, or playback behavior.
- A separate glow control; the existing CRT Effects setting governs it.
- Light-background themes in this first release.

## Design

### Theme Catalog and State

A closed `ColorThemePreset` catalog represents the five themes. Each preset has a stable persisted identifier, a user-facing label, a complete semantic palette, and effect styling values needed by scanlines, bloom, and optional glow.

The semantic palette contains roles rather than component-specific colors:

- `background` and `panel`
- `textPrimary`, `textSecondary`, and `textMuted`
- `border` and `borderEmphasis`
- `accentPrimary` and `accentSecondary`
- `selection`
- `warning` and `error`
- `visualizerPrimary`, `visualizerSecondary`, and `visualizerPeak`
- `scanlineTint`
- `artworkPlaceholderColors`
- `glowColor` and glow strength, with zero strength for themes that do not use it

Green Terminal preserves the current appearance exactly. Amber and Blue substitute coherent monochrome ranges while keeping conventional warning and error meaning. The '80s and '90s presets use multiple accent and visualizer colors but maintain high-contrast primary text.

`TerminalSettingsState` includes the active theme preset. The existing preferences repository stores its stable identifier, never an enum position, so themes can later be reordered safely. Missing, corrupted, or obsolete identifiers resolve to Green Terminal without affecting other preferences.

The selected font and color theme remain separate state fields. Any combination is valid.

### Theme Interfaces and Behavior

`NocturneLTheme` receives both the selected color theme and font preset. It derives the Material color scheme from the semantic palette and exposes the additional terminal-specific roles through a composition-local theme object.

Components use semantic roles from that object rather than importing fixed green constants. This includes custom-drawn elements such as seek bars, navigation marks, scanlines, visualizers, artwork placeholders, drag indicators, and CRT borders. Material components continue using the derived `MaterialTheme.colorScheme`.

The '80s and '90s palettes may assign different accents within a component, such as secondary visualizer bands or emphasized selections, but component structure and behavior remain common across all themes.

Effects remain a separate policy:

- Theme selection determines available colors and effect styling.
- Effective CRT Effects state determines whether scanlines, bloom, and glow are rendered.
- With effects disabled, all themed colors remain active.
- The '90s glow is limited to borders and emphasized or selected elements; text never receives a glow.
- Android reduced-motion behavior continues to suppress decorative effects through the existing effective-effects state.

Settings receives an `onCycleColorTheme` callback and displays the compact selector. Cycling wraps in this order:

`GREEN TERMINAL -> AMBER TERMINAL -> BLUE TERMINAL -> '80s SYNTHWAVE -> '90s NEON -> GREEN TERMINAL`

The website's Settings manual documents the selector, catalog, persistence, font independence, artwork preservation, and effect-toggle behavior.

### Error Handling and Edge Cases

- Missing, unreadable, or unknown saved theme identifiers fall back to Green Terminal.
- A failed preference write may leave the newly selected theme active for the current session; the next launch restores the last valid saved theme or Green Terminal.
- Every palette must meet readable contrast for primary text, secondary text, controls, and selected states. Decorative neon colors cannot become the sole indicator of warnings, errors, selection, or disabled state.
- Warning and error roles retain consistent meaning across all themes, even when that means they are not strictly monochrome in Amber or Blue Terminal.
- Glow must not alter layout size, touch targets, clipping, or text measurement. It may soften gracefully near screen edges rather than expanding a component's bounds.
- Effects-disabled and Android reduced-motion states render without scanlines, bloom, or glow while preserving palette and selection clarity.
- Multicolor visualizers may vary color by band or layer but must not introduce additional flashing or animation behavior.
- Transparent overlays and disabled controls must remain distinguishable on both the base background and panel background.
- Theme switching during playback must not recreate playback state, reset navigation, or interrupt audio.
- Album artwork pixels and retro quantization remain theme-independent; only surrounding frames, placeholders, and overlays change.
- Android-owned interfaces such as the folder picker and permission dialogs, plus launcher icons and splash assets, remain outside the app theme.
- Adding future presets requires a complete semantic palette; incomplete palettes cannot silently inherit arbitrary colors from another theme.

## Testing Strategy

- Unit-test the fixed theme catalog, cycle order, wraparound behavior, stable identifiers, and invalid-value fallback.
- Test preference restoration and persistence without disturbing font, effects, library, or visualizer settings.
- Test every semantic palette for completeness and minimum contrast across its intended background roles.
- Add wiring coverage proving the selected theme flows from settings state into the app-wide theme and that the Settings selector invokes the cycle action.
- Extend source guards so themed components and custom drawing code cannot reintroduce fixed Green Terminal color imports.
- Add component tests for warnings, errors, selections, disabled controls, visualizers, scanlines, placeholders, and border glow under representative palettes.
- Add screenshot references for Settings in all five themes and representative dense screens for Amber, Blue, Synthwave, and Neon.
- Cover '90s Neon with effects enabled and disabled, confirming that glow disappears without changing layout or text clarity.
- Verify that Green Terminal's existing screenshot references remain unchanged.
- Exercise live theme switching during playback and navigation to ensure state and audio continue uninterrupted.
- Test representative combinations of every color theme with the most layout-sensitive font preset rather than multiplying every screenshot by all font/theme combinations.
- Update the product-site contract tests and manually verify the Settings manual content on narrow and desktop layouts.
- Run the complete unit, instrumentation, and screenshot suites before considering the feature complete.

## Open Questions

None.
