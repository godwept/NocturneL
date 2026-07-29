# NocturneL Terminal UI Design

**Date:** 2026-07-29  
**Status:** Approved

## Goal

Redesign NocturneL so it feels like a native continuation of the original Nocturne PWA rather than a generic Material application with terminal colors. Preserve the local player's album-first navigation and Android-native behavior while faithfully carrying over the PWA's typography, spacing, navigation, framing, controls, glow, scanlines, and terminal feedback.

## Success Criteria

- [ ] The original PWA is immediately recognizable through its black/phosphor palette, typography, ASCII framing, bracket controls, compact spacing, glow, scanlines, active-tab pulse, and terminal-style feedback.
- [ ] The library uses a two-column, album-first grid optimized for the Pixel 7.
- [ ] Album covers retain their original color identity through sharp adaptive 16-color quantization and restrained ordered dithering.
- [ ] Now-playing receives the strongest CRT treatment while remaining readable and touch-friendly.
- [ ] Disabling effects removes scanlines, glow animation, blinking, artwork animation, marquees, and other motion without changing functionality.
- [ ] Compact-looking terminal controls retain accessible Android touch targets and semantic labels.
- [ ] Fonts, artwork processing, and effects operate fully offline.

## Scope

**In scope:**

- Rebuild the shared visual system using the PWA's colors, bundled terminal fonts, type scale, spacing, glow levels, ASCII frames, dashed separators, status text, bracket buttons, toggles, and terminal inputs.
- Replace Material-style navigation with a horizontally scrollable PWA-style command bar and a clearly glowing active destination.
- Restyle folder setup, library/rescan, album grid, album detail, artists, search, playlists, now-playing, and settings.
- Add a reusable artwork pipeline with sharp adaptive 16-color quantization, subtle ordered dithering, cached results, and terminal placeholders.
- Apply stronger CRT treatment to now-playing artwork while keeping album-grid artwork sharp.
- Preserve existing backend behavior, navigation destinations, local playback, document pickers, and playlist operations.
- Persist the effects preference and honor Android reduced-motion accessibility settings.
- Add unit, Compose, screenshot, and physical-device coverage for the redesigned experience.

**Out of scope:**

- Spotify-specific PWA screens or features such as login, devices, likes, lyrics, or network status.
- Changes to library scanning, metadata extraction, playback architecture, Room schema, or playlist file formats.
- Pixelating or permanently modifying source artwork.
- Additional themes, a theme editor, visualizers, landscape/tablet redesigns, or online font/artwork downloads.
- UI functionality not already required by the approved local-player design.

## Design

### Visual direction

The native app keeps its album-first information architecture while porting the PWA's visual language faithfully:

- Pure black background with bright phosphor green foreground, dim/muted green hierarchy, amber active/warning states, and red errors.
- Bundled display and monospace fonts matching the PWA's VT323 and Share Tech Mono character.
- Compact terminal rhythm, square corners, dashed separators, responsive ASCII rules, text glow, and restrained CRT overlays.
- PWA-strength effects enabled by default, with one Settings control disabling all optional motion and overlays.
- Material components are replaced at the presentation layer by native terminal components rather than merely recolored.

### UI state

The existing Room music catalog remains unchanged. Presentation state consists of:

- `TerminalPreferences`
  - Effects enabled
  - Effective reduced-motion state, combining the saved preference with Android accessibility settings
  - Local persistence observed by the root UI
- `TerminalEffects`
  - Scanlines
  - Static glow
  - Animated glow/pulse
  - Blink
  - Marquee
  - Artwork CRT animation
  - Values derived centrally so every screen disables effects consistently
- `ArtworkPresentation`
  - Original artwork source
  - Quantized result
  - Placeholder fallback
  - Loading/error state
  - Optional now-playing CRT treatment

Navigation, selected album, search text, scan progress, playlist state, and playback state remain owned by their existing screen or ViewModel boundaries. The redesign does not introduce a new global application-state container.

### Artwork processing

Artwork processing follows this pipeline:

1. Load the resolved manual, embedded, folder, or placeholder source.
2. Derive an adaptive palette of at most 16 colors from that cover.
3. Quantize to that palette and apply subtle ordered dithering without reducing image dimensions.
4. Cache the transformed bitmap using source identity, requested size, palette count, and transformation version.
5. Render cached artwork sharply in the two-column album grid.
6. Apply only lightweight GPU scanline, vignette, glow, pulse, and restrained glitch effects to now-playing presentation.
7. Fall through to the next artwork source or deterministic terminal placeholder if decoding or processing fails.

Quantization runs away from the main thread, deduplicates simultaneous requests, and uses bounded caches so rapid scrolling remains smooth.

### Shared components

