# Portrait Full-Screen Visualizers Redesign

**Date:** 2026-09-23  
**Status:** Approved

## Goal

Redesign the full-screen Radar and Frequency Grid visualizers so they use portrait screen space intentionally while preserving their existing NocturneL identity, audio behavior, terminal/CRT styling, and normal NOW-screen presentation.

Radar remains a centered circular instrument, but its sweep and transient effects extend into the full viewport. Frequency Grid becomes a portrait-native field of square cells rather than a centered 30×30 square.

## Success Criteria

- [ ] Full-screen Grid fills substantially more of a portrait display with square cells instead of remaining constrained to a centered 30×30 square.
- [ ] Full-screen Grid keeps roughly the existing horizontal density while deriving its row count from the available portrait aspect ratio.
- [ ] Full-screen Radar keeps a recognizable centered circular core with its current rings, spokes, energy rings, sweep arm, trail, transient echo, and CRT treatment.
- [ ] Radar's full-screen sweep continues beyond the circular core as a faint beam with a broader phosphor wake.
- [ ] Radar beat/transient glow starts at the radar center and travels outward through the radar toward the physical screen edges and corners while fading.
- [ ] The radar core is visually transparent/open enough for the outward pulse to remain visible through and around it.
- [ ] Spectrum remains visually and behaviorally unchanged.
- [ ] NOW-screen Radar and Grid remain unchanged.
- [ ] Existing full-screen swipe, Exit, Back, immersive-system-bar, sync, CRT, reduced-motion, and audio-analysis behavior remains intact.
- [ ] Automated geometry/rendering tests pass and the redesigned visualizers are verified on a physical Pixel device.

## Scope

### In scope

#### Full-screen Grid

- Replace the square 30×30 full-screen presentation with a portrait grid that fills substantially more of the usable display.
- Preserve square cells; never stretch cells vertically to force a fill.
- Keep roughly the existing horizontal density, targeting approximately the current 30-column feel, while deriving the number of rows from the actual usable aspect ratio.
- Preserve the current frequency-hotspot concept, live intensity, peak treatment, afterglow, theme colors, and scanlines.
- Add a portrait-specific deterministic hotspot distribution so activity is spread throughout the taller field instead of being confined to an implied square.
- Allow active regions to approach the top and bottom screen boundaries naturally so the grid feels like it continues beyond the phone.

#### Full-screen Radar

- Keep a clearly recognizable centered circular radar core.
- Preserve the existing rings, audio-reactive energy rings, spokes, sweep arm, sweep trail, transient echo, and current afterglow behavior.
- Remove the visual impression of an opaque square scene so full-screen effects can remain visible behind and through the radar.
- Replace the current top/bottom margin glow with a radial beat pulse that originates at the exact radar center.
- Let that pulse expand through the radar, cross the circular boundary, and continue toward the screen edges and corners while fading.
- Extend the sweep into the full viewport using a hybrid treatment:
  - a very faint beam continues from the radar center to the actual screen boundary;
  - a broader, softer phosphor wake trails behind it;
  - both remain dimmer than the radar core.
- Stronger transients may create a brighter/farther pulse, but the effect must remain restrained and never become a full-screen flash.

#### Shared behavior

- Apply these adaptations only in full-screen mode.
- Keep the existing square NOW-screen Radar and Grid unchanged.
- Keep Spectrum unchanged because it already uses the full portrait viewport effectively.
- Preserve existing full-screen mode switching, Exit behavior, Android Back handling, immersive system bars, sync offset, CRT setting, reduced-motion handling, theme behavior, and audio-analysis pipeline.
- Treat viewport changes, system inset changes, and configuration changes as geometry recalculation events so no stale visual geometry survives a resize.

### Out of scope

- Changes to the audio-analysis algorithm.
- New FFT/frequency data.
- New visualization modes.
- User-adjustable visual-effect controls.
- Changes to normal NOW-screen visualizer dimensions or behavior.
- A rectangular Radar core.
- Stretching Grid cells to fill the display.
- Major Spectrum changes.
- New playback controls or navigation in full-screen mode.
- Randomized hotspot regeneration.
- Scrolling, panning, or camera movement inside Grid.

## Design

### Full-Screen Radar

The circular radar remains the primary visual instrument and stays centered at roughly the current perceived scale. The surrounding screen becomes part of the visualization rather than empty margin space.

The render layers are:

1. Terminal background.
2. Expanding beat/transient pulse.
3. Full-screen sweep wake.
4. Faint extended sweep beam.
5. Radar core rings, spokes, energy response, transient echo, and afterglow.
6. Bright live sweep arm.
7. CRT scanlines/effects.

Inside the circular core, the Radar remains crisp and visually dominant.

Outside the core:

- The current sweep angle continues toward the actual viewport boundary as a very faint beam.
- A broader, softer phosphor wake follows behind the beam.
- The beam and wake use the same sweep angle as the radar core so they read as one continuous instrument.
- The outer beam and wake must always remain weaker than the core sweep.

The beat/transient pulse:

- originates at the exact radar center;
- expands radially outward over time;
- remains visible through the radar core instead of being blocked by a square background;
- crosses the radar boundary and continues toward the display edges and corners;
- uses a brighter/thinner leading edge with a softer fading body behind it;
- fades continuously as it travels outward;
- may overlap briefly with newer pulses;
- uses a bounded number of active pulses so repeated transients cannot accumulate without limit.

The effect should make the entire screen feel like part of the radar field without enlarging the radar core itself into a giant circle.

### Full-Screen Frequency Grid

The full-screen Grid becomes portrait-native.

The horizontal density remains close to the current 30-column design. The renderer calculates a row count from the actual available viewport while keeping every cell square. On a typical tall phone this may produce roughly 30 columns by 55–65 rows, but the exact row count is geometry-driven rather than fixed.

