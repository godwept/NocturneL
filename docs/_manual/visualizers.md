---
layout: manual
title: Visualizers and sync
description: Cycle visual modes, control terminal effects, and align visuals with audio.
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

## CRT effects and motion

Visualizer afterglow, scanline treatment, cover movement, and related terminal effects follow **CRT EFFECTS** in Settings. If Android's reduced-motion preference is enabled, NocturneL pauses optional motion even when CRT EFFECTS remains saved as on.

## Adjust visualizer sync

Radar, Spectrum, and Frequency Grid modes show sync controls in the upper corners:

- Select **−** to decrease the offset by **25 ms**.
- Select **+** to increase the offset by **25 ms**.
- Hold **−** or **+** to repeat the **25 ms** adjustment and accelerate during a continued hold.
- Select the **VIS SYNC** label to reset to `0 ms`.

The supported range is **-2000 ms** through **+2000 ms**. Adjust by observation until visual changes align with what you hear. Device audio pipelines differ, so one offset is not guaranteed to fit every phone, output device, or Bluetooth route.
