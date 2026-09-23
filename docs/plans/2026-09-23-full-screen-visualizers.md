# Full Screen Visualizers Implementation Plan

**Date:** 2026-09-23

**Design doc:** docs/specs/2026-09-23-full-screen-visualizers-design.md

**Status:** Ready for review

## Overview

Add an immersive visualization view to the existing Compose activity. Long pressing the NOW display opens Radar, Spectrum, or Frequency Grid; a left swipe advances and a right swipe reverses through those three modes. A tap briefly reveals Exit. Radar and Grid remain centered squares with a soft pulse glow in the unused vertical space. The NOW display and expanded view share one selected mode and one continuous audio-analysis activation state.

For each named instrumentation class below, run the class-filtered connected test on an attached emulator or device, for example `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDeckTest`; substitute the fully qualified class named in that task. A new test should fail for the missing behavior before the corresponding implementation is added.

## Tasks

### Task 1: Model the shared visualization session

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSessionStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSessionState.kt`

**Test first:** Assert that the initial state is `ART` and closed; opening from `ART` selects `RADAR`; opening from each audio mode preserves that mode; left swipes cycle `RADAR → BANDS → GRID → RADAR`; right swipes reverse that cycle; close preserves the selected mode. Assert `analysisNeeded(nowVisible)` stays true before and after entering or leaving full screen in an audio mode, and is false for art or a hidden NOW screen. Swipes while closed leave the state unchanged.

**Implementation:** Add an immutable `VisualizerSessionState(mode, expanded)` with `open()`, `close()`, `swipeLeft()`, `swipeRight()`, and `analysisNeeded(nowVisible)` transitions. Keep the existing four-mode short-tap cycle in `VisualizerDisplayMode.next()` unchanged. Store no session state in preferences.

**Verify:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerSessionStateTest"`.

### Task 2: Make the NOW display use a supplied mode

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:** Update `VisualizerDeckTest` to hold `mode` in its Compose test harness and pass a callback that updates it. Keep assertions for the existing ART/RADAR/BANDS/GRID short-tap order, mode label, square bounds, and sync controls. Add a test that an externally selected `GRID` is rendered without stepping through the cycle.

**Implementation:** Replace `VisualizerDeck`'s private remembered mode with required `mode: VisualizerDisplayMode` and `onModeChange: (VisualizerDisplayMode) -> Unit` inputs; short tap calls `onModeChange(mode.next())`. For this task, let `NowPlayingScreen` temporarily remember and pass the mode. Leave the current analysis callback in place until Task 11 moves ownership above both views.

**Verify:** Run the `VisualizerDeckTest` instrumentation class; the old display interactions still pass.

### Task 3: Add long press entry on NOW

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:** Long press the art and an audio mode in the existing NOW display tests. Assert the expansion callback fires once and the short-tap mode callback does not fire. Assert a normal tap still advances exactly one mode. Assert the long-click semantics expose an accessible “Open full screen visualization” action.

**Implementation:** Add `onExpand` through `NowPlayingScreen` to `VisualizerDeck`. Use a combined click/long-click gesture on the visualizer surface, preserving sync-control hit targets and the existing short-tap behavior.

**Verify:** Run `VisualizerDeckTest` and `NowPlayingVisualizerTest` on an emulator or device.

### Task 4a: Pass mode through the NOW screen

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingQueueTest.kt`, `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:** Adapt standalone NOW tests and the screenshot preview to supply mode state and a mode-change callback. Add a NOW test that changing the supplied mode from outside immediately changes the rendered visualizer while a track change leaves it selected.

**Implementation:** Make `NowPlayingScreen` take `visualizerMode`, `onVisualizerModeChange`, and `onExpandVisualizer` as explicit inputs and forward them to `VisualizerDeck`. For this intermediate step, let `NocturneLApp` remember the selected mode above `TerminalScaffold` and pass it through; opening full screen is connected in Task 10b.

**Verify:** Run `NowPlayingVisualizerTest`, `NowPlayingControlsTest`, and `NowPlayingQueueTest`, then `.\gradlew.bat :app:validateDebugScreenshotTest` to compile the preview and confirm its reference is unchanged.

### Task 4b: Keep the session state across view replacement

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:** In a controlled Compose host, select Grid, remove the NOW child from composition, reinsert it with the same parent session, and assert Grid remains selected. Assert opening from art changes parent session mode to Radar.

