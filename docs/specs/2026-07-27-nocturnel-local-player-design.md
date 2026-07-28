# NocturneL Local Player Design

**Date:** 2026-07-27
**Status:** Approved

## Goal

Replace the Spotify remote with **NocturneL**, a fully offline native Android local-music player. The user grants it access to one chosen music folder and its subfolders; it keeps the terminal aesthetic while making albums, cover art, playlists, and playback polished and deliberate.

## Success Criteria

- [ ] The user chooses a local music folder once; NocturneL reads its recursive contents without Spotify, network access, or the Spotify app.
- [ ] Album-first browsing shows large local cover art using embedded art, `cover.jpg`/`folder.jpg`, a manual override, then a styled fallback.
- [ ] Explicit rescans discover library changes and clearly report unsupported or missing files.
- [ ] Standard playback works reliably in the foreground and through Android notification/lock-screen controls, including queue, seek, previous/next, shuffle, repeat, and metadata-backed gapless transitions only.
- [ ] Local playlists can be created, played, imported from `.m3u8`, and exported to `.m3u8`.
- [ ] The app remains usable offline and preserves the terminal visual identity.

## Scope

**In scope:**

- Native Android app, optimized for the user's phone and built around a folder selected through Android's system picker.
- Offline recursive library scan for common local formats where Android can decode them; embedded tags plus filename/folder fallbacks.
- Album, artist, track, search, queue, now-playing, and playlist views.
- Local cover-art fallback chain: embedded artwork, `cover.jpg` / `folder.jpg`, manually assigned image, then a terminal-themed generated placeholder.
- Explicit rescan, `.m3u8` import/export, playback persistence, Android media notification, lock-screen/headset controls, shuffle, repeat, and supported gapless transitions.
- A refined terminal UI with much larger album art, high contrast, restrained CRT effects, and accessibility/reduced-motion options.

**Out of scope:**

- Spotify integration, accounts, streaming, cloud sync, social features, lyrics, online metadata/art lookup, equalizer, crossfade, or automatic silence trimming.
- iOS, desktop, and Android Auto support.
- Background filesystem monitoring; rescans remain user-triggered.

## Design

### Data model and local state

NocturneL stores a local catalog and settings only; it never copies audio files.

- **Library source:** selected Android folder URI, display name, last successful rescan time, and access status.
- **Album:** stable folder-based ID; title/artist/year from tags when consistently available, otherwise derived from folder names; resolved cover-art source and optional manual cover override.
- **Track:** document URI plus stable relative path, filename, format, duration, track/disc number, title/artist/album metadata, and a scan status: `playable`, `unsupported`, `missing`, or `metadata issue`.
- **Artist:** derived grouping, not a separately managed source of truth.
- **Playlist:** local name, ordered track references, creation/update time, and an optional source `.m3u8` reference. Missing tracks stay visible and are skipped during playback rather than silently removed.
- **Queue/playback:** current item, ordered queue, position, shuffle/repeat mode, and last playback position; sufficient to restore a session after reopening.
- **Scan report:** timestamp plus counts and details for new, changed, missing, skipped, and unsupported files.
- **Preferences:** terminal effects/reduced motion, theme intensity, and other small UI choices.

### Key interfaces and app boundaries

- **Library access:** chooses/revalidates the root folder and opens files only within that granted tree.
- **Library scanner:** recursively enumerates files on explicit rescan, extracts supported metadata, derives filename/folder fallbacks, and produces a scan report.
- **Artwork resolver:** selects embedded artwork, then album-folder cover files, then a saved override, then the generated terminal placeholder.
- **Catalog repository:** persists the local album/track/playlist catalog and scan results, and exposes it to the UI.
- **Playlist codec:** imports and exports portable `.m3u8` files, resolving relative paths against the selected music root.
- **Playback service:** owns the queue and audio lifecycle, presents Android's media notification/lock-screen controls, and restores state safely.
- **Gapless policy:** enables a seamless transition only when format/metadata indicates it is safe; otherwise plays each track unchanged.
- **Compose UI:** terminal-styled screens consume catalog and playback state without directly touching files or audio APIs.

### Error handling and edge cases

- **Folder access lost/revoked:** playback and scanning stop safely; NocturneL explains the issue and offers a folder reselection action.
- **Missing or moved files:** marked as missing after a rescan; playlist entries remain visible but are skipped with a brief notice.
- **Unsupported/corrupt audio:** kept in the scan report with the reason where available; never shown as playable.
- **Missing/inconsistent metadata:** use filename and folder-name fallbacks; albums without cover art receive the terminal placeholder.
- **Unreadable cover image:** fall through to the next artwork source without blocking album browsing.
- **Malformed or external `.m3u8` entries:** import valid relative paths; report skipped entries rather than failing the entire playlist.
- **Audio interruptions:** pause/duck appropriately for calls, other media, headphones/Bluetooth changes, and regain focus according to Android's media behavior.
- **Long scans/large libraries:** run away from the UI thread, show progress, and preserve the previous catalog if a scan is canceled or fails.
- **Gapless uncertainty:** never trim audio based on a silence guess; use normal transitions unless a supported metadata-backed path is confirmed.

## Testing Strategy

- Unit-test filename/folder metadata fallbacks, artwork precedence, playlist parsing/export, scan-diff rules, queue behavior, shuffle/repeat, and gapless eligibility.
- Instrument Android storage access with fake/document-provider test fixtures: nested folders, mixed formats, missing files, bad tags, missing covers, and revoked permissions.
- Test the playback service's state transitions and media-session commands without relying on live hardware audio.
- Compose UI tests for album-first navigation, search, playlist creation, rescan progress/results, error states, and reduced-motion rendering.
- Manual testing on the user's Android phone for selected-folder persistence, notification/lock-screen/headset controls, Bluetooth behavior, real-library scan performance, and intended gapless album transitions.
- Release verification: clean install, offline operation, permission recovery, `.m3u8` round-trip, and a signed debug/release build.

## Open Questions

- Exact Android minimum SDK and target SDK will be selected during implementation based on current Android toolchain guidance.
- Full format support is limited to codecs available on the target device; unsupported formats will be reported instead of treated as playable.
