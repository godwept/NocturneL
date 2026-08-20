# Library Scan and Transient Notices Design

**Date:** 2026-08-20
**Status:** Approved

## Goal

After a user selects or replaces their music folder, NocturneL immediately scans it—no trip to Settings required. During the scan, the app makes progress understandable and cancellable; after ordinary actions, stale success text clears itself instead of occupying the screen indefinitely.

## Success Criteria

- [ ] Selecting or confirming a replacement folder starts exactly one scan automatically.
- [ ] Initial setup shows scan state in place of the empty library.
- [ ] A scan first reports file discovery, then shows real completed/total indexing progress with Cancel.
- [ ] Cancelling preserves the prior catalog; an initial scan returns to setup.
- [ ] Routine success messages clear after five seconds; errors and cancellation results stay until superseded.

## Scope

**In scope:**

- Automatically scan after initial folder selection and after a confirmed folder replacement.
- Show discovery and indexing progress, including a Cancel action.
- Retain the old library during replacement-folder scans until a new scan succeeds.
- Apply a shared five-second expiry to routine success notices, including scan and playlist transfer results.
- Keep errors and cancellation notices visible until a later message replaces them.
- Add tests for scan triggering, progress states, cancellation, and expiry behavior.

**Out of scope:**

- Background scans that continue after the app closes.
- System-notification scan progress.
- Automatic periodic rescans or filesystem watching.
- A full app-wide message queue or notification-center redesign.

## Design

### State and behavior

The scan state will explicitly represent four phases: idle, discovering files, indexing a known total, and finished. Progress will carry both completed and total counts only after discovery, so the UI never pretends to know a percentage before it does.

Selecting a folder first persists access and clears playback state when the folder changed, then immediately starts the scan. A completed scan atomically replaces the catalog. If cancelled or failed, the prior catalog remains; for a brand-new setup there is no catalog, so the setup screen remains available to retry.

A small shared transient-notice state will hold the latest message and its severity. Success notices schedule their own five-second clearing; replacing a message cancels the prior timer. Error and cancellation notices have no timer.

### Key interfaces and UI

The scanner will report structured progress rather than an unlabelled integer: a discovery phase followed by indexing progress with completed and total file counts. The existing scan cancellation hook will remain the single cancellation path.

The library view model will expose current scan phase and progress, a start method used by both manual Rescan and completed folder selection, a cancel method, and a transient notice publisher that applies the severity and expiry rules.

The initial setup screen will switch to a scan-status view immediately after a folder is chosen. On later scans, that same compact status view will appear above the current library; scanning disables duplicate scan actions and provides Cancel. Playlist and other routine success messages will use the shared transient-notice behavior rather than owning a permanent screen field.

### Error handling and edge cases

- If access to the selected folder cannot be retained or is lost, the scan does not start; the user sees a persistent actionable error.
- If a replacement scan fails or is cancelled, its previous catalog remains available. A successful replacement clears playback state as it does today.
- If an initial scan fails or is cancelled, the user remains in setup and can select the folder again.
- A second scan request while one is active is ignored; the visible control remains disabled.
- Progress counts files traversed, including non-audio files, so the display accurately reflects the work being done; the completion report still distinguishes added, changed, missing, skipped, and unsupported items.
- A new message replaces the prior notice and resets its five-second timer. Errors and cancellation messages do not auto-dismiss.

## Testing Strategy

- Unit-test progress transitions: discovery, known-total indexing, completion, failure, and cancellation.
- Unit-test that selecting either an initial or replacement folder starts one scan automatically.
- Unit-test that cancellation preserves a prior catalog and leaves initial setup retryable.
- UI-test the initial scan view, replacement-scan overlay, determinate progress, disabled duplicate scan action, and Cancel.
- Unit-test the notice lifecycle: successes clear after five seconds; a replacement restarts the timer; errors and cancellations persist.
- Run the existing Android test suite and manually test a large real library, including folder replacement and cancellation at both scan stages.

## Open Questions

None.
