---
layout: manual
title: Settings
description: Manage the library source, scans, terminal effects, privacy, and listening data.
section: Configuration
nav_order: 80
permalink: /manual/settings/
---

## Local library

- **CHANGE MUSIC FOLDER** opens Android's folder picker. When the chosen source differs, confirm or cancel the change. Confirmation clears favorites, history, counts, and resume state tied to the old source.
- **RESCAN LIBRARY** discovers and indexes current files. While it runs, the action reads **SCANNING...** and progress is displayed.
- **CANCEL** stops an active rescan and preserves the previous completed catalog.

## Appearance and motion

**CRT EFFECTS** controls scanlines, glow, cover-flow movement, marquee movement, and visualizer afterglow. Android's reduced-motion setting overrides optional animation at runtime; the saved CRT preference remains unchanged and the Settings screen explains that effects are paused.

**COLOR THEME** cycles through **GREEN TERMINAL**, **AMBER TERMINAL**, **BLUE TERMINAL**, **'80S SYNTHWAVE**, and **'90S NEON**. The choice recolors the complete interface and visualizers immediately, is saved automatically, works offline, and remains independent from the selected font. Album covers keep their original colors. Turning off **CRT EFFECTS** retains the theme's core colors while removing scanlines, visualizer bloom, and the '90s Neon border glow.

**FONT PRESET** cycles through **CLASSIC**, **MAINFRAME**, **PIXEL**, and **MODERN** display/body font pairings. Each choice changes the entire app immediately and is saved automatically. Every font is bundled with NocturneL for fully offline use.

The library's GRID/FLOW choice and sort mode persist from their controls on **LIB**. The visualizer sync offset also persists from controls on **NOW** even though those values are adjusted outside Settings.

## Privacy and listening data

**PRIVACY POLICY** opens the public NocturneL policy in the system browser.

**CLEAR HISTORY + COUNTS** requires **CONFIRM CLEAR**. It deletes qualified-play history and play counts while favorites and resume state remain preserved. Choose **CANCEL** to leave listening activity unchanged.
