# PCM Terminal Visualizer Implementation Plan

**Date:** 2026-08-16  
**Design doc:** `docs/specs/2026-08-16-pcm-terminal-visualizer-design.md`  
**Status:** Ready for review

## Overview

Add a four-state display deck to Now Playing that always starts on album art and cycles through radar, spectrum, and oscilloscope views. A Media3 1.8.0 `TeeAudioProcessor` will pass decoded PCM through unchanged while a fixed-capacity, non-blocking handoff feeds a background FFT analyzer. The UI will consume immutable snapshots, render deterministic terminal-green Canvas scenes, and enable analysis only while a visualizer is visible. The work is split into test-first slices so audio transport, analysis, state, rendering geometry, Compose behavior, screenshots, and Pixel 7 verification can be validated independently.

## Fixed Implementation Decisions

These values resolve the implementation details intentionally left open by the approved design:

- Package signal-processing and playback integration under `ca.stewark.nocturnel.visualizer`; place Compose rendering under `ca.stewark.nocturnel.ui.playback.visualizer`.
- Use a 2,048-sample Hann-windowed radix-2 FFT and publish no faster than 30 frames per second.
- Publish 128 waveform points and 32 logarithmic bands spanning 40 Hz through 16 kHz, capped at the current Nyquist frequency.
- Derive `lowEnergy` from 40–250 Hz, `midEnergy` from 250–2,000 Hz, and `highEnergy` from 2,000–16,000 Hz.
- Normalize FFT magnitude with `ln(1 + 8 * magnitude) / ln(9)`, clamped to `0f..1f`.
- Smooth each band with a 75% new / 25% old attack when rising and a 15% new / 85% old release when falling.
- Define the transient as the positive increase in overall RMS energy multiplied by four, clamped to `0f..1f`, with a 0.70 per-frame release.
- Use a fixed 131,072-sample mono ring. The producer publishes a monotonically increasing write count only after writing samples; the consumer copies the newest window and drops/retries a frame if a format/reset generation changes during the copy.
- Support Media3 PCM encodings 8-bit unsigned, 16-bit signed, 24-bit signed packed, 32-bit signed, and 32-bit float. Unsupported encodings publish `UNAVAILABLE` without affecting the PCM passed to playback.
- Use `TeeAudioProcessor` because Media3 1.8.0 guarantees that it outputs its input unmodified while providing the same input to `AudioBufferSink`. Install it through `DefaultAudioSink.Builder.setAudioProcessors(...)` in a custom `DefaultRenderersFactory`.
- Disable analysis work unless both playback is active and a visualizer mode is visible. The fixed handoff storage may remain owned by the application repository, but FFT working arrays and the worker job are created on activation and released on deactivation.
- Use `Phosphor`, `PhosphorDim`, `PhosphorMuted`, `PhosphorBright`, `TerminalBlack`, and the existing `Scanlines` component; add no new color preference or theme token.

## Tasks

### Task 1: Define immutable analysis and display-mode contracts (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalysisModelsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalysisModels.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayMode.kt`

**Test first:**

Create tests that assert:

```kotlin
assertEquals(AnalysisStatus.IDLE, AudioAnalysisFrame.Idle.status)
assertEquals(128, AudioAnalysisFrame.Idle.waveform.size)
assertEquals(32, AudioAnalysisFrame.Idle.bands.size)
assertEquals(VisualizerDisplayMode.RADAR, VisualizerDisplayMode.ART.next())
assertEquals(VisualizerDisplayMode.BANDS, VisualizerDisplayMode.RADAR.next())
assertEquals(VisualizerDisplayMode.SCOPE, VisualizerDisplayMode.BANDS.next())
assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.SCOPE.next())
```

Also assert the labels and accessibility names are exactly `ART 1/4` / `Album art`, `RADAR 2/4` / `Circular radar`, `BANDS 3/4` / `Spectrum bars`, and `SCOPE 4/4` / `Oscilloscope`.

Run the targeted tests and confirm they fail because the contracts do not exist.

**Implementation:**

Add:

- `enum class AnalysisStatus { IDLE, ACTIVE, UNAVAILABLE }`.
- `data class AudioAnalysisFrame` with `waveform: List<Float>`, `bands: List<Float>`, `energy`, `lowEnergy`, `midEnergy`, `highEnergy`, `transient`, `frameId`, and `status`.
- A companion `Idle` instance containing immutable zero-filled lists of exactly 128 and 32 elements. Add an `Unavailable` instance with the same shapes.
- `enum class VisualizerDisplayMode(label: String, accessibilityName: String)` in the exact cycle above, with a `next()` function implemented explicitly rather than by ordinal arithmetic.

