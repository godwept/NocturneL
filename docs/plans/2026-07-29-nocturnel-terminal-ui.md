# NocturneL Terminal UI Implementation Plan

**Design:** `docs/specs/2026-07-29-nocturnel-terminal-ui-design.md`  
**Goal:** Keep the native app's album-art-first navigation and screen structure while matching the original Nocturne PWA's terminal styling, including sharp identity-preserving 16-color artwork and a persisted effects toggle.  
**Scope guard:** This is a presentation-layer change. Do not change the Room schema, scanner behavior, audio engine, queue semantics, or import/export formats.

## Working rules

- Complete the tasks in order. Each checkbox is intended to take roughly 2–5 minutes.
- For every behavior change, add the named failing test first, run it and confirm the expected failure, make only the described implementation change, then rerun the focused test.
- After each phase, run the stated phase gate before continuing.
- Reuse the existing `AlbumEntity`, `TrackEntity`, `PlaylistEntity`, `PlaylistRepository`, `PlaybackConnection`, and Coil 2.7 setup.
- Keep all controls at least 48 dp in both dimensions, even when the drawn bracket or glyph is smaller.
- Keep the current two-column album grid on a Pixel 7-sized viewport.
- Preserve any pre-existing edits in `.github/workflows/android.yml`; add only the screenshot-validation step described below.

## Phase 1: Establish the visual contract

### Task 1: Add shared UI test fixtures

- [ ] **1.1 — Add a failing fixture smoke test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/UiFixturesTest.kt`. Reference `sampleAlbum`, `sampleTracks`, and `samplePlaylist`, and assert their IDs and relationships. The missing fixture symbols are the expected failure.

- [ ] **1.2 — Add the fixture factory (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/UiFixtures.kt` with deterministic builders for `AlbumEntity`, `TrackEntity`, and `PlaylistEntity`. Use fixed IDs, titles, artists, durations, paths, and no device-dependent data.

