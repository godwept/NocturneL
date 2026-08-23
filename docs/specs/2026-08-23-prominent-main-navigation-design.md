# Prominent Main Navigation Design

**Date:** 2026-08-23
**Status:** Approved

## Goal

Make NocturneL's five primary destinations more prominent by moving Settings out of the main tab strip and into the application header. The primary tabs will share the available width evenly and use each active font preset's intended typography instead of a forced compact size.

## Success Criteria

- [ ] `LIB`, `SEA`, `ART`, `PLY`, and `NOW` each occupy one-fifth of the usable navigation width.
- [ ] Primary tabs use the active theme's prominent 14sp `labelLarge` typography without affecting other app text.
- [ ] All five tabs remain visible and usable at 320dp width, enlarged Android font scale, and with every bundled font preset.
- [ ] A Settings gear appears at the far right of the `NOCTURNEL` header with an accessible label and minimum touch target.
- [ ] When Settings is open, the gear receives the selected treatment and no primary tab appears selected.
- [ ] Existing navigation state, back behavior, content, and terminal styling remain intact.

## Scope

**In scope:**

- Restructure the header into a title-and-settings row.
- Replace the `SET` text tab with a terminal-styled gear icon button.
- Render only the five primary destinations in the tab row.
- Give each primary tab equal width and restore theme-driven label typography.
- Preserve minimum touch targets and active-state animation.
- Add or update Compose, source-guard, and screenshot coverage for all font presets and constrained width/font scale.
- Update user documentation that describes `SET` as a sixth navigation label.

**Out of scope:**

- Renaming the five abbreviated tabs.
- Redesigning the Settings screen.
- Changing destination state, screen content, colors, or font presets.
- Introducing a broader icon library or changing other app controls.
- Altering website navigation unrelated to the Android app.

## Design

### State and navigation behavior

No new persisted data is required. `NocturneLDestination.SETTINGS` remains an existing destination so saved navigation state and screen routing continue to work.

The main tab collection excludes Settings. Selecting the header gear navigates through the same destination callback used by the tabs. When Settings is selected, all five primary tabs are inactive and the gear is active. Selecting a primary tab from Settings clears nested screen state exactly as current navigation does. The gear uses the existing active-navigation pulse when effects are enabled and a steady selected state when effects are disabled.

### Component design and interfaces

`TerminalScaffold` retains its selected destination, destination callback, and effects inputs. Its header becomes a full-width row containing `NOCTURNEL` on the left and the Settings icon button on the right.

`TerminalNavigation` renders only the five primary destinations. Each button receives equal weight, centered content, the existing bracketed terminal treatment, and unmodified `MaterialTheme.typography.labelLarge` so the primary navigation is visibly prominent.

The Settings control is a terminal-styled gear icon button with:

- A gear symbol matching the app's sharp terminal aesthetic.
- A minimum 48dp touch target.
- `Settings` as its accessibility content description.
- The same selected color and alpha conventions as navigation buttons.
- No visible `SET` label, leaving the tab row entirely for primary navigation.

The implementation should reuse facilities already available to the project. If an appropriate icon is not already available without adding a broad icon dependency, use a small repo-native vector asset.

### Error handling and edge cases

- At 320dp width, each primary tab retains a minimum touch height while sharing width equally; abbreviated labels must not clip with any font preset.
- At enlarged font scale, labels remain centered and visible without shrinking to a hard-coded text size.
- The gear's visual glyph may be smaller than its touch area, preserving a compact header without reducing usability.
- Screen readers announce the control as Settings, and its selected state remains semantically exposed.
- If Settings is restored after process recreation, the gear appears selected immediately.
- Reduced-motion or disabled CRT effects removes pulsing while retaining a clear static selected state.
- The icon uses semantic theme colors and remains legible in every existing color theme.
- The scanline overlay does not intercept gear or tab interaction.

## Testing Strategy

Tests are written before the production change and cover:

- The primary navigation displays exactly the five abbreviated tabs and no `SET` tab.
- Each primary tab remains displayed and clickable at 320dp with every font preset.
- Constrained width plus enlarged font scale does not hide any tab.
- The Settings gear is displayed, accessible by the Settings description, and navigates when clicked.
- The selected state belongs to the gear on Settings and to the appropriate tab elsewhere.
- Source guards verify five equal-width tab targets, theme typography without a hard-coded font size, and no horizontally scrolling fallback.
- Screenshot previews cover normal and constrained header/navigation layouts across the font presets.
- The focused JVM and Compose test suites pass, followed by the complete project test suite and release build where available.
- Git status confirms only feature-related files are committed before syncing `main` to `origin`.

## Open Questions

None.
