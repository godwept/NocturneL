---
layout: manual
title: Visualizers and sync
description: Cycle visual modes, open full screen visualizers, and align visuals with audio.
section: Listening
nav_order: 50
permalink: /manual/visualizers/
---

## Cycle display modes

Tap the square display on **NOW** to cycle through four modes:

1. **Album art**
2. **Circular radar**
3. **Spectrum bars**
4. **Frequency grid**

The current mode label appears briefly after each tap. Live audio analysis runs only while a visualizer is visible. **SIGNAL UNAVAILABLE** means the active playback path is not currently providing analyzable audio; playback itself may continue normally.

## Full screen visualizers

Long-press the display on **NOW** to open the current visualizer in full screen. If **Album art** is showing, full screen opens at **Circular radar**. Full screen cycles through Circular radar, Spectrum bars, and Frequency grid; album artwork stays on NOW. The app controls and Android system bars hide while full screen is open.

- **Swipe left** for the next visualizer: Circular radar → Spectrum bars → Frequency grid → Circular radar.
- **Swipe right** to move through those three modes in reverse.
- **Tap** the display to reveal **EXIT**. It hides again after about three seconds.
- Select **EXIT** or use **Android Back** to return to NOW. The last full screen mode remains selected on NOW.

Radar and Grid stay centered as squares on a tall screen; Spectrum uses the available height. With CRT effects enabled and active audio analysis, a soft beat glow spreads into the space around Radar and Grid. Sync controls remain on NOW.

## CRT effects and motion

Visualizer afterglow, scanline treatment, cover movement, and related terminal effects follow **CRT EFFECTS** in Settings. If Android's reduced-motion preference is enabled, NocturneL pauses optional motion even when CRT EFFECTS remains saved as on.

## Adjust visualizer sync

Radar, Spectrum, and Frequency Grid modes show sync controls in the upper corners:

- Select **−** to decrease the offset by **25 ms**.
- Select **+** to increase the offset by **25 ms**.
- Hold **−** or **+** to repeat the **25 ms** adjustment and accelerate during a continued hold.
- Select the **VIS SYNC** label to reset to `0 ms`.

The supported range is **-2000 ms** through **+2000 ms**. Adjust by observation until visual changes align with what you hear. Device audio pipelines differ, so one offset is not guaranteed to fit every phone, output device, or Bluetooth route.