- [ ] **1.3 — Verify the fixture contract (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.UiFixturesTest
  ```
  Expected: the fixture smoke test passes.

### Task 2: Encode the PWA palette and spacing

- [ ] **2.1 — Write the failing token test (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/theme/TerminalTokensTest.kt`. Assert the ARGB values for black `#000000`, alternate black `#050805`, phosphor green `#00FF41`, dim green `#00B32D`, muted green `#008020`, bright green `#39FF7C`, amber `#FFB000`, red `#FF3030`, and scanline alpha `0.18f`.

- [ ] **2.2 — Replace approximate colors with named tokens (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/theme/Color.kt`. Define only the tokens named in the design. Update `Theme.kt` so the dark color scheme maps background/surface/on-surface/primary/secondary/error to those tokens; keep the entire app dark.

- [ ] **2.3 — Add dimensions and verify (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/theme/TerminalDimensions.kt` with the 4/8/12/16/24/32 dp spacing scale, zero corner radius, 1 dp borders, and 48 dp minimum touch target. Rerun:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.theme.TerminalTokensTest
  ```

### Task 3: Bundle the two offline fonts

- [ ] **3.1 — Add a failing resource compilation reference (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/theme/Typography.kt` to reference `R.font.vt323_regular` and `R.font.share_tech_mono_regular`. Run `.\gradlew.bat assembleDebug` and confirm it fails because the resources do not yet exist.

- [ ] **3.2 — Add licensed font assets (2–5 min)**  
  Add `app/src/main/res/font/vt323_regular.ttf` and `app/src/main/res/font/share_tech_mono_regular.ttf` from their canonical Google Fonts family directories. Add their OFL text to `app/src/main/res/raw/ofl_vt323.txt` and `app/src/main/res/raw/ofl_share_tech_mono.txt`.

- [ ] **3.3 — Map typography roles and compile (2–5 min)**  
  Finish `Typography.kt`: use VT323 for display/headline/title roles and Share Tech Mono for body/label roles. Do not add downloadable-font providers or network font dependencies. Run:
  ```powershell
  .\gradlew.bat assembleDebug
  ```

### Task 4: Define the effects policy

- [ ] **4.1 — Write failing policy tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/effects/EffectsPolicyTest.kt`. Cover: saved default is enabled; a saved disabled value wins; Android reduced-motion forces effective effects off without overwriting the saved preference.

- [ ] **4.2 — Implement the pure policy (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/effects/EffectsPolicy.kt` with `savedEffectsEnabled` and `systemAnimationsEnabled` inputs and one `effectiveEffectsEnabled` result.

- [ ] **4.3 — Verify the policy (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.effects.EffectsPolicyTest
  ```

### Task 5: Persist the visual setting

- [ ] **5.1 — Write a failing repository instrumented test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`. Use an isolated `SharedPreferences` name, assert the first read is `true`, save `false`, reconstruct the repository, and assert `false`.

- [ ] **5.2 — Add the small preferences repository (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`. Store only `effects_enabled`; expose a `StateFlow<Boolean>` and `setEffectsEnabled(Boolean)`. Do not add DataStore or alter Room.

- [ ] **5.3 — Add settings state ownership (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`. Combine the repository flow with Android's animator-duration setting through `EffectsPolicy`; expose saved and effective values separately.

- [ ] **5.4 — Verify persistence (2–5 min)**  
  Run the new repository test with `connectedDebugAndroidTest`, then run `.\gradlew.bat testDebugUnitTest`.

## Phase 2: Build the reusable terminal component set

### Task 6: Replace the hard-coded frame

- [ ] **6.1 — Write failing frame semantics tests (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/AsciiFrameTest.kt`. Assert an optional title is exposed, content remains discoverable, and the frame does not expose repeated dash characters as accessibility text.

- [ ] **6.2 — Implement the adaptive ASCII frame (2–5 min)**  
  Replace `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalFrame.kt` with `AsciiFrame`. Draw square-corner borders with Compose drawing primitives, render optional terminal corner/title glyphs, and keep decorative glyphs out of semantics. Never size the frame with a fixed dash count.

- [ ] **6.3 — Verify the frame (2–5 min)**  
  Run the focused `AsciiFrameTest`.

### Task 7: Add bracket buttons

- [ ] **7.1 — Write failing control tests (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/BracketButtonTest.kt`. Assert enabled click behavior, disabled behavior, content description for icon-only controls, and a minimum 48 dp hit target.

- [ ] **7.2 — Implement text and icon variants (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalButton.kt` with `BracketButton` and `BracketIconButton`. Render `[ LABEL ]`/terminal glyphs with square edges, visible focus/pressed states, and amber for the selected/active state.

- [ ] **7.3 — Verify controls (2–5 min)**  
  Run the focused `BracketButtonTest`.

### Task 8: Add terminal form controls

- [ ] **8.1 — Write failing form-control tests (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalFormControlsTest.kt`. Assert that `TerminalTextField` edits text and labels itself, and `TerminalToggle` reports checked state and toggles from the whole 48 dp row.

- [ ] **8.2 — Implement the text field (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalTextField.kt`. Use `BasicTextField`, a square green border, terminal cursor/selection colors, label text, and no Material pill/rounded shape.

- [ ] **8.3 — Implement the toggle (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalToggle.kt`. Render `[X]` and `[ ]`, use semantic toggle state, and retain a full-width 48 dp click target.

- [ ] **8.4 — Verify form controls (2–5 min)**  
  Run the focused `TerminalFormControlsTest`.

### Task 9: Add notices and marquee behavior

- [ ] **9.1 — Write failing notice/marquee tests (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalFeedbackTest.kt`. Assert notice severity maps to green/amber/red semantics and that disabled effects expose the complete marquee text without animation.

- [ ] **9.2 — Implement notices (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalNotice.kt` with info, warning, and error variants and readable semantic text.

- [ ] **9.3 — Implement the effects-aware marquee (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalMarquee.kt`. Animate only when the measured text overflows and effective effects are enabled; otherwise show a static clipped or wrapped value.

- [ ] **9.4 — Verify feedback components (2–5 min)**  
  Run the focused `TerminalFeedbackTest`.

### Task 10: Build terminal navigation and scaffold

- [ ] **10.1 — Write a failing navigation test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalNavigationTest.kt`. Assert every destination is reachable by horizontal scrolling, the selected item reports selected state, and its label is amber.

- [ ] **10.2 — Extract destinations (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/navigation/NocturneLDestination.kt`. Move the private enum out of `NocturneLApp.kt`, retaining the existing destination set and stable keys.

- [ ] **10.3 — Implement terminal navigation (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalNavigation.kt`. Use a horizontally scrollable row, bracket labels, an amber active pulse only when effects are enabled, and a dashed green separator.

- [ ] **10.4 — Write a failing scaffold test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalScaffoldTest.kt`. Assert the app title, navigation, content, and optional status line are present in reading order.

- [ ] **10.5 — Implement the scaffold (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalScaffold.kt` with `TerminalHeader`, navigation slot, content slot, and optional status line. Use black surfaces, exact spacing tokens, square separators, and safe drawing insets.

- [ ] **10.6 — Make scanlines policy-driven (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/components/Scanlines.kt` to use the `0.18f` token and emit no overlay when effective effects are disabled.

- [ ] **10.7 — Phase gate (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=ca.stewark.nocturnel.ui.components
  ```

## Phase 3: Implement sharp adaptive 16-color artwork

### Task 11: Define a platform-neutral pixel model

- [ ] **11.1 — Write failing pixel-model tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/artwork/PixelBufferTest.kt`. Assert dimensions are validated, pixel count must equal width × height, and alpha is retained.

- [ ] **11.2 — Implement the model (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/artwork/PixelBuffer.kt` as an immutable width/height/ARGB value object usable by JVM tests without Android `Bitmap`.

- [ ] **11.3 — Verify the model (2–5 min)**  
  Run the focused `PixelBufferTest`.

### Task 12: Select an identity-preserving palette

- [ ] **12.1 — Write failing palette tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/artwork/AdaptivePaletteTest.kt`. Cover one-color input, fewer than 16 colors, more than 16 colors, deterministic output, transparent pixels, and a red-dominant cover remaining red-dominant.

- [ ] **12.2 — Build the histogram (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/artwork/AdaptivePaletteQuantizer.kt`. Add deterministic RGB histogram creation that ignores fully transparent pixels.

- [ ] **12.3 — Add palette reduction (2–5 min)**  
  In the same class, reduce the histogram to at most 16 representative colors using deterministic median-cut buckets. Preserve source-derived colors; do not substitute a fixed global terminal palette.

- [ ] **12.4 — Verify palette selection (2–5 min)**  
  Run the focused `AdaptivePaletteTest`.

### Task 13: Quantize pixels sharply

- [ ] **13.1 — Write failing quantization tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/artwork/ArtworkQuantizationTest.kt`. Assert output dimensions and alpha match input, opaque output contains no more than 16 RGB colors, identical input is deterministic, and no smoothing/intermediate colors are introduced.

- [ ] **13.2 — Map pixels to the adaptive palette (2–5 min)**  
  Add nearest-color mapping to `AdaptivePaletteQuantizer.kt` using a perceptual weighted RGB distance. Return an unchanged-sized `PixelBuffer`.

- [ ] **13.3 — Verify sharp quantization (2–5 min)**  
  Run the focused `ArtworkQuantizationTest`.

### Task 14: Add subtle ordered dithering

- [ ] **14.1 — Write failing dithering tests (2–5 min)**  
  Extend `ArtworkQuantizationTest.kt` with a small grayscale gradient. Assert dithering changes the distribution, remains deterministic, retains the 16-color ceiling, and does not resize the image.

- [ ] **14.2 — Implement the ordered matrix (2–5 min)**  
  Add a small Bayer ordered-dither pass before nearest-color mapping. Keep its amplitude low enough that dominant hue tests continue to pass. Do not add blur, bilinear resampling, or error diffusion.

- [ ] **14.3 — Verify color identity (2–5 min)**  
  Rerun `AdaptivePaletteTest` and `ArtworkQuantizationTest`.

### Task 15: Give transformed images stable cache identities

- [ ] **15.1 — Write failing key tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/artwork/RetroArtworkCacheKeyTest.kt`. Assert the key changes for source identity, modification token, palette size, or dither version, but is stable for equal inputs.

- [ ] **15.2 — Implement cache keys (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/artwork/RetroArtworkCacheKey.kt`. Include a version constant so future algorithm changes invalidate old results cleanly.

- [ ] **15.3 — Verify keys (2–5 min)**  
  Run the focused key test.

### Task 16: Adapt the quantizer to Coil

- [ ] **16.1 — Write a failing bitmap adapter test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/artwork/RetroArtworkTransformationTest.kt`. Transform a generated bitmap and assert original dimensions, alpha, no more than 16 opaque RGB colors, and a stable Coil cache key.

- [ ] **16.2 — Implement the Coil transformation (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/artwork/RetroArtworkTransformation.kt`. Convert `Bitmap` to/from `PixelBuffer`, execute quantization in Coil's suspending transformation path, use nearest-neighbor behavior, and expose `RetroArtworkCacheKey`.

- [ ] **16.3 — Verify off-main integration (2–5 min)**  
  Run the focused transformation instrumented test. Confirm the transformation contains no `runBlocking` and no UI-thread dispatcher override.

### Task 17: Preserve artwork fallback order

- [ ] **17.1 — Write failing resolver tests (2–5 min)**  
  Extend `app/src/test/java/ca/stewark/nocturnel/artwork/ArtworkResolverTest.kt` to assert ordered candidates: manual artwork, embedded artwork, folder artwork, generated placeholder. Cover a failed first candidate advancing to the second.

- [ ] **17.2 — Return ordered candidates (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/artwork/ArtworkResolver.kt` to expose a small ordered candidate list while retaining the existing source precedence.

- [ ] **17.3 — Add a presentation load state (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/artwork/ArtworkLoadState.kt` with loading, loaded, and fallback states. Keep this out of Room entities.

- [ ] **17.4 — Verify fallback behavior (2–5 min)**  
  Run all `ca.stewark.nocturnel.artwork` unit tests.

### Task 18: Replace `AlbumArtwork` with retro artwork components

- [ ] **18.1 — Write a failing composable test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/artwork/RetroArtworkTest.kt`. Assert a placeholder is visible immediately, failed candidates advance, final failure keeps the terminal placeholder, and content descriptions are retained.

- [ ] **18.2 — Implement `RetroArtwork` (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/artwork/RetroArtwork.kt`. Reuse the app's shared Coil loader, apply `RetroArtworkTransformation`, use stable request/cache keys, and advance through resolver candidates on error.

- [ ] **18.3 — Add the CRT wrapper (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/artwork/CrtArtwork.kt`. Overlay scanlines, restrained chromatic offset, and optional glitch only when effective effects are enabled; always keep the underlying 16-color image sharp.

- [ ] **18.4 — Remove the superseded component (2–5 min)**  
  Replace all `AlbumArtwork` call sites, then delete `app/src/main/java/ca/stewark/nocturnel/ui/components/AlbumArtwork.kt` only after `rg "AlbumArtwork" app/src` returns no production references.

- [ ] **18.5 — Phase gate (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.artwork.*"
  .\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=ca.stewark.nocturnel.artwork
  ```

## Phase 4: Restyle the library journey

### Task 19: Restyle library setup

- [ ] **19.1 — Write a failing setup-screen test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/LibrarySetupScreenTest.kt`. Assert the terminal title/instructions, bracket source action, scan status, error notice, and retry action.

- [ ] **19.2 — Replace Material controls (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySetupScreen.kt` to use `AsciiFrame`, `BracketButton`, and `TerminalNotice`. Preserve every existing callback and state branch.

- [ ] **19.3 — Verify setup UI (2–5 min)**  
  Run the focused setup-screen test.

### Task 20: Lock the album grid to two columns

- [ ] **20.1 — Write the failing grid test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumGridScreenTest.kt`. At a Pixel 7-equivalent width, assert the first two albums share a row, the third begins the next row, titles/artists are visible, and clicking a cover returns the correct album.

- [ ] **20.2 — Implement the album-first card (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumGridScreen.kt`. Use `GridCells.Fixed(2)`, square `RetroArtwork`, compact terminal metadata, and no Material cards or rounded shapes.

- [ ] **20.3 — Add library status treatment (2–5 min)**  
  In the same screen, render empty/scanning/error states with terminal notices and preserve the artwork-first visual hierarchy.

- [ ] **20.4 — Verify the grid (2–5 min)**  
  Run the focused grid test.

### Task 21: Restyle album detail

- [ ] **21.1 — Write a failing album-detail test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreenTest.kt`. Assert cover, album metadata, back/play/shuffle bracket actions, ordered tracks, durations, and track click callbacks.

- [ ] **21.2 — Build the terminal header block (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt` with `RetroArtwork`, `AsciiFrame`, terminal metadata, and bracket actions.

- [ ] **21.3 — Restyle the track rows (2–5 min)**  
  Replace Material buttons with square, full-width 48 dp rows showing track number/title/duration and a clear active/pressed state.

- [ ] **21.4 — Verify album detail (2–5 min)**  
  Run the focused album-detail test.

### Task 22: Add artist grouping and drill-in

- [ ] **22.1 — Write failing grouping tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/library/ArtistGroupingTest.kt`. Assert case-insensitive grouping, stable alphabetic order, album counts, and an `Unknown Artist` fallback.

- [ ] **22.2 — Implement the grouping model (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/library/ArtistGrouping.kt` with pure mapping functions from albums to artist rows and selected-artist albums.

- [ ] **22.3 — Write a failing screen interaction test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/ArtistsScreenTest.kt`. Assert artist rows open a detail view and album covers in that view invoke the album callback.

- [ ] **22.4 — Restyle the artist index (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/library/ArtistsScreen.kt` to use terminal rows, album counts, and an artist-selection callback.

- [ ] **22.5 — Add artist detail (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/library/ArtistDetailScreen.kt` with a back action and the same two-column album-art-first layout.

- [ ] **22.6 — Verify artists (2–5 min)**  
  Run `ArtistGroupingTest` and `ArtistsScreenTest`.

### Task 23: Group search results

- [ ] **23.1 — Write failing search projection tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/library/SearchProjectionTest.kt`. Cover blank query, case-insensitive matching, and separate track/album/artist result groups without duplicates.

- [ ] **23.2 — Implement the pure projection (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchProjection.kt`. Return typed result groups and stable ordering.

- [ ] **23.3 — Write a failing search-screen test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/SearchScreenTest.kt`. Enter a query, assert the three labeled groups, and verify track, album, and artist callbacks.

- [ ] **23.4 — Restyle search (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/library/SearchScreen.kt` to use `TerminalTextField`, ASCII group headings, terminal result rows, and empty-result notices.

- [ ] **23.5 — Verify search (2–5 min)**  
  Run the projection and screen tests.

## Phase 5: Complete playlist presentation

### Task 24: Expose playlist entries without schema changes

- [ ] **24.1 — Write a failing Room query test (2–5 min)**  
  Extend `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`. Seed a playlist and tracks; assert the returned playlist-entry rows preserve position and include title, artist, relative path, and missing-track status.

- [ ] **24.2 — Add the query projection (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/data/model/PlaylistEntryRow.kt` and add a `LEFT JOIN` read query to `app/src/main/java/ca/stewark/nocturnel/data/dao/LibraryDao.kt`. Do not add or migrate tables.

- [ ] **24.3 — Verify Room behavior (2–5 min)**  
  Run the focused `NocturneLDatabaseTest`.

### Task 25: Add playlist-detail state

- [ ] **25.1 — Write failing state tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailStateTest.kt`. Assert entry ordering, exclusion of already-added tracks from the add list, and disabled up/down actions at list boundaries.

- [ ] **25.2 — Implement the pure state mapper (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailState.kt` with playlist metadata, ordered rows, available tracks, and boundary flags.

- [ ] **25.3 — Verify the mapper (2–5 min)**  
  Run the focused state test.

### Task 26: Wire playlist edit commands

- [ ] **26.1 — Write failing ViewModel tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModelTest.kt` with a fake repository/DAO boundary. Cover open, rename, add, remove, move up, move down, delete, and refreshed detail state after each mutation.

- [ ] **26.2 — Add detail loading (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModel.kt` with selected playlist ID and `StateFlow<PlaylistDetailState?>`.

- [ ] **26.3 — Add edit methods (2–5 min)**  
  In the same ViewModel, delegate add/remove/move/rename/delete to the existing `PlaylistRepository`, then refresh the selected detail. Keep import/export behavior unchanged.

- [ ] **26.4 — Verify commands (2–5 min)**  
  Run the focused `PlaylistViewModelTest` plus existing playlist tests.

### Task 27: Restyle playlist index and detail

- [ ] **27.1 — Write a failing playlist UI test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreenTest.kt`. Assert create/import/export controls, row selection, rename/delete, and play.

- [ ] **27.2 — Restyle the index (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt` to use terminal fields, bracket commands, ASCII frames, and notices while preserving document-picker callbacks.

- [ ] **27.3 — Write a failing detail UI test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`. Assert `[ ADD TRACK ]` reveals an inline available-track list, `[+]` adds, `[↑]`/`[↓]` reorder, `[X]` removes, and boundary arrows disable correctly.

- [ ] **27.4 — Implement playlist detail (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`. Use full-width entry rows and 48 dp command targets; keep the add-track picker inline and searchable rather than adding a new navigation layer.

- [ ] **27.5 — Verify playlist screens (2–5 min)**  
  Run both focused playlist UI tests.

## Phase 6: Restyle playback and settings

### Task 28: Expose now-playing presentation data

- [ ] **28.1 — Write failing playback-state tests (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackUiStateTest.kt`. Assert current path, album title, current queue index, and up-next items are projected in queue order.

- [ ] **28.2 — Extend the presentation model (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`. Add only presentation fields to `PlaybackUiState` and map them from Media3's current item and timeline; do not change player commands or queue semantics.

- [ ] **28.3 — Carry album identity in media metadata (2–5 min)**  
  Update media-item construction in `PlaybackConnection.kt` and `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt` to include the existing album ID in metadata extras. Use it only to resolve the displayed artwork.

- [ ] **28.4 — Verify playback state (2–5 min)**  
  Run the focused test and all existing playback unit tests.

### Task 29: Build the terminal seek control

- [ ] **29.1 — Write a failing seek test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/components/TerminalSeekBarTest.kt`. Assert progress semantics, tap/drag callbacks, clamping, and a 48 dp touch region.

- [ ] **29.2 — Implement the seek bar (2–5 min)**  
  Create `app/src/main/java/ca/stewark/nocturnel/ui/components/TerminalSeekBar.kt` with a square ASCII-style track and block thumb. Keep drag math in a pure helper covered by the same test file.

- [ ] **29.3 — Verify seeking (2–5 min)**  
  Run the focused seek test.

### Task 30: Extract and restyle Now Playing

- [ ] **30.1 — Write a failing Now Playing test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreenTest.kt`. Assert large CRT artwork, title/artist/album marquee fields, elapsed/duration, play/pause, previous/next, shuffle/repeat, seek callback, up-next rows, and error notice.

- [ ] **30.2 — Extract the screen (2–5 min)**  
  Move the private composable from `NocturneLApp.kt` into `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt` with explicit state and callbacks.

- [ ] **30.3 — Apply the terminal layout (2–5 min)**  
  Use `CrtArtwork`, `TerminalMarquee`, `TerminalSeekBar`, bracket icon controls, amber active shuffle/repeat states, and an ASCII-framed up-next list.

- [ ] **30.4 — Verify Now Playing (2–5 min)**  
  Run the focused Now Playing test.

### Task 31: Extract and restyle Settings

- [ ] **31.1 — Write a failing Settings test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsScreenTest.kt`. Assert the effects toggle reflects the saved value, invokes its callback, explains reduced-motion override, and exposes library rescan/source actions already present in app state.

- [ ] **31.2 — Extract the screen (2–5 min)**  
  Move the private settings composable from `NocturneLApp.kt` into `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`.

- [ ] **31.3 — Apply terminal controls (2–5 min)**  
  Use `AsciiFrame`, `TerminalToggle`, bracket actions, and notices. Display saved-on/effective-off distinctly when Android reduced motion suppresses effects.

- [ ] **31.4 — Verify Settings (2–5 min)**  
  Run the focused Settings test and `TerminalPreferencesRepositoryTest`.

## Phase 7: Integrate navigation and effects

### Task 32: Make root navigation restorable

- [ ] **32.1 — Write a failing root navigation test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/NocturneLAppTest.kt`. Assert selecting library/artist/search/playlists/now-playing/settings changes content, back exits artist/album/playlist detail first, and state survives activity recreation.

- [ ] **32.2 — Replace inline Material scaffold/nav (2–5 min)**  
  Update `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt` to use `TerminalScaffold` and `TerminalNavigation`. Replace plain `remember` destination/detail selections with `rememberSaveable` stable IDs.

- [ ] **32.3 — Connect detail routes (2–5 min)**  
  In `NocturneLApp.kt`, wire artist, album, search-result, and playlist callbacks to their detail screens without introducing a second navigation framework.

- [ ] **32.4 — Connect saved/effective effects (2–5 min)**  
  Obtain `SettingsViewModel`, pass effective effects to scanlines/navigation/marquee/CRT artwork, and pass the saved toggle value to Settings. Remove the old in-memory `effectsEnabled` state.

- [ ] **32.5 — Verify root integration (2–5 min)**  
  Run the focused `NocturneLAppTest`.

### Task 33: Verify effects-off behavior across components

- [ ] **33.1 — Add a failing cross-component test (2–5 min)**  
  Create `app/src/androidTest/java/ca/stewark/nocturnel/ui/effects/EffectsDisabledTest.kt`. Render the root with effective effects disabled and assert static navigation, static marquee, plain retro artwork, and no scanline overlay test tags.

- [ ] **33.2 — Remove stray unconditional effects (2–5 min)**  
  Search with:
  ```powershell
  rg -n "infiniteRepeatable|rememberInfiniteTransition|Scanlines|glitch|chromatic" app/src/main/java
  ```
  Route every visual animation/overlay found through effective effects. Do not suppress functional progress or touch feedback.

- [ ] **33.3 — Verify the global toggle (2–5 min)**  
  Run `EffectsPolicyTest` and `EffectsDisabledTest`.

## Phase 8: Add visual regression coverage

### Task 34: Configure Compose preview screenshot testing

- [ ] **34.1 — Add a failing screenshot preview (2–5 min)**  
  Create `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt` with one `@PreviewTest` Pixel 7-sized album-grid preview. Run `.\gradlew.bat updateDebugScreenshotTest` and confirm the screenshot source set/plugin is not configured.

- [ ] **34.2 — Add version-catalog entries (2–5 min)**  
  Update `gradle/libs.versions.toml` with the official Compose Preview Screenshot Testing plugin (at least `0.0.1-alpha15`) and `androidx.compose.ui:ui-tooling`/preview validation dependency aliases compatible with the existing AGP 9.0.1, Kotlin 2.2.10, and JDK 17 setup.

- [ ] **34.3 — Enable screenshot tests (2–5 min)**  
  Update `gradle.properties` with `android.experimental.enableScreenshotTest=true`. Update `app/build.gradle.kts` to apply the screenshot plugin, set the matching experimental property, and add only `screenshotTestImplementation` dependencies.

- [ ] **34.4 — Generate the first reference (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat updateDebugScreenshotTest
  .\gradlew.bat validateDebugScreenshotTest
  ```
  Commit the generated album-grid reference under the plugin-generated reference-image location.

### Task 35: Cover the approved screen states

- [ ] **35.1 — Add setup and library previews (2–5 min)**  
  Extend `TerminalUiScreenshotTest.kt` with deterministic setup, populated two-column library, and album-detail previews. Generate references and validate.

- [ ] **35.2 — Add discovery previews (2–5 min)**  
  Add artist index/detail and grouped search previews. Generate references and validate.

- [ ] **35.3 — Add playlist previews (2–5 min)**  
  Add playlist index and populated playlist-detail previews, including the inline add-track state. Generate references and validate.

- [ ] **35.4 — Add playback/settings previews (2–5 min)**  
  Add Now Playing and Settings previews with effects on, plus one representative root preview with effects off. Generate references and validate.

- [ ] **35.5 — Add artwork identity fixtures (2–5 min)**  
  Add three tiny deterministic bitmap resources under `app/src/screenshotTest/res/drawable-nodpi/` representing red-, blue-, and warm-neutral-dominant covers. Add a screenshot preview that proves each retains its original identity after 16-color processing.

- [ ] **35.6 — Validate all references (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat validateDebugScreenshotTest
  ```

### Task 36: Put visual tests in CI

- [ ] **36.1 — Add the CI command (2–5 min)**  
  Carefully update `.github/workflows/android.yml`, preserving its current uncommitted content. Add `validateDebugScreenshotTest` after unit tests and before APK assembly; do not reorder or remove existing build/test/upload steps.

- [ ] **36.2 — Verify workflow syntax locally (2–5 min)**  
  Inspect the diff with:
  ```powershell
  git diff -- .github/workflows/android.yml
  ```
  Confirm the only new plan-related workflow change is screenshot validation.

## Phase 9: Final verification and cleanup

### Task 37: Remove obsolete Material-shaped UI

- [ ] **37.1 — Add a failing source guard (2–5 min)**  
  Create `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`. Scan `ui/` production Kotlin sources and fail on direct use of `Button(`, `OutlinedTextField(`, `Card(`, or rounded-shape declarations outside the terminal component package.

- [ ] **37.2 — Remove remaining violations (2–5 min)**  
  Run the guard, replace only reported presentation usages with the shared terminal components, and leave Material theme infrastructure that Compose still requires.

- [ ] **37.3 — Verify the guard (2–5 min)**  
  Run the focused source-guard test.

### Task 38: Run the complete automated suite

- [ ] **38.1 — Run Android lint (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat lintDebug
  ```
  Fix only issues introduced by this implementation; do not fold unrelated lint cleanup into the UI change.

- [ ] **38.2 — Run JVM tests (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```

- [ ] **38.3 — Run instrumented tests (2–5 min)**  
  With the configured emulator/device available, run:
  ```powershell
  .\gradlew.bat connectedDebugAndroidTest
  ```

- [ ] **38.4 — Run screenshot validation (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat validateDebugScreenshotTest
  ```

- [ ] **38.5 — Assemble release inputs (2–5 min)**  
  Run:
  ```powershell
  .\gradlew.bat assembleDebug assembleDebugAndroidTest
  ```

### Task 39: Perform the Pixel 7 acceptance pass

- [ ] **39.1 — Check the visual baseline (2–5 min)**  
  On a Pixel 7/API 33+ emulator or device, verify black backgrounds, exact green/amber/red roles, VT323 headings, Share Tech Mono body text, square borders, two album columns, and no clipped system-bar content.

- [ ] **39.2 — Check artwork behavior (2–5 min)**  
  Open visually distinct covers and confirm each is sharp, at most 16 colors, subtly dithered, identity-preserving, placeholder-first, and stable after revisiting the screen.

- [ ] **39.3 — Check effects accessibility (2–5 min)**  
  Verify effects are on by default, the Settings toggle persists across process restart, disabling it removes scanlines/glitch/pulse/marquee motion, and Android reduced motion suppresses effects without changing the saved toggle.

- [ ] **39.4 — Check interaction and accessibility (2–5 min)**  
  Exercise every destination, album/artist/playlist detail, add/remove/reorder controls, playback controls, seeking, import/export, and back behavior. Confirm compact glyphs retain 48 dp targets and TalkBack labels are meaningful.

- [ ] **39.5 — Inspect the final change set (2–5 min)**  
  Run:
  ```powershell
  git status --short
  git diff --stat
  ```
  Confirm no Room schema, scanner, playback-engine, or unrelated files changed, and distinguish the pre-existing workflow/spec edits from implementation work.

## Definition of done

- All new pure behavior has JVM tests; all new Compose interactions have instrumented tests.
- Screenshot references cover every major approved screen and an effects-off state.
- The Pixel 7 layout is a two-column album-art-first grid.
- Artwork uses a deterministic per-cover palette of no more than 16 colors, subtle ordered dithering, sharp rendering, fallback handling, and stable caching.
- Visual effects match the PWA by default, persist through Settings, and respect Android reduced motion.
- Existing library, playlist, and playback behavior remains functional.
- `testDebugUnitTest`, `connectedDebugAndroidTest`, `validateDebugScreenshotTest`, `assembleDebug`, and `assembleDebugAndroidTest` all pass.
- No unrelated files are modified.
