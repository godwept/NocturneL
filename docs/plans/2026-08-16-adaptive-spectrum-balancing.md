# Adaptive Spectrum Balancing Implementation Plan

**Date:** 2026-08-16  
**Design doc:** `docs/specs/2026-08-16-adaptive-spectrum-balancing-design.md`  
**Status:** Ready for review

## Overview

Calibrate the existing PCM spectrum analysis so quiet music fills a useful portion of the display and upper-frequency content remains visible without flattening the musical balance. The change stays inside `AudioAnalyzer`: apply a capped frequency lift, blend neighboring logarithmic bands, track one shared spectral envelope, derive one bounded adaptive gain, and gate near-silence before the existing temporal attack/release stage. The published frame model, FFT, playback path, and Compose renderer remain unchanged.

## Fixed Implementation Decisions

- Keep the existing 32 logarithmic bands from 40 Hz through 16 kHz and the existing `ln(1 + 8 * magnitude) / ln(9)` magnitude mapping.
- Begin frequency compensation at 800 Hz. Use `(centerFrequency / 800 Hz)^0.75`, equivalent to approximately +4.5 dB per octave, and cap the multiplier at `4.25f`. This calibrated curve offsets both natural roll-off and duplicated low FFT bins while remaining a single predictable tilt.
- Blend interior bands with weights `0.25 left + 0.50 center + 0.25 right`. Blend edge bands with `0.70 self + 0.30 neighbor` so all weights sum to one.
- Measure the shared spectral level as the maximum spatially smoothed band. Do not normalize bands independently.
- Track the level envelope with `0.65` new / `0.35` old when level rises and `0.08` new / `0.92` old when it falls.
- Derive target gain as `0.78 / max(levelEnvelope, 0.0001)`, clamped to `0.35f..64f`. The high ceiling is required for genuinely quiet audible material and is protected by the RMS silence gate.
- Move adaptive gain toward a lower target with `0.65` new / `0.35` old and toward a higher target with `0.08` new / `0.92` old. Initialize gain to `1f`.
- Treat RMS energy below `0.002f` as silence. Silence supplies zero target bands to the existing temporal release and does not update the level envelope or adaptive gain.
- Reuse one additional `FloatArray(BAND_COUNT)` for compensated/spatially smoothed values. Do not allocate working collections per analysis frame.
- Apply the stages in this exact order: FFT projection, frequency compensation, neighbor smoothing, global adaptive gain, existing per-band temporal attack/release.
- Keep RMS energy, waveform samples, and transient calculation based on the original sanitized PCM so visual balancing cannot change loudness or transient semantics.

## Tasks

### Task 1: Add deterministic spectrum fixtures and frequency-lift coverage

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Create `AudioAnalyzerSpectrumBalancingTest` in the existing visualizer package. Add a reusable `sine(frequencyHz, amplitude, sampleRateHz = 48_000)` fixture that returns exactly `AudioAnalyzer.FFT_SIZE` samples. Add tests that call an internal, calculation-only `frequencyLift(centerFrequencyHz)` seam and assert:

```kotlin
assertEquals(1f, analyzer.frequencyLift(100f), .001f)
assertEquals(1f, analyzer.frequencyLift(800f), .001f)
assertEquals(2.828f, analyzer.frequencyLift(3_200f), .001f)
assertEquals(4.25f, analyzer.frequencyLift(16_000f), .001f)
```

Also iterate representative finite frequencies from `40f..16_000f` and assert that lift is monotonic, finite, and always in `1f..4.25f`. Run the focused test and confirm it fails because the seam and compensation constants do not exist.

**Implementation:**

In `AudioAnalyzer`, group the new tuning values in the companion object as `FREQUENCY_LIFT_START_HZ = 800f`, `FREQUENCY_LIFT_EXPONENT = .75f`, and `MAX_FREQUENCY_LIFT = 4.25f`. Add:

```kotlin
internal fun frequencyLift(centerFrequencyHz: Float): Float
```

Return `1f` at and below 800 Hz; above 800 Hz return `(centerFrequencyHz / 800f).pow(.75f).coerceAtMost(4.25f)`. Keep this helper calculation-only and do not change `projectBands` yet.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest'
```

All frequency-lift tests pass while existing analyzer behavior remains unchanged.

### Task 2: Add neighbor smoothing with reusable storage

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Add tests for an internal `smoothNeighborBands(source, destination)` seam using 32-element arrays:

- A single `1f` at interior index 16 produces `.25f`, `.5f`, `.25f` at indexes 15–17 and zero elsewhere.
- A single `1f` at index 0 produces `.70f` at index 0 and `.25f` at index 1; separately verify the final edge mirrors this behavior.
- A constant source remains constant, proving normalized weights do not change a flat spectrum.
- Non-finite and out-of-range inputs are sanitized so every destination value is finite and in `0f..1f`.
- Passing arrays whose lengths differ from `BAND_COUNT` fails with `require` rather than silently producing partial output.

Run the focused class and confirm the new tests fail before implementation.

**Implementation:**

Add one preallocated `balancedBands = FloatArray(BAND_COUNT)` to `AudioAnalyzer`. Add named constants `NEIGHBOR_WEIGHT = .25f`, `CENTER_WEIGHT = .50f`, and `EDGE_SELF_WEIGHT = .70f`. Implement `smoothNeighborBands` without allocation or in-place mutation: sanitize each read, use `.70/.30` at the two edges, and `.25/.50/.25` for interior indexes. Do not connect it to `analyze()` yet.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest'
```