Do not add user settings or persistence.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalysisModelsTest' --tests '*VisualizerDisplayModeTest'
```

Both test classes pass.

### Task 2: Add the fixed-capacity latest-sample handoff (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/PcmSampleRingBufferTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/PcmSampleRingBuffer.kt`

**Test first:**

Cover these contracts with a small injected capacity, such as eight samples:

- A copy before enough samples exist returns `false` and leaves the destination zeroed.
- Writing `[1, 2, 3, 4]` and copying the latest four returns those values in order.
- Writing past capacity keeps only the newest samples.
- Calling `reset()` prevents pre-reset samples from appearing in a later copy and increments the generation.
- A copy whose generation changes during the operation returns `false` rather than publishing a mixed window.

Use only deterministic single-thread calls for ordering tests and one two-thread stress test that repeatedly writes numbered blocks while the reader verifies every successful window is monotonically ordered.

**Implementation:**

Create `PcmSampleRingBuffer(capacity: Int = 131_072)` backed by one preallocated `FloatArray`, an `AtomicLong` published write count, and an atomic generation. Provide:

```kotlin
fun write(sample: Float)
fun copyLatest(destination: FloatArray): Boolean
fun reset()
val generation: Long
```

The producer writes the array element before publishing the new count. `copyLatest` captures generation and write count, copies by absolute circular indexes, then verifies generation and overrun conditions. Do not use locks, channels, queues, or allocate from `write`.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*PcmSampleRingBufferTest'
```

All handoff and stress tests pass.

### Task 3: Decode PCM into the handoff without mutating Media3 buffers (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/PcmAnalysisBufferSinkTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/PcmAnalysisBufferSink.kt`

**Test first:**

Build direct native-order `ByteBuffer` fixtures for each supported encoding and assert:

- 8-bit unsigned min/center/max normalize near `-1f/0f/1f`.
- 16-, 24-, and 32-bit signed min/zero/max normalize near `-1f/0f/1f`.
- Float PCM clamps finite values to `-1f..1f` and converts non-finite values to zero.
- Stereo frames are averaged to mono; mono frames are unchanged.
- `handleBuffer` does not change the input buffer's position, limit, byte order, or bytes.
- `flush(sampleRate, channels, encoding)` updates format and resets prior samples.
- Unsupported encoding sets availability to false and writes nothing.
- When capture is disabled, `handleBuffer` writes nothing and performs no format conversion.

Run the targeted test and confirm it fails because the sink is absent.

**Implementation:**

Implement Media3's `TeeAudioProcessor.AudioBufferSink` with:

```kotlin
fun setCaptureEnabled(enabled: Boolean)
val sampleRateHz: Int
val available: Boolean
```

In `handleBuffer`, use absolute indexed reads so the read-only Media3 buffer is never advanced. Average all channels once per PCM frame and write normalized mono values to `PcmSampleRingBuffer`. Use `C.ENCODING_PCM_8BIT`, `PCM_16BIT`, `PCM_24BIT`, `PCM_32BIT`, and `PCM_FLOAT`; mark other encodings unavailable. Perform no allocation, blocking, logging, or exception throwing on the audio callback path.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*PcmAnalysisBufferSinkTest'
```

All encoding, downmix, reset, and passivity tests pass.

### Task 4: Implement the deterministic FFT primitive (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/Radix2FftTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/Radix2Fft.kt`

**Test first:**

Test a small size first:

- Construction rejects non-power-of-two sizes.
- Eight zero samples produce zero magnitude in every bin.
- An eight-sample cosine at one cycle per window peaks at bin one.
- Reusing the same FFT instance for different inputs does not leak prior results.

Then test a 2,048-sample 1 kHz sine at 48 kHz and assert the strongest bin is within one bin of `1000 * 2048 / 48000`.

**Implementation:**

