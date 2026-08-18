# Favorites and Listening History Design

**Date:** 2026-08-18
**Status:** Approved

## Goal

Turn NocturneL's existing Library screen into a useful daily landing experience while remaining entirely offline. Users can quickly resume a meaningful paused session, revisit recent listening, manage favorite albums and tracks, and see lifetime listening counts without mixing personal activity with scanned catalog metadata.

## Success Criteria

- [ ] A saved queue with at least 10 seconds of progress appears as a compact Resume card and resumes at the saved track and position.
- [ ] Users can independently favorite or unfavorite albums and tracks from every natural browsing and playback context.
- [ ] A track records one play per queue occurrence after 50% or four minutes of actual playback, whichever comes first.
- [ ] The newest 200 qualified play events are retained locally while lifetime counts remain accurate.
- [ ] The Library landing previews Resume, Favorites, and distinct recently played tracks before the album collection.
- [ ] Full Favorites and History views expose the complete retained local data.
- [ ] Switching library sources clears source-bound listening data after confirmation; rescanning the same source preserves it.
- [ ] No listening or favorites data leaves the device.

## Scope

**In scope:**

- Independent favorite state for albums and tracks.
- Favorite toggles on album cards and details, track rows and menus, search results, Favorites, and Now Playing.
- A combined Favorites view with albums first and tracks second.
- Qualified-play detection based on actual playback progress.
- Per-track lifetime play counts and derived album totals.
- A chronological full History view containing the latest 200 play events.
- Deduplicated recently played tracks on the Library preview.
- A compact Resume card backed by the saved playback queue and position.
- Quiet restoration on a fresh app launch, with active-session service recovery allowed to continue playback.
- Confirmed clearing of history and counts in Settings.
- Confirmed clearing of all source-bound listening data when changing library folders.
- Empty, missing-file, and access-lost states consistent with the terminal UI.

**Out of scope:**

- Cloud sync, accounts, backup, or cross-device history.
- Favorite artists or playlists.
- Ratings, tags, notes, listening streaks, charts, or time-listened analytics.
- Multiple resumable sessions or per-track bookmarks.
- Editing individual history events.
- Importing or exporting favorites and listening data.
- Recommendations based on listening behavior.
- Changing the 200-event retention limit in Settings.

## Design

### Data Model and State

Room will keep listening data separate from the scanned catalog:

- `favorite_tracks`
  - `relativePath` primary key
  - `favoritedAtEpochMillis`
- `favorite_albums`
  - `albumId` primary key
  - `favoritedAtEpochMillis`
- `track_play_stats`
  - `relativePath` primary key
  - `playCount`
  - `lastPlayedAtEpochMillis`
- `play_history`
  - auto-generated event ID
  - unique playback qualification ID
  - `relativePath`
  - `playedAtEpochMillis`

The listening repository will expose reactive projections for favorite albums joined to the playable catalog, favorite tracks joined to the playable catalog, per-track counts, album totals calculated from their track counts, the newest 200 history events, and a deduplicated recent-track preview based on each track's newest event.

Catalog rows and listening rows will not use destructive foreign-key cascades. Ordinary rescans can mark files missing without erasing personal data. Missing items are omitted from actionable previews but can remain identifiable in full history until retention removes them. Changing the library source clears all four activity tables in the same transaction that replaces the source catalog.

The saved playback snapshot remains outside Room, but its format is extended to retain stable queue-occurrence IDs and qualification state. For each occurrence that has begun playback, it records accumulated real listening time and whether its play was already counted. This prevents seeking or process restoration from creating duplicate plays. Position jumps do not add listening time.

A Resume projection combines the saved queue snapshot with current playable catalog rows. It exists only when the current item remains available, the queue has not completed or been cleared, and actual progress has reached 10 seconds.

### Interfaces and UI Behavior

A dedicated listening-data repository owns favorites, history, counts, retention, and source-bound clearing. It supports:

- Toggling a track favorite.
- Toggling an album favorite.
- Recording a qualified play transactionally by appending the history event, incrementing lifetime stats, and pruning events beyond the newest 200.
- Clearing history and counts while preserving favorites.
- Clearing all listening data during a confirmed library-source change.

The playback service owns a progress tracker because it reliably observes background playback. The tracker accumulates elapsed time only while a queue occurrence is actively playing, ignores seeks and discontinuities, emits at most one qualified-play event for that occurrence, and persists its state with the playback snapshot.

A listening view model combines repository flows with current catalog data for the UI. Playback controls remain in `PlaybackConnection`; favorite actions do not become media-session commands.