**Implementation:** Replace the temporary remembered mode in `NocturneLApp` with one remembered `VisualizerSessionState`. Have NOW mode changes copy the new mode into the session. Keep this state above the content that full screen will replace.

**Verify:** Run `NowPlayingVisualizerTest`; remounting the NOW child retains the parent mode.

### Task 5a: Create the expanded view and Exit control

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`

**Test first:** Render `FullScreenVisualizer` directly in a Compose test with `RADAR`. Assert the audio scene is displayed, app navigation and playback controls are absent, Exit starts hidden, a tap reveals an accessible Exit control, and pressing Exit calls `onExit` once without cycling the mode.

**Implementation:** Add a full-window Compose view using the theme background and existing `TerminalVisualizerScene`. Keep Exit as a separate hit target above the gesture surface, positioned inside safe drawing insets. Hold Exit visibility locally; receive `mode`, `frame`, `effectsEnabled`, and callbacks from the parent. Do not add sync or playback controls.

**Verify:** Run `FullScreenVisualizerTest` on an emulator or device.

### Task 5b: Hide Exit after a fresh tap timeout

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`

**Test first:** With the Compose test clock paused, tap to reveal Exit, advance just short of three seconds and assert it remains visible, tap again, advance through the first timeout and assert it still shows, then advance through the new timeout and assert it hides.

**Implementation:** Restart a local three-second hide job on every visualization tap. Cancel that job when Exit is pressed or the view leaves composition.

**Verify:** Run `FullScreenVisualizerTest`; the timer is deterministic under the test clock.

### Task 6: Add deliberate horizontal swipe navigation

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`

**Test first:** Swipe left and right across the center of the view and assert one corresponding callback per gesture; test both ends of the mode cycle through the supplied mode callback. Assert a tap, a short movement, and a mostly vertical drag do not switch modes or accidentally reveal Exit. Assert a swipe does not activate Exit.

**Implementation:** Detect a horizontal drag only after touch slop and a deliberate travel threshold; choose left for `onSwipeLeft` and right for `onSwipeRight`. Consume the recognized gesture so it does not also count as a tap. Keep the gesture surface clear of the Exit target and system edge gesture areas.

**Verify:** Run `FullScreenVisualizerTest`; all tap, timer, and swipe cases pass together.

### Task 7: Define the ambient glow signal

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAmbientGlowTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAmbientGlow.kt`

**Test first:** Assert `ambientGlowAlpha(mode, frame, effectsEnabled)` is zero for BANDS, ART, idle, unavailable, and disabled effects. For active Radar and Grid, assert zero transient produces zero alpha, finite transient rises monotonically and is capped, and `NaN`/infinite/negative values produce zero. Pass the existing effective effects flag so reduced motion disables the glow without a second settings path.

**Implementation:** Add a small pure alpha calculation driven by `AudioAnalysisFrame.transient`, clamped to a subtle maximum. Reuse a theme visualizer color at render time; do not use `TerminalPalette.glowStrength`, which is zero for several themes. Add no new beat detection or persistence.

**Verify:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerAmbientGlowTest"`.

### Task 8a: Size full screen scenes without distortion

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`

**Test first:** On a tall test viewport, assert Radar and Grid scene bounds are square and centered, while BANDS scene bounds fill the view. Check a narrow viewport does not clip the square.

**Implementation:** Measure the available viewport. Give Radar and Grid the shorter dimension as width and height; give BANDS the full viewport. Keep the existing scene geometry and scanlines unchanged.

**Verify:** Run `FullScreenVisualizerTest`; inspect a phone-sized emulator frame for clipping.

### Task 8b: Draw the audio pulse in square margins

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAmbientGlow.kt`

**Test first:** Capture a deterministic full screen frame and compare sampled pixels above and below the square for active Radar/Grid versus idle or unavailable frames. Assert BANDS has no separate margin glow and changing the theme changes the glow color.

**Implementation:** Draw a soft gradient behind the Radar/Grid square, fading from its upper and lower edges toward the outer screen edges. Use the Task 7 alpha and `TerminalTheme.palette.visualizerPrimary`; never cover the visualizer content or Exit control.

**Verify:** Run `FullScreenVisualizerTest`; visually inspect the pulse at low and high transient values.

