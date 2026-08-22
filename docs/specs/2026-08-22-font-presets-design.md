# Font Presets Design

**Date:** 2026-08-22
**Status:** Approved

## Goal

Add four bundled typography presets that let users change NocturneL's complete display/body font pairing from Settings. The active preset changes the running app immediately, survives restarts, works fully offline, and preserves the current VT323 and Share Tech Mono appearance as the default for existing and new users.

## Success Criteria

- [ ] Settings exposes one compact cycling control showing the active preset name.
- [ ] Each cycle immediately updates typography across every app screen.
- [ ] The chosen preset persists automatically across app restarts.
- [ ] All four presets remain readable without clipped, overlapping, or unexpectedly wrapped text on supported screen sizes.
- [ ] Font files and required license notices ship with the app.
- [ ] The website's Settings manual documents the selector and available presets.

## Scope

**In scope:**

- Four named, bundled display/body font pairings.
- A compact `FONT PRESET: <NAME>` cycle control in Settings.
- Immediate app-wide theme updates and automatic persistence.
- Safe fallback to the Classic preset if stored preference data is missing or invalid.
- Local license notices for every bundled font.
- Unit, wiring, and screenshot coverage for persistence and visual behavior.
- An update to the website's Settings manual page.
- Vertical scrolling for Settings so the new control remains reachable on small screens and at larger Android font scales.

**Out of scope:**

- Device-installed or downloadable fonts.
- User-imported font files.
- Separate display and body selectors.
- Font size, weight, spacing, or accessibility controls.
- Website homepage changes or new website screenshots.
- Cloud synchronization of the preference.

## Design

### Preset Catalog

| Preset | Display font | Body font | Character |
|---|---|---|---|
| Classic | VT323 | Share Tech Mono | Current CRT terminal |
| Mainframe | Oxanium | IBM Plex Mono | Technical terminal/HUD |
| Pixel | Press Start 2P | Space Mono | Pixel-console |
| Modern | Space Mono Bold | IBM Plex Mono | Clean contemporary mono |

All fonts are bundled with the application and require no network access. Their applicable license texts are retained alongside the font resources.

### Data Model and State

Use a closed `FontPreset` model with stable persisted identifiers. Store the identifier rather than a resource filename or enum ordinal so the catalog can be reordered without corrupting saved choices.

`TerminalSettingsState` includes the active preset. `TerminalPreferencesRepository` reads and writes its stable identifier. Missing, obsolete, or malformed values resolve to Classic, so existing installations require no database or preference migration.

Each preset owns the complete Compose typography mapping for display, headline, title, body, and label roles. Font sizes remain consistent with the current theme. Narrowly targeted line-height or letter-spacing adjustments are permitted only where required to prevent clipping or preserve readability.

### Theme and Settings Interfaces

`NocturneLTheme` accepts the active `FontPreset` and resolves its complete Material typography set. Settings state is observed above the theme boundary so changing the preset recomposes the whole interface, including Settings itself.

`SettingsScreen` receives an `onCycleFontPreset` callback and presents a compact control in this form:

`FONT PRESET: CLASSIC    [NEXT]`

Activation follows a fixed, wrapping order:

`CLASSIC -> MAINFRAME -> PIXEL -> MODERN -> CLASSIC`

The callback persists the new identifier and publishes the updated settings state. There is no Apply action, confirmation dialog, or temporary preview state.

The website Settings manual's Appearance and Motion section lists the four presets, explains that a selection affects the entire app immediately, and states that all fonts are bundled for offline use.

### Error Handling and Edge Cases

- Unknown or corrupted persisted identifiers fall back to Classic without affecting unrelated preferences.
- Classic retains the existing font resources and typography values, preventing an unexpected visual change for existing users.
- Cycling wraps predictably, and rapid repeated taps resolve to the latest selected value.
- Settings content scrolls vertically so the selector remains reachable on small screens and at larger Android font scales.
- Presets must accommodate navigation labels, long track metadata, dialog text, and compact controls without clipping or overlap. Existing overflow and ellipsis behavior remains authoritative.
- Android's normal glyph fallback handles characters absent from a bundled font, including international music metadata.
- If a preference write fails, the current session may continue to display the selected preset. A later launch resolves to the last readable value or Classic.
- Font license notices change in the same commit whenever the bundled catalog changes.

## Testing Strategy

- Unit-test the preset model's fixed cycle order, wrapping behavior, stable identifiers, and invalid-value fallback.
- Test preference restoration and persistence, including behavior when the font preference does not yet exist.
- Add wiring tests proving the selected preset flows from settings state into the app-wide theme and that the Settings control invokes the cycle action.
- Add typography mapping tests confirming every Material text role uses the expected display or body family.
- Add screenshot coverage for Settings under all four presets and representative dense screens under the layout-sensitive Pixel preset.
- Verify small-screen and enlarged-system-font rendering so Settings remains usable and text does not overlap.
- Keep existing Classic screenshot references unchanged unless testing exposes an unrelated pre-existing rendering difference.
- Update the website contract test to require the preset names and offline behavior in the Settings manual.
- Run the complete unit and screenshot suites before considering implementation complete.

## Open Questions

None.