The Library landing is ordered as:

1. Compact Resume card, when eligible.
2. Compact Favorites preview containing both albums and tracks, with `View all`.
3. Distinct Recently Played track preview, with `View all`.
4. Existing complete album library.

Favorites and History are nested Library views rather than new bottom-navigation destinations. Back returns to the same Library scroll position.

Interaction behavior:

- Resume continues the existing restored queue at its saved track and position.
- Selecting a favorite album opens album detail.
- Selecting a favorite or recent track starts playback from that track.
- History is newest-first and shows the timestamp plus the track's current lifetime play count.
- Album cards and details show their derived total; track rows show their lifetime count.
- Empty sections are omitted from the landing rather than filled with placeholder frames.
- Favorite toggles update optimistically and revert with a concise status message if local persistence fails.

### Error Handling and Edge Cases

- Qualified-play writes are idempotent. Each queue occurrence keeps a stable qualification ID under a uniqueness constraint. A transactional count increment occurs only when its history event is newly inserted, preventing retries or restoration from double-counting.
- Actual elapsed listening time accumulates only while `isPlaying` is true and the same occurrence remains active. Pauses, buffering, seeks, scrubbing, and stopped time do not contribute.
- The threshold is the earlier of 50% or four minutes. Runtime duration is preferred, with scanned duration as fallback. If duration is unavailable, the play qualifies at four minutes or on natural completion.
- A queue occurrence can qualify only once. Re-adding the same track creates a new occurrence and can produce another play; revisiting the same occurrence cannot.
- When playback reaches the end of the queue, the saved snapshot is marked completed and Resume disappears. Clearing or replacing the queue also invalidates the prior Resume session.
- If queued tracks disappear during a rescan, restoration removes unavailable entries. Resume keeps the saved position only when the same current track survives; otherwise it begins the first surviving entry at 0:00 and does not show Resume until that entry gains meaningful progress.
- Missing favorite items are hidden from playable views but their records survive same-source rescans. They return automatically if their identifiers reappear.
- Missing tracks can remain visible but disabled in full History using retained catalog metadata. They do not expose an actionable playback control.
- Lost folder permission disables Resume and playback actions without deleting listening data. Regranting access restores normal behavior.
- Canceling a library-switch confirmation changes nothing. Confirming clears favorites, history, counts, play-qualification state, and Resume atomically with the source change.
- Clearing history and counts is confirmed, preserves favorites and Resume, and resets any already-qualified occurrence so continuing that occurrence does not immediately recreate the cleared event.
- Local database failures leave playback running. The UI reports a concise failure, and idempotent recording permits a safe retry without count inflation.
- Fresh app launches explicitly request paused restoration. Service recovery belonging to the same active app or playback session may honor the prior playing state.

## Testing Strategy

Unit tests will cover:

- Favorite toggling and independent album/track state.
- Qualified-play thresholds for short, normal, long, and unknown-duration tracks.
- Accumulation across pause/resume and exclusion of seeks, buffering, and stopped time.
- One-play-per-occurrence behavior and idempotent retry after restoration.
- Transactional count increments and pruning to the newest 200 events.
- Deduplicated recent-preview ordering while full history preserves repeats.
- Album totals derived from track counts.
- Resume eligibility at the 10-second boundary.
- Resume invalidation after completion, queue replacement, explicit clearing, and source changes.
- Restoration when the current or other queued tracks are missing.
- Playback snapshot codec migration and malformed-snapshot handling.
- Clearing history and counts without clearing favorites or Resume.

Room integration tests will verify:

- All activity-table queries and reactive updates.
- Atomic history insertion, count increment, and retention pruning.
- Same-source rescans preserve listening data.
- Confirmed source changes clear all source-bound data.
- Missing catalog items are hidden or disabled according to the approved rules.
- Database migration from the current version preserves the existing catalog and playlists.

Compose UI tests will verify:

- Landing-section ordering and omission of empty sections.
- Resume action and displayed progress.
- Favorites preview and full two-section Favorites view.
- Recently Played preview deduplication and full History ordering.
- Favorite controls in album, track, search, history, and Now Playing contexts.
- Play-count rendering for tracks and derived album totals.
- Confirmation and failure states for clearing data and changing sources.
- Back navigation restores the Library landing and its scroll position.

Screenshot coverage will add terminal-themed fixtures for the populated landing screen, Favorites, History, and confirmation states. The full unit, instrumentation, and screenshot suites must pass with no unrelated file changes.

## Open Questions

None.