Create an iterative in-place radix-2 Cooley–Tukey FFT that owns reusable real, imaginary, bit-reversal, and twiddle arrays. Its public operation accepts a `FloatArray` of exactly the configured size plus a caller-owned magnitude output of `size / 2 + 1`. It must not allocate during transforms.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*Radix2FftTest'
```

All zero, tone-location, and reuse tests pass.

### Task 5: Project PCM into waveform, frequency, and energy values (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerProjectionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Generate deterministic 2,048-sample fixtures and assert:

- Silence yields a 128-point zero waveform, 32 zero bands, zero RMS, and zero low/mid/high energy.
- A 100 Hz sine makes low energy larger than mid and high.
- A 1 kHz sine makes mid energy larger than low and high.
- An 8 kHz sine makes high energy larger than low and mid.
- The largest band for each tone contains that tone according to the logarithmic 40 Hz–16 kHz edges.
- Waveform downsampling chooses evenly spaced values from the full window and preserves sign.
- A 22.05 kHz sample rate caps band edges at Nyquist without indexing outside the FFT output.

**Implementation:**

Create `AudioAnalyzer` using one 2,048-sample input array, a precomputed Hann window, `Radix2Fft`, and preallocated magnitude/band working arrays. Compute RMS from unclipped normalized PCM, logarithmic bands from the mean magnitude of FFT bins covered by each band, and region energies from band values. Return a new immutable `AudioAnalysisFrame` only after all working calculations complete.

Keep band construction, waveform downsampling, and region aggregation as internal pure helpers so their edge behavior remains directly testable.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerProjectionTest'
```

All signal-location and shape tests pass.

### Task 6: Add attack, release, transient, and reset behavior (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerDynamicsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Using fixed sine and impulse windows, assert:

- A rising band applies the 75/25 attack coefficients.
- A falling band applies the 15/85 release coefficients.
- A sudden RMS increase produces a transient in `0f..1f`.
- A steady signal does not repeatedly retrigger the transient.
- Silence after a transient decays by 0.70 per frame.
- `reset()` clears band smoothing, prior energy, transient history, and frame numbering.
- Every published scalar and list value is finite and clamped to `0f..1f`.

**Implementation:**

Add dynamics state to `AudioAnalyzer` using the fixed coefficients in this plan. Increment `frameId` only when an active frame is successfully produced. Make `reset()` clear all mutable history while retaining reusable FFT allocations.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerDynamicsTest'
```

All dynamics and reset tests pass.

### Task 7: Coordinate analysis lifecycle and frame publication (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalysisRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalysisRepository.kt`

**Test first:**

Use `kotlinx-coroutines-test` and an injected dispatcher/delay interval to verify:

- Initial state is `AudioAnalysisFrame.Idle`.
- `setConsumerActive(true)` alone does not start FFT work while playback is inactive.
- `setPlaybackActive(true)` alone does not start FFT work with no visible consumer.
- When both flags are true and fresh samples exist, an `ACTIVE` frame is published.
- Disabling either flag cancels the worker, disables sink capture, clears analyzer history, and publishes `IDLE` immediately.
- `resetStream()` clears stale samples and smoothing before the next active frame.
- Unsupported sink format publishes `UNAVAILABLE` while mode consumption remains active.
- Repeated enable calls create only one worker.
- If multiple PCM windows arrive between ticks, the newest is analyzed and old windows are dropped.

**Implementation:**

Create an application-scoped repository owning `PcmSampleRingBuffer`, `PcmAnalysisBufferSink`, and a `MutableStateFlow<AudioAnalysisFrame>`. Expose only `StateFlow`, the sink required by the renderer factory, and these lifecycle calls:

```kotlin
fun setConsumerActive(active: Boolean)
fun setPlaybackActive(active: Boolean)
fun resetStream()
fun close()
```

Start a single background coroutine only when both flags are true. Create its `AudioAnalyzer` and FFT working storage at activation, poll at 33 ms, copy the newest 2,048 samples, and publish only when the ring's write count changed. Catch analysis exceptions at the worker boundary, publish `UNAVAILABLE`, and keep playback untouched. On deactivation, cancel the worker and drop the analyzer reference so FFT working arrays can be reclaimed.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalysisRepositoryTest'
```

All lifecycle, freshness, and failure-isolation tests pass.

### Task 8: Install the pass-through processor in Media3 (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerAudioProcessorTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/VisualizerRenderersFactory.kt`

**Test first:**

Create a `TeeAudioProcessor` around `PcmAnalysisBufferSink`, configure it for 16-bit stereo PCM, queue a direct fixture buffer, and assert:

- The processor output bytes are exactly equal to the input bytes.
- The sink receives the expected downmixed samples.
- Flushing the processor resets sink history.

Add a source guard that reads `VisualizerRenderersFactory.kt` and requires calls to `DefaultAudioSink.Builder(context)`, `setEnableFloatOutput(enableFloatOutput)`, `setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)`, and `setAudioProcessors(arrayOf(teeAudioProcessor))`.