The portrait Grid preserves the existing rendering layers:

- dim base cells;
- retained/ghost intensity;
- live intensity;
- bright peak cells;
- CRT afterglow;
- scanlines.

The full-screen hotspot layout is distinct from the existing square layout:

- Existing audio bands and intensity calculations remain unchanged.
- Hotspots use deterministic normalized positions distributed across the full portrait field.
- Neighboring hotspot influence may overlap so the display still forms flowing active regions instead of isolated dots.
- Low, mid, and high frequencies are not rigidly separated into horizontal zones.
- The layout should preserve the current scattered-organic character.
- Resizing changes physical placement but not the underlying frequency-to-hotspot relationships.

The result should feel like a larger terminal pixel field has been revealed above and below the existing square rather than the old 30×30 design being stretched.

### State and Interfaces

This redesign remains a presentation-layer change.

It continues to use the existing:

- `AudioAnalysisFrame`;
- selected visualizer mode;
- sync offset;
- CRT/effective-effects flag;
- reduced-motion behavior;
- afterglow state.

No new persisted settings or audio-analysis state are introduced.

Radar derives full-screen-specific effects from existing data:

- extended beam/wake from the existing sweep angle;
- outward pulse from the existing transient value.

Grid derives full-screen-specific geometry from the current viewport:

- existing NOW Grid keeps its current 30×30 geometry;
- full-screen Grid calculates portrait rows while keeping square cells and roughly the existing horizontal density.

The existing `TerminalVisualizerScene` concept should remain the shared rendering path rather than creating an entirely separate duplicated visualizer system. Full-screen presentation may supply the rendering context needed for Radar and Grid to choose their expanded geometry/effects.

A viewport or configuration resize must cause geometry to be recalculated cleanly. No stale pulse, hotspot, or cell geometry may be reused at incompatible dimensions.

### Visual Limits and Edge Cases

#### Radar

- The circular core remains the brightest and most detailed part of the visualization.
- The extended beam is weaker than the core sweep.
- The phosphor wake is weaker than the extended beam.
- A strong transient must never produce a full-screen white/bright flash.
- Outer pulse intensity fades as distance from the radar center increases.
- A small bounded number of transient pulses may coexist; expired pulses are removed.
- Leaving Radar or losing eligibility for the optional effect clears presentation-only pulse history.

#### Grid

- Cells remain square at every supported viewport.
- Whole rows are used.
- Small leftover space caused by aspect ratio or insets is distributed symmetrically rather than absorbed by stretching cells.
- Unusually narrow or short viewports degrade safely by reducing usable geometry rather than producing overlapping or zero-size cells.
- Hotspot placement remains deterministic across redraws and resize events.

#### Shared

- `UNAVAILABLE` continues to show the existing `SIGNAL UNAVAILABLE` treatment cleanly.
- Optional Radar glow/wake/pulse effects follow the existing effective CRT/reduced-motion behavior.
- Idle or unavailable analysis cannot leave stale pulses or Grid afterglow visible.
- Switching Radar → Spectrum → Grid clears Radar-only presentation state.
- Returning to NOW leaves its existing square visualizers unchanged.
- Exit and swipe gesture handling remains independent of the visualization effects beneath it.
- The outermost portions of the viewport should generally be the dimmest, preserving the terminal-instrument aesthetic rather than becoming a generic music visualizer.

## Testing Strategy

### Pure geometry and behavior tests

#### Grid

Verify that:

- a portrait viewport keeps roughly the existing horizontal density;
- row count increases appropriately for taller viewports;
- every cell remains square;
- leftover edge space is distributed symmetrically;
- frequency hotspots are spread through the full portrait field;
- hotspot mapping is deterministic;
- resizing recalculates geometry without changing the underlying frequency-to-hotspot relationships;
- invalid, zero, or very small dimensions fail safely.

#### Radar

Verify that:

- the extended beam uses the same sweep angle as the radar core;
- the beam reaches the actual viewport boundary for representative sweep angles;
- wake intensity is always lower than the extended beam and core sweep;
- beat pulses originate at the radar center;
- pulse radius expands monotonically over time;
- pulse intensity fades as it travels;
- pulses cross the radar boundary and can reach/fade toward viewport edges;
- expired pulses are removed;
- repeated close transients remain bounded;
- changing mode, disabling effects, or losing active analysis clears Radar-only pulse state.

### Compose and full-screen rendering tests

Verify that:

- Radar no longer reads as an opaque centered square in full screen;
- the circular radar core remains centered and proportionally correct;
- faint beam/wake rendering appears outside the radar circle;
- outside effects remain weaker than the core;
- a transient creates an outward-moving pulse visible through the radar and beyond its boundary;
- Grid fills a tall viewport with portrait geometry rather than reverting to a centered square;
- Grid cells remain square at representative Pixel-sized dimensions;
- Spectrum rendering and behavior remain unchanged;
- reduced-motion/effective-effects disabling suppresses optional Radar pulse/wake behavior;
- idle and unavailable states do not retain stale effects;
- switching modes clears presentation-only state;
- existing swipe, Exit, Back, immersive-system-bar, analysis-handoff, and mode-retention tests continue to pass.

### Physical-device verification

Verify on a Pixel device:

- Radar during quiet passages;
- Radar during steady rhythmic material;
- Radar during strong transients;
- extended sweep beam/wake visibility without overpowering the core;
- pulse propagation from center through the radar to screen edges;
- Grid with sparse audio activity;
- Grid with dense audio activity;
- square-cell integrity across the full portrait field;
- visual comfort over sustained playback;
- immersive-system-bar and gesture-navigation behavior;
- clean return to the unchanged NOW visualizers.

## Open Questions

None.
