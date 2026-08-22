# Portrait Orientation Lock Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Keep NocturneL in portrait orientation on phones where Android honors application orientation requests, preventing ordinary device rotation from switching the main activity to landscape.

## Success Criteria

- [ ] `MainActivity` requests portrait orientation in the application manifest.
- [ ] Rotating a supported phone does not switch NocturneL to landscape.
- [ ] Android 16 large-screen behavior remains unchanged, without a temporary compatibility opt-out.
- [ ] Existing application behavior is otherwise unaffected.

## Scope

**In scope:**

- Add the standard portrait orientation declaration to `MainActivity`.
- Add a manifest policy test for the declaration.

**Out of scope:**

- Forcing portrait orientation on Android 16 large screens.
- Adding a temporary restricted-resizability compatibility opt-out.
- Runtime orientation controls or a user-selectable orientation setting.
- Adaptive layout changes.
- Installation filtering based on portrait-screen hardware support.

## Design

Declare `android:screenOrientation="portrait"` on the existing `MainActivity` in `AndroidManifest.xml`. The activity lifecycle, Compose UI, application state, and public interfaces remain unchanged.

Android can ignore the requested orientation on large screens, managed devices, or manufacturer-specific configurations. This is expected platform behavior and does not require application error handling.

## Testing Strategy

Add a unit-level manifest policy assertion confirming that `MainActivity` declares `android:screenOrientation="portrait"`. Run the relevant manifest policy test and the complete unit test suite, then verify that only the design document, manifest, and corresponding test changed as part of implementation.

## Open Questions

None.