**Implementation:**

Create `@UnstableApi internal class VisualizerRenderersFactory(context, bufferSink) : DefaultRenderersFactory(context)`. Construct one `TeeAudioProcessor(bufferSink)` and override Media3 1.8.0's:

```kotlin
protected fun buildAudioSink(
    context: Context,
    enableFloatOutput: Boolean,
    enableAudioTrackPlaybackParams: Boolean,
): AudioSink
```

Return a `DefaultAudioSink` configured with the two incoming flags and `setAudioProcessors(arrayOf(teeAudioProcessor))`. Do not enable float output, offload, passthrough, silence skipping, or playback-speed features beyond the values Media3 supplies.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerAudioProcessorTest'
.\gradlew.bat compileDebugKotlin
```

The processor is byte-identical and the Media3 1.8.0 override compiles.

### Task 9: Own the repository in the application and playback service (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerPlaybackWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt`, `app/src/main/AndroidManifest.xml`

**Test first:**

Add a source-wiring test that asserts:

- `NocturneLApplication` exposes one lazy `audioAnalysis` repository.
- `NocturneLPlaybackService` constructs `ExoPlayer.Builder(this, VisualizerRenderersFactory(this, app.audioAnalysis.bufferSink))`.
- Playback state changes call `setPlaybackActive(player.isPlaying)`.
- media-item transitions and position discontinuities call `resetStream()`.
- service destruction marks playback inactive.
- `AndroidManifest.xml` contains neither `RECORD_AUDIO` nor `MODIFY_AUDIO_SETTINGS`.

Run it first and confirm the expected wiring assertions fail.

**Implementation:**

Add the lazy application repository. In the service, obtain the application once, create the custom renderers factory, and pass it to the two-argument ExoPlayer builder. Extend the existing player listener without duplicating it:

- Update playback activity in `onIsPlayingChanged`.
- Reset analysis within `onEvents` when `EVENT_MEDIA_ITEM_TRANSITION` or `EVENT_POSITION_DISCONTINUITY` is present.
- Mark playback inactive before releasing the player in `onDestroy`.

Retain every existing audio attribute, noisy-device, restore, session, save-state, and queue behavior. Add no audio-capture permissions.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerPlaybackWiringTest'
.\gradlew.bat compileDebugKotlin
```

The wiring guard and production compilation pass.

### Task 10: Expose analysis through the existing playback connection (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playback/PlaybackAnalysisContractTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`

**Test first:**

Add a source-contract test that requires `PlaybackConnection` to:

- Expose `analysisState` as the application repository's read-only state flow.
- Provide `setVisualizerActive(active: Boolean)` delegating to `setConsumerActive`.
- Deactivate the consumer in `release()` before cancelling its scope.

The test must also reject adding visualization fields to `PlaybackUiState`, preserving separation from persisted/transport state.

**Implementation:**

Wire `PlaybackConnection` to the already-owned application repository. Do not duplicate frames into its one-second playback refresh loop and do not transport high-frequency analysis through MediaSession commands.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*PlaybackAnalysisContractTest'
```

The state-flow and lifecycle contract passes.

### Task 11: Generate radar geometry from deterministic frames (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/RadarGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Test pure geometry helpers with a 200-by-200 surface:

- Zero bands create 32 spoke endpoints on the base radius and keep every point in bounds.
- Raising band zero increases only its corresponding radial endpoint.
- Low/mid/high energies produce ordered inner, middle, and outer ring radii.
- A transient creates an echo radius outside the outer energy ring but within the clipped surface.
- Sweep angle is `(frameId * 2f) % 360f`, so it advances on PCM frames and is stable for an unchanged frame.

**Implementation:**

Add immutable internal geometry value types and `radarGeometry(frame, width, height)`. Use 32 equally spaced angles, four static grid rings, energy-driven inner/middle/outer radii, and a transient echo. Keep this file free of Compose dependencies so unit tests can validate all calculations.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*RadarGeometryTest'
```

All radar bounds and PCM-frame-driven sweep tests pass.

### Task 12: Generate spectrum and scope geometry (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/SpectrumGeometryTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/ScopeGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

For spectrum geometry, assert exactly 32 columns, segment counts clamped to the available height, bass-to-treble ordering preserved, and peak markers never below their bar tops.