- `TerminalScaffold`: black root surface, bounded content width, safe-area handling, horizontal command navigation, and CRT overlay.
- `TerminalHeader`: glowing `NOCTURNEL` title with compact source or status text.
- `TerminalNav`: horizontally scrolling destinations with dim inactive commands and amber glowing active state.
- `AsciiFrame`: responsive top and bottom rules with a title interruption and no fixed character count.
- `BracketButton`: visually compact `[ COMMAND ]` control backed by at least a 48 dp touch target.
- `TerminalIconButton`: character controls such as `[|>]`, `[<<]`, `[>>]`, shuffle, and repeat with accessible labels.
- `TerminalTextField`: prompt prefix, underline, phosphor caret, and terminal placeholder text.
- `TerminalToggle`: bracket or checkbox presentation rather than a Material switch.
- `TerminalNotice`: informational, warning, and error variants.
- `TerminalMarquee`: scrolls overflowing metadata only when effects and motion are enabled.
- `RetroArtwork`: sharp adaptive 16-color artwork for grids and detail screens.
- `CrtArtwork`: now-playing artwork wrapper with stronger scanlines, vignette, glow, and restrained animation.

### Screen structure

**Folder setup**

- Centered ASCII panel with a concise explanation and one obvious bracket action.
- Intentional access-lost and folder-reselection states.

**Library**

- Compact source/rescan frame followed by a two-column cover grid.
- Each tile includes square artwork, album title, artist, and compact year/count metadata.
- Scan progress, cancellation, report summary, empty library, and error states use terminal notices.

**Album detail**

- Prominent quantized cover with back, play, shuffle, queue, set-cover, and clear-cover commands.
- Compact numbered track list in disc/track order.

**Artists**

- Compact terminal list grouped by artist.
- Artist selection opens the artist's albums rather than ending at a count-only row.

**Search**

- PWA-style `>` prompt input.
- Clearly separated track, album, and artist result groups with intentional loading, empty, and error states.

**Playlists**

- Compact playlist list and detail view.
- Track add/remove/reorder, import/export, play, and shuffle commands.
- Missing entries remain visible in dim warning styling and are not actionable for playback.

**Now playing**

- Strongest CRT presentation with large quantized artwork.
- Title, artist, album, seekable terminal time line, transport row, shuffle/repeat state, playback notices, and upcoming queue.
- Artwork retains its adaptive palette beneath the CRT overlays.

**Settings**

- Framed groups for library source, display effects, and reduced-motion status.
- Terminal toggles replace Material switches.
- Effects are on by default and the preference persists.

### Accessibility and responsive behavior

- Visible terminal controls may be compact, but their semantic touch regions remain at least 48 dp.
- All symbolic controls have spoken labels.
- ASCII rules adapt to measured width and never wrap because of hard-coded repeated characters.
- Long metadata truncates cleanly; marquee starts only for genuine overflow and remains static under reduced motion.
- The command navigation scrolls horizontally and brings the active destination into view.
- System font scaling may increase row height and reduce density, but labels and controls do not overlap or clip.
- Android reduced-motion settings override optional animation while preserving static borders, color hierarchy, and contrast.

### Error handling and edge cases

- Artwork decode or quantization failures fall through to the next valid source and then the terminal placeholder.
- Placeholders display immediately during processing to avoid empty boxes and layout shifts.
- Failed scans, revoked folder access, playlist import/export failures, and unavailable playback use consistent terminal notices with recovery actions.
- Empty libraries, searches, albums, playlists, and playback sessions have designed terminal states rather than blank areas.
- Missing playlist entries remain visible but cannot dispatch playback.
- Back handling exits detail views before leaving the app.
- Navigation selection remains stable across recomposition and rotation.

## Testing Strategy

### Unit tests

- Adaptive palette generation never exceeds 16 colors.
- Quantization preserves transparency, image dimensions, deterministic output, and original cover identity.
- Ordered dithering remains restrained.
- Artwork cache keys change with source, size, palette count, or transformation version.
- Effects configuration combines saved preferences and reduced-motion state correctly.
- Time, track-number, repeat, scan-summary, and terminal-label formatting is correct.

### Compose tests

- The Pixel 7 viewport renders a two-column album grid.
- Navigation exposes every destination, identifies the active destination, and remains horizontally scrollable.
- Bracket controls preserve semantic labels and minimum touch targets.
- Album tiles expose artwork, album, and artist information.
- Album detail preserves track ordering and dispatches playback actions.
- Search renders prompt, loading, empty, error, and grouped-result states.
- Missing playlist entries remain visible and non-playable.
- Now-playing controls dispatch previous, play/pause, next, seek, shuffle, and repeat.
- Effects-off mode removes optional overlays and animation while preserving content.
- Folder access, scan, artwork, playlist, and playback failures render actionable notices.

### Screenshot and device verification

Golden captures cover:

- Empty setup
- Populated two-column library
- Album detail
- Search results
- Playlist detail with a missing entry
- Now-playing with artwork
- Settings with effects on and off

The existing GitHub build and unit-test workflow remains required and compiles instrumented tests. A physical Pixel 7 pass verifies scrolling performance, font scaling, artwork caching, reduced motion, effect intensity, and touch ergonomics.

## Open Questions

None.
