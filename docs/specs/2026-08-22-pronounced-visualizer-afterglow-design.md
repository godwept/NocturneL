# Pronounced Visualizer Afterglow Design

**Date:** 2026-08-22
**Status:** Approved

## Goal

Strengthen both visualizer afterglows from 250 ms to 500 ms while preserving live-motion clarity. The radar will gain a pronounced but readable phosphor treatment across its complete geometry, while spectrum bands will retain brighter, more obvious ghost segments.

## Success Criteria

- [ ] Radar bloom is clearly visible at normal phone brightness.
- [ ] Radar geometry remains readable rather than becoming a green haze.
- [ ] Sweep history persists smoothly for approximately 500 ms.
- [ ] Falling band ghosts are obvious during ordinary music playback.
- [ ] Live bars and the live sweep remain brighter than retained history.
- [ ] Effects clear immediately when disabled, paused, unavailable, resized, or switched.

## Scope

**In scope:**

- Increase radar and band afterglow duration from 250 ms to 500 ms.
- Increase retained-history brightness for both visualizer modes.
- Add layered canvas bloom to all radar geometry.
- Use broader, softer bloom layers beneath crisp radar cores.
- Strengthen radar sweep and trail hierarchy so motion remains dominant.
- Make band ghosts more visible through color, opacity, and retained-segment presentation.
- Update focused state, geometry, UI, and screenshot tests.

**Out of scope:**

- Bloom on spectrum bars; their improvement remains a crisp phosphor persistence effect.
- User-adjustable glow strength or duration controls.
- GPU shaders, platform blur effects, or bitmap render layers.
- Changes to audio analysis, FFT data, synchronization, navigation, or layout.
- Changes to scanline behavior or the overall color palette.
- Persistence when playback is paused or the signal is unavailable.

## Design

### State and Decay Model

The existing presentation-only afterglow state remains the foundation. Nothing is persisted, and the audio-frame model remains unchanged.

- A shared 500 ms decay window replaces the current 250 ms duration.
- Radar history remains a bounded sequence of sweep-angle samples. Its capacity increases enough to represent the longer trail without unbounded allocation.
- Each radar sample fades by elapsed monotonic time, independent of display refresh rate.
- Band envelopes retain their recent higher level for the same 500 ms window, descending toward the live value while fading.
- Band ghost opacity starts visibly above the current dim appearance, then decays smoothly rather than disappearing abruptly.
- Bloom requires no additional history state. It is derived from the current radar geometry and retained sweep samples during drawing.
- Existing lifecycle resets remain unchanged: mode changes, size changes, disabled effects, rewound frames, paused playback, and inactive or unavailable analysis clear retained history immediately.

### Rendering Interfaces and Visual Hierarchy

The public composable interface remains unchanged. The enhancement stays inside the visualizer rendering layer.

Radar rendering uses two ordered passes:

1. **Bloom pass:** Draw every radar element with wider, translucent phosphor strokes. Static grid elements receive restrained bloom, energy rings and transient echoes receive stronger bloom, and the sweep and retained trail receive the strongest bloom.
2. **Core pass:** Redraw the existing geometry with narrow, sharper strokes. This preserves legibility and prevents the canvas from looking uniformly blurred.

Small internal drawing helpers keep circles, spokes, sweep lines, and retained trails consistent between the passes. Bloom strengths and stroke expansion are grouped as internal visual constants rather than exposed as settings.

Band rendering remains crisp:

- Ghost segments use a more luminous phosphor treatment instead of disappearing into the dim theme token.
- Their 500 ms opacity curve keeps fresh ghosts obvious and fades them below live bars over time.
- Live segments and peak markers render last, guaranteeing that current audio data remains dominant.
- No bloom layer is added to bars.

### Error Handling and Edge Cases

- The first active frame shows bloom around current radar geometry but no invented motion trail.
- Radar history crossing 360 degrees continues along the sweep direction without flashing across the circle.
- Large frame-time gaps expire old samples immediately instead of creating a frozen bright trail.
- Bloom strokes are clipped to the visualizer canvas so they cannot bleed into playback controls or borders.
- Elements near canvas edges may have their outermost glow clipped, but their crisp core remains fully visible.
- A band ghost is drawn only where the retained bar exceeds the live bar; it never covers or recolors live segments.
- Silence and zero-height bands can briefly retain valid falling ghosts while analysis remains active.
- Changing band count safely rebuilds the envelope collection.
- Disabling effects, changing modes, resizing, pausing, losing the signal, or receiving an unavailable frame clears all historical glow immediately.
- The effect remains decorative and adds no controls, semantics, or accessibility announcements.

## Testing Strategy

Pure unit tests will verify:

- Radar and band history remains active before 500 ms and expires at 500 ms.
- Both opacity curves fade monotonically.
- Fresh afterglow is visibly stronger than the previous thresholds but remains below live-element brightness.
- Radar history remains bounded despite the increased capacity.
- Band envelopes never fall below current live levels.
- Reset, resize, mode-change, inactive-signal, frame-rewind, and large-gap behavior remains correct.

Geometry and source-contract tests will verify:

- Bloom stroke widths and opacity tiers preserve the intended hierarchy.
- Static radar bloom is weaker than motion bloom.
- Core radar elements are drawn after their bloom counterparts.
- Band ghosts remain behind live segments and cover only retained-height differences.
- The implementation continues using Compose's frame clock without independent timers.

Screenshot previews will capture representative radar and band afterglow states at the stronger settings. The radar reference must show readable crisp geometry over a pronounced halo; the bands reference must show unmistakable falling ghosts without overpowering live bars.

A final device check at ordinary display brightness will confirm the perceptual requirement that automated alpha tests cannot fully measure.

## Open Questions

None.