For scope geometry, assert exactly 128 x-ordered points, zero waveform on the vertical centerline, `-1f` at the bottom inset, `1f` at the top inset, and all coordinates clipped to the square. Verify persistence trails use older supplied frames only when effects are enabled.

**Implementation:**

Add:

- `spectrumGeometry(frame, width, height)` using terminal-block segment heights and one peak marker per band.
- `scopeGeometry(frame, width, height)` mapping the normalized waveform to a centered polyline.
- A bounded three-frame UI-side history model for optional persistence trails; it clears on idle, unavailable status, mode change, and lower `frameId` after a reset.

Do not add a second smoothing implementation; use the analyzer's band smoothing.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*SpectrumGeometryTest' --tests '*ScopeGeometryTest'
```

Both geometry suites pass.

### Task 13: Draw the terminal visualizer scenes and fallback (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Create Compose tests against a stateless `TerminalVisualizerScene(mode, frame, effectsEnabled, modifier)` and assert:

- Radar, bands, and scope expose stable test tags `visualizer-radar`, `visualizer-bands`, and `visualizer-scope`.
- `UNAVAILABLE` displays `SIGNAL UNAVAILABLE` in every visualizer mode.
- `IDLE` does not display unavailable text and keeps the selected scene mounted.
- `effectsEnabled = true` includes `scanlines`; false excludes it.
- Scene nodes are not clickable; click ownership remains in the deck.

**Implementation:**

Draw all scenes with Compose `Canvas`, `VisualizerGeometry`, and existing terminal colors. Use `PhosphorMuted` for grids/trails, `PhosphorDim` for secondary traces, `Phosphor` for primary data, and `PhosphorBright` only for transient/peak accents. Add the existing `Scanlines` overlay only when effects are enabled. Use a black background and square clipping. Do not introduce gradients, non-terminal colors, or independent timer animations.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

If a device is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.TerminalVisualizersTest
```

### Task 14: Add the tappable display deck and accessibility behavior (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Compose the deck with a placeholder album-art lambda and fixed active frame. Assert:

- Initial tag is `visualizer-art` on every fresh composition.
- Four clicks produce radar, bands, scope, then art in exact order.
- Each state has one click action and a state description matching its accessibility name.
- The click label identifies the next mode.
- `onVisualizerActiveChanged` receives `false` initially, `true` on entering radar, stays active while cycling radar/bands/scope, and receives `false` on returning to art or disposal.
- `ART 1/4`, `RADAR 2/4`, `BANDS 3/4`, or `SCOPE 4/4` appears after a tap and disappears after advancing the test clock past the fixed 1,200 ms label duration.

**Implementation:**

Create `VisualizerDeck` with plain `remember`, never `rememberSaveable`, for its mode. Use one square `Box` with a single `clickable` modifier and content selected by mode. Use `DisposableEffect(mode != ART)` to notify activation without toggling off between visualizer-to-visualizer changes. Use an `Animatable` controlled by `LaunchedEffect`: snap alpha to 1 after a tap, hold for 800 ms, then animate to zero over 400 ms. The initial album-art composition must not show a label.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run the focused connected test when a device is available.

### Task 15: Replace the artwork slot on Now Playing with the deck (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Compose `NowPlayingScreen` with mutable playback and analysis state and assert:

- Album artwork or the existing `▓▓` placeholder is the initial square content.
- A tap enters radar without changing track title, transport controls, shuffle, repeat, seek bar, or Up Next.
- Updating to a different `PlaybackUiState.currentPath` while radar is selected leaves radar selected.
- Disposing and recomposing Now Playing returns to album art.
- The deck remains square and occupies exactly the former artwork position.

Update calls to `NowPlayingScreen` in existing UI fixtures only enough to satisfy the new parameters.

**Implementation:**

Add `analysisFrame: AudioAnalysisFrame` and `onVisualizerActiveChanged: (Boolean) -> Unit` to `NowPlayingScreen`. Replace only the existing artwork/placeholder branch with `VisualizerDeck`; pass the existing `CrtArtwork` or placeholder as the art lambda. Leave all metadata, notices, seek behavior, controls, queue rendering, padding, and framing unchanged.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run the focused connected test when available and confirm existing Now Playing tests still compile.

### Task 16: Wire live frames into Compose and stop work off-screen (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/VisualizerAppWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Add a source-wiring test requiring:

- `playback.analysisState.collectAsState()` alongside the existing playback state.
- `analysisFrame = analysisFrame` and `onVisualizerActiveChanged = playback::setVisualizerActive` in the Now Playing call.
- No analysis parameter on library, search, artist, album, playlist, or settings screens.