All frequency-lift and neighbor-smoothing tests pass.

### Task 3: Integrate compensation and spatial smoothing into band projection

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Add a mixed-tone test using one PCM window containing equal low-amplitude tones at 400 Hz and 6,400 Hz. Locate each tone's logarithmic band using `bandEdge`, then assert after one analyzed frame:

- Both target neighborhoods contain non-zero output.
- The compensated 6,400 Hz neighborhood is at least half as strong as the 400 Hz neighborhood, keeping the right side visibly active without flattening the spectrum.
- The strongest band remains within one band of either injected tone rather than moving to an unrelated frequency.

Add a single-tone test asserting the target band remains the local maximum while each immediate neighbor is positive and lower than the target. Confirm these tests fail with the current raw projection.

**Implementation:**

After `projectBands(sampleRateHz)`:

1. Aggregate the bins in each logarithmic band using root-sum-square magnitude so each band represents its total spectral energy and high bands are not disproportionately diluted by their larger bin counts.
2. For each band, calculate its geometric center from adjacent `bandEdge` values.
3. Multiply `rawBands[band]` by `frequencyLift(center)` and clamp the result to `0f..1f` in place.
4. Call `smoothNeighborBands(rawBands, balancedBands)`.
5. Feed `balancedBands`, rather than `rawBands`, into the existing per-band temporal attack/release loop.

Do not add adaptive gain or silence gating in this task. Preserve the existing attack/release coefficients exactly.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest' --tests '*AudioAnalyzerTest'
```

The mixed-tone and local-peak tests pass, and low/mid/high tone classification remains correct.

### Task 4: Add the shared spectral envelope and adaptive gain

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Add integration tests that repeatedly analyze deterministic PCM rather than reading private state:

- Analyze a `0.02f`-amplitude 1 kHz sine for 60 frames and assert the final peak band is in `0.60f..0.90f`, demonstrating useful quiet-signal expansion without clipping.
- Analyze the same waveform at amplitudes `0.02f` and `0.20f` with separate settled analyzers. Normalize each returned band list by its maximum and assert corresponding values differ by no more than `0.12f`, demonstrating preservation of spectral shape.
- Settle an analyzer on the quiet signal, then feed ten `0.90f`-amplitude multitone frames. Assert every published band stays in `0f..1f` and fewer than one quarter of the bands equal `1f`, demonstrating rapid gain reduction rather than widespread clipping.
- Feed a steady quiet signal and assert the maximum band does not reverse direction by more than `.03f` between consecutive late frames, guarding against visible gain pumping.

Run the focused tests and confirm quiet expansion fails before adaptive gain exists.

**Implementation:**

Add `levelEnvelope = 0f` and `adaptiveGain = 1f` fields plus these grouped constants: `TARGET_SPECTRUM_PEAK = .78f`, `MIN_LEVEL = .0001f`, `MIN_ADAPTIVE_GAIN = .35f`, `MAX_ADAPTIVE_GAIN = 64f`, `LEVEL_RISE = .65f`, `LEVEL_FALL = .08f`, `GAIN_REDUCTION = .65f`, and `GAIN_EXPANSION = .08f`.

After neighbor smoothing, take the maximum finite value in `balancedBands`. Move `levelEnvelope` toward that value using the rise coefficient when the value is higher and the fall coefficient otherwise. Calculate the clamped target gain, then move `adaptiveGain` toward it quickly when gain must decrease and slowly when it may increase. Multiply every `balancedBands` value by `adaptiveGain`, clamp to `0f..1f`, and then run the existing per-band temporal smoothing.

Keep this calculation shared across all bands. Do not retain per-band maxima or gain values.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest' --tests '*AudioAnalyzerTest'
```

Quiet expansion, spectral-shape, loud-transition, pumping, and existing analyzer tests pass.

### Task 5: Gate silence and reset all adaptive state

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalyzer.kt`

**Test first:**

Add tests covering the lifecycle boundary:

- Analyze 90 frames of alternating `+0.0005f/-0.0005f` PCM and assert every band remains exactly zero even though the samples are not digital zero.
- Settle on a quiet audible signal, analyze silence, and assert the band peak decreases across subsequent silence frames through the existing temporal release.
- After settling adaptive gain on quiet music, call `reset()` and assert the first frame for a fixed signal exactly matches the first frame from a fresh analyzer within `.0001f` for every band.
- Analyze arrays containing `NaN`, positive infinity, and negative infinity and assert every frame scalar and band remains finite and clamped.

Confirm the near-silence and adaptive-reset tests fail before implementation.

**Implementation:**

Move RMS calculation early enough that the balancing stage can use it without changing the existing `energy` value. Add `SILENCE_RMS = .002f`. When energy is below the threshold, fill `balancedBands` with zero and skip both level-envelope and adaptive-gain updates; still run the existing band release loop so visible bars settle smoothly.

Extend `reset()` to clear `balancedBands` and `levelEnvelope` and restore `adaptiveGain` to `1f`, in addition to its existing state resets. Preserve waveform output, transient calculation, frame numbering, and active status.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest' --tests '*AudioAnalyzerTest'
```