### Task 9: Add scoped immersive system bars

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/ImmersiveSystemBarsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/ImmersiveSystemBars.kt`

**Test first:** In a host activity, record which status/navigation bars are visible, compose the immersive effect, assert they hide, dispose the effect, and assert the original visibility returns. Repeat across a re-created host view and verify the full-screen content still fills the window. Allow system insets to settle before assertions.

**Implementation:** Add a Compose `DisposableEffect` using `WindowCompat.getInsetsController` and `WindowInsetsCompat.Type.systemBars()` to hide bars while the expanded view is composed and restore each bar to its prior state on disposal. Use transient-bars-by-swipe behavior so Android edge navigation remains available. Keep Exit padded for safe drawing and display cutouts.

**Verify:** Run `ImmersiveSystemBarsTest` on an emulator or device using gesture navigation.

### Task 10a: Contain the screen switch and Back handling

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerPresentationTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerPresentation.kt`

**Test first:** Compose the presentation host with a small test NOW content slot. Assert closed session shows that slot; expanded session shows `FullScreenVisualizer` instead; Exit and Android Back both request `session.close()`. Assert the host forwards the current mode and audio frame.

**Implementation:** Add a focused `VisualizerPresentation` composable that chooses between its normal-content slot and `FullScreenVisualizer`, supplies the current session mode and audio frame, composes `ImmersiveSystemBars` only while expanded, and installs a `BackHandler` while expanded. It calls the tested session transitions for Exit and swipes.

**Verify:** Run `VisualizerPresentationTest` on an emulator or device.

### Task 10b: Use the presentation host in the app

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerPresentationTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:** Extend the presentation test with a controlled NOW display: long press from art opens Radar; two left swipes reach Grid; Exit returns to NOW with Grid selected; reopening starts at Grid.

**Implementation:** Wrap the existing `TerminalScaffold` branch in `VisualizerPresentation` after the source-setup branch. Pass the session, analysis frame, effects flag, and normal-content slot. Connect NOW's long press to `session.open()` and keep the existing destination routing inside the normal-content slot.

**Verify:** Run `VisualizerPresentationTest` and manually repeat the flow in the real app; the app chrome is absent while expanded.

### Task 11: Transfer analysis activation above both views

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerPresentationTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerPresentation.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:** In `VisualizerPresentationTest`, record the analysis-active callback while switching NOW audio → full screen → NOW audio; it must never emit `false` between the views. Entering art or leaving NOW must emit `false`. Update deck tests so they no longer expect the deck to own analysis activation. Cover idle and unavailable frames, which still keep the consumer active while an audio mode is visible.

**Implementation:** Remove the deck-level `DisposableEffect` and `onVisualizerActiveChanged` plumbing. Have `VisualizerPresentation` derive whether analysis is needed from `VisualizerSessionState.analysisNeeded(nowVisible)` and call its `onVisualizerActiveChanged` callback only when that Boolean changes, then send `false` when the host leaves composition. In `NocturneLApp`, pass `nowVisible` from the NOW destination and queue-editor state, and pass `playback::setVisualizerActive`. Keep playback-connection disposal cleanup.

**Verify:** Run the focused Compose tests and `VisualizerSessionStateTest`; observe no analysis interruption during entry or exit on a playing device.

### Task 12: Verify the complete feature

**Files:** No source changes expected.

**Test first:** Review `git diff --check` and `git status --short`. Confirm the approved design file is preserved and only planned implementation and test files changed. Resolve any failing test before considering the task complete.

**Implementation:** Run `.\gradlew.bat :app:testDebugUnitTest`, `.\gradlew.bat :app:connectedDebugAndroidTest`, `.\gradlew.bat :app:validateDebugScreenshotTest`, and `.\gradlew.bat :app:assembleDebug`. On a device or emulator, check portrait Radar/Grid centering and glow, full-height Spectrum, a display cutout, gesture navigation, transient Android bars, Exit and Back restoration, reduced motion, and idle/unavailable audio.

**Verify:** All automated tests pass, the debug APK exists, and the manual checks match the approved design.

## Definition of Done

- [ ] All tasks are completed in order, with each task's tests written before its implementation.
- [ ] Long press, swipe, Exit, Back, immersive bars, and mode retention behave as approved.
- [ ] Radar and Grid pulse in the unused vertical space while respecting CRT and reduced motion settings.
- [ ] Audio analysis remains active across the transition between NOW and full screen.
- [ ] Unit and instrumentation tests pass and a debug APK builds.
- [ ] No unrelated files are modified.