Run the test and confirm it fails on missing wiring.

**Implementation:**

Collect the analysis state once in `NocturneLApp` and pass it only to Now Playing. Rely on `VisualizerDeck` disposal to deactivate analysis when navigation removes the screen. Keep the destination, album selection, and playback connection lifetimes unchanged.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerAppWiringTest'
.\gradlew.bat compileDebugKotlin
```

Both checks pass.

### Task 17: Add deterministic visual screenshot coverage (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Add fixed preview frames and stateless scene previews for:

- Radar with strong low energy and a transient, effects on.
- Spectrum with a bass-to-treble band pattern, effects on.
- Oscilloscope with a deterministic sine waveform, effects on.
- Oscilloscope with the same waveform, effects off.
- Radar idle.
- Bands unavailable with `SIGNAL UNAVAILABLE`.

Update the existing Now Playing preview only with the new default idle frame and no-op activation callback; it must continue to show album art by default.

Run screenshot validation first. New previews should fail because references do not exist; existing references should remain unchanged.

**Implementation:**

Inspect every newly rendered preview for square clipping, readable green hierarchy, terminal styling, label-free deterministic scenes, effects differences, and correct fallback text. Generate only the six new reference images. If an existing reference changes unexpectedly, fix the implementation instead of accepting that change unless it is the unavoidable approved artwork-slot wrapper difference and has been visually reviewed.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
```

Validation passes with only reviewed visualizer references added or intentionally changed.

### Task 18: Extend Pixel 7 audio/visual verification (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Add unchecked release-checklist entries for:

- Exact tap cycle and album-art reset after leaving/re-entering Now Playing.
- Obvious kick/snare alignment in all three visualizers.
- Pause, resume, seek, next, previous, and gapless transition behavior.
- Rapid mode switching without audible glitches.
- Silence and missing/unavailable signal states.
- Effects on/off rendering.
- A 30-minute visualization session checking dropped audio, animation smoothness, heat, and battery impact.

**Implementation:**

Run the checklist on the Pixel 7 using at least one bass-heavy track, one percussion-heavy track, one quiet passage, and one known gapless album transition. Record any failure as a follow-up issue; do not loosen automated thresholds or add unplanned tuning in this task.

**Verify:** Every new checklist item is either checked with the tested build identifier noted in the commit/PR description or explicitly reported as a release blocker.

### Task 19: Run regression, scope, and audio-safety checks (2–5 min)

**Files:** all files changed by Tasks 1–18

**Test first:**

Run targeted scope checks:

```powershell
rg -n 'RECORD_AUDIO|MODIFY_AUDIO_SETTINGS|android\.media\.audiofx\.Visualizer' app
rg -n 'rememberSaveable' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer
rg -n 'delay\(|infiniteRepeatable|rememberInfiniteTransition' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt
```

The first and third commands return no matches. `rememberSaveable` must not be used for display mode. A delay is allowed only in `VisualizerDeck` for the 1,200 ms temporary label, not in the Canvas scenes.

**Implementation:**

Fix only failures introduced by this feature. Do not add permissions, system-wide capture, offline analysis, settings, persistence, full-screen UI, new visualizer types, semantic beat detection, or unrelated playback/UI refactors.

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

If a device is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Confirm only the approved visualizer implementation, its tests/references, the approved design, this plan, and the Pixel 7 checklist are modified.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] All new production behavior was introduced after a failing test or contract check.
- [ ] The display starts on album art on every Now Playing visit.
- [ ] Tapping cycles exactly through art, radar, bands, scope, and art.
- [ ] Track changes retain the selected mode during the same visit.
- [ ] PCM is captured from NocturneL's Media3 playback pipeline with no microphone/system-capture permission.
- [ ] The audio processor's output is byte-identical to its input.
- [ ] Audio-thread work performs no blocking, I/O, logging, or per-buffer allocation.
- [ ] FFT work runs only while playback is active and a visualizer is visible.
- [ ] Silence, pause, seek, transitions, format changes, unavailable signal, and teardown behave as designed.
- [ ] Radar, spectrum, and scope match the approved terminal-green visual language with effects on and off.
- [ ] Unit tests, Android-test assembly, Compose tests, screenshot validation, lint, and debug assembly pass.
- [ ] Pixel 7 playback remains glitch-free and perceptually synchronized during the extended device check.
- [ ] No unplanned files are modified.
