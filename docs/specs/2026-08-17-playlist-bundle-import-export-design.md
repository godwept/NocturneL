# Playlist Bundle Import and Export Design

**Date:** 2026-08-17
**Status:** Approved

## Goal

Allow listeners to back up every NocturneL playlist to one portable ZIP file and later restore those playlists through the existing import flow. Each playlist remains available as a standard UTF-8 `.m3u8` file inside the ZIP, while a small versioned manifest preserves exact names and supports reliable round trips.

## Success Criteria

- [ ] `EXPORT ALL` writes every playlist, including empty playlists, to one ZIP.
- [ ] Each playlist is represented by an independently usable `.m3u8` entry.
- [ ] A versioned manifest preserves exact playlist names and maps them to ZIP entries.
- [ ] The existing import action accepts standalone `.m3u8` files and NocturneL ZIP bundles.
- [ ] Playlist names and track ordering round-trip through a bundle.
- [ ] Name conflicts keep both playlists by selecting the next `(2)`, `(3)`, and subsequent suffix.
- [ ] Valid playlists import when other bundle entries are malformed, with imported and skipped counts reported.
- [ ] Corrupt, oversized, or unsafe ZIP content cannot escape storage or crash the app.

## Scope

**In scope:**

- An `EXPORT ALL` action on the playlist index.
- One user-selected `.zip` destination.
- One UTF-8 `.m3u8` entry per playlist.
- A versioned JSON manifest containing exact playlist names and ZIP entry mappings.
- Automatic standalone playlist or ZIP detection through the existing import action.
- Conflict-safe renaming and best-effort bundle import.
- Empty playlist preservation.
- Clear success, partial-success, cancellation, and failure notices.

**Out of scope:**

- Album artwork, application settings, playback position, or library database backup.
- Selecting only some playlists for bulk export.
- Password-protected or encrypted ZIPs.
- Editing ZIP contents inside the app.
- Synchronization with cloud services.
- Importing arbitrary ZIP layouts without a recognizable NocturneL manifest.

## Design

### Bundle Format

The ZIP contains a fixed manifest and a directory of portable playlists:

```text
nocturnel-playlists.json
playlists/0001-road-trip.m3u8
playlists/0002-favorites.m3u8
```

Manifest version 1 contains only a NocturneL playlist-bundle identifier, the schema version, and ordered records mapping each exact playlist name to its `.m3u8` entry. Numeric prefixes guarantee unique ZIP paths when playlist names repeat. The readable filename portion is sanitized for portability and is not treated as the authoritative playlist name.

A ZIP without a valid NocturneL manifest is rejected as an unsupported backup rather than having its structure guessed. Individual `.m3u8` entries remain standard relative-path playlists that other music applications can consume independently.

### Data and Naming

No Room migration or persistent backup entity is introduced. Import and export use transient records containing a playlist name, a ZIP entry path, and ordered track paths.

Imported names are compared case-insensitively against existing playlists and playlists already imported from the same bundle. A conflict selects the next available suffix, such as `Favorites (2)` and then `Favorites (3)`. Exact names from the manifest are otherwise preserved.

ZIP bundle imports retain syntactically safe relative track paths even when a track is currently absent from the library. This preserves a complete backup for a later library rescan. Standalone `.m3u8` import retains its current behavior of reporting unknown tracks as skipped.

### User Interface and Flow

The playlist index changes the current `IMPORT M3U8` action to `IMPORT`. It accepts `.m3u8`, `.m3u`, and `.zip` documents and detects the selected format.

An `EXPORT ALL` action appears alongside `IMPORT`. It opens Android's create-document picker with a default filename of `NocturneL Playlists.zip`. Existing per-playlist `EXPORT` actions remain unchanged.

The bundle codec provides pure encode and decode operations. Encoding converts named, ordered playlists into a manifest and `.m3u8` entries. Decoding returns valid playlist records plus structured skipped-entry details.

Android document handling remains separate from the codec. The document service streams the ZIP to or from the selected content URI. The view model gathers playlist names and paths, coordinates import and export, resolves naming conflicts, and publishes concise result notices. The repository retains responsibility only for creating playlists and replacing ordered entries.

### Error Handling and Safety

- ZIP entries are streamed and are never extracted to filesystem paths.
- Only manifest-declared files beneath `playlists/` are considered.
- Duplicate, missing, directory, encrypted, and unsupported entries are skipped and reported.
- Imports accept at most 1,000 playlist entries, 4 MB per `.m3u8`, and 64 MB of total uncompressed content.
- A malformed playlist does not create a partial database record; other valid playlists continue importing.
- Empty `.m3u8` entries create empty playlists.
- Invalid absolute paths, parent traversal, and malformed path records are skipped.
- Export cancellation changes no application data.
- Export failure reports `Playlist export failed`.
- Successful export reports the number of playlists written.
- Import reports created playlists, imported tracks, skipped playlists, and skipped track entries without listing every individual error in the primary UI.

## Testing Strategy

- Bundle codec unit tests verify manifest versioning, exact and duplicate names, empty playlists, deterministic unique ZIP paths, UTF-8 content, and track ordering.
- Round-trip tests encode several playlists into one ZIP and decode them without data loss.
- Import tests cover `(2)` and `(3)` naming, case-insensitive conflicts, unknown-track preservation for bundles, and unchanged standalone `.m3u8` behavior.
- Corruption tests cover missing or malformed manifests, missing declared entries, malformed individual playlists, traversal paths, excessive entry counts, oversized content, and mixed valid and invalid playlists.
- View-model tests verify success, partial-success, cancellation, and failure notices.
- Compose tests verify `IMPORT` and `EXPORT ALL`, accepted document types, the default ZIP filename, and retention of individual `EXPORT` actions.
- Android document-service tests verify ZIP reading and writing through content URIs.
- A device check exports multiple playlists, deletes them, imports the ZIP, and confirms names, empty playlists, track order, and conflict renaming.

## Open Questions

None.