Silence, decay, reset, finite-value, and existing analyzer tests pass.

### Task 6: Add a pink-spectrum end-to-end calibration test

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalyzerSpectrumBalancingTest.kt`

**Test first:**

Add a deterministic `pinkMultitone` fixture composed from tones at 100, 200, 400, 800, 1,600, 3,200, 6,400, and 12,800 Hz. Scale each tone by `baseAmplitude / sqrt(frequency / 100f)`, sum them, and apply one fixed normalization factor so no PCM sample exceeds `0.12f`. Analyze 60 identical frames, then assert:

- The mean of bands 16–31 is at least `45%` of the mean of bands 0–15.
- At least 12 of the 16 right-half bands render above `0.08f`.
- The frame maximum is in `0.60f..0.90f`.
- The left-half mean remains greater than the right-half mean, preserving the fixture's bass-heavy character.

Run this test before any final tuning. If it fails, adjust only the named balancing constants within the bounds established by Tasks 1–5; do not change the FFT, band edges, renderer, public frame model, or PCM path.

**Implementation:**

No new production structure is expected. Calibrate only `MAX_FREQUENCY_LIFT`, envelope coefficients, target peak, adaptive-gain bounds, or neighbor weights if the end-to-end fixture demonstrates that the approved behavior is not met. Keep the +3 dB/octave formula, 800 Hz starting point, silence threshold, shared-gain model, and stage ordering fixed.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest'
```

All focused balancing tests pass with the final constants documented by their names in `AudioAnalyzer`.

### Task 7: Extend focused Pixel 7 visual calibration coverage

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Run:

```powershell
rg -n 'right half|quiet passage|treble-rich' docs/testing/pixel-7-release-checklist.md
```

Confirm the current checklist has no explicit coverage for adaptive spectrum width, quiet-signal expansion, or treble compensation.

**Implementation:**

Add unchecked checklist entries requiring the tester to:

- Play a balanced or pink-noise-like track and confirm the right half remains visibly active without becoming stronger than the left by default.
- Play a quiet passage and confirm the spectrum expands gradually without pumping, then transitions promptly into a loud passage without widespread clipping.
- Play a treble-rich track and confirm upper bands respond while silence and near-silence still settle to baseline.
- Seek and change tracks after a quiet passage and confirm learned gain does not carry into the new signal.

Do not alter or remove existing playback-safety and visualizer checks.

**Verify:** Re-run the `rg` command and confirm all three signal scenarios are present. On the target Pixel 7, perform the new checks and record the build identifier in the PR description; report failures as blockers rather than weakening automated thresholds.

### Task 8: Run regression and scope checks

**Files:** all files changed by Tasks 1–7

**Test first:**

Run the focused analyzer suite, then the full unit suite. Treat any failure as a regression to resolve only within the approved analyzer balancing scope.

**Implementation:**

Fix only failures introduced by adaptive spectrum balancing. Do not change `AudioAnalysisFrame`, `Radix2Fft`, playback PCM processing, visualizer geometry, Compose drawing, settings, or user preferences. Do not add per-band gain state or per-frame working allocations.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalyzerSpectrumBalancingTest' --tests '*AudioAnalyzerTest'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
git status --short
```

If a device is attached, also run `connectedDebugAndroidTest`. Confirm only `AudioAnalyzer.kt`, the new analyzer balancing test, the Pixel 7 checklist, and the approved design/plan documents are modified. Existing deterministic visualizer screenshots should remain unchanged because their fixed `AudioAnalysisFrame` fixtures and renderer are unchanged.

## Definition of Done

- [ ] All tasks completed in order with each production change preceded by a failing test.
- [ ] Quiet audible signals expand to a useful display height without clipping.
- [ ] A deterministic pink-spectrum fixture produces meaningful activity in both halves while retaining bass emphasis.
- [ ] Frequency compensation is gentle, monotonic, and capped.
- [ ] Neighbor smoothing reduces jaggedness without erasing local peaks.
- [ ] One shared adaptive gain preserves relationships between bands.
- [ ] Sudden loud sections reduce gain promptly and quiet sections expand without pumping.
- [ ] Digital silence and sub-threshold noise settle to zero and do not raise gain.
- [ ] Analyzer reset clears all adaptive state.
- [ ] RMS energy, waveform, transient behavior, public frame contracts, playback PCM, and rendering APIs remain unchanged.
- [ ] Focused analyzer tests and the complete unit suite pass.
- [ ] Android-test assembly, lint, and debug assembly pass.
- [ ] Pixel 7 checks pass for balanced, quiet, loud-transition, treble-rich, silence, seek, and track-change scenarios.
- [ ] No unplanned files are modified.
