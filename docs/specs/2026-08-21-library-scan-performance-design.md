# Library Scan Performance Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Make library indexing faster while preserving scan correctness, cancellation, access-loss handling, and atomic catalog updates. Unchanged rescans should avoid reopening audio files when the storage provider supplies a reliable fingerprint, while first scans and changed albums should avoid redundant embedded-artwork reads. More invasive traversal and concurrency changes will be adopted only when device measurements justify them.

## Success Criteria

- [ ] Unchanged files with reliable size and modified-time data reuse their stored metadata.
- [ ] New, changed, moved, fingerprint-unknown, and prior metadata-error files are read normally.
- [ ] Providers reporting an unknown or unreliable modified time remain on the conservative full-read path.
- [ ] Embedded artwork is requested only until the first usable image is found for each album.
- [ ] Existing scan correctness, progress, cancellation, access-loss, and atomic persistence behavior is preserved.
- [ ] Direct traversal and bounded metadata concurrency are measured independently against the current implementation.
- [ ] A measured optimization is retained only when it produces a repeatable improvement without changing scan results.
- [ ] Automated tests cover incremental decisions, artwork selection, migration, cancellation, and result equivalence.

## Scope

**In scope:**

- Persist file size and last-modified time alongside each track.
- Reuse catalog metadata for unchanged files during rescans.
- Conservatively reread files whose provider returns an unknown modified time.
- Preserve detection of additions, changes, moves, deletions, and metadata failures.
- Stop embedded-artwork extraction after finding the first usable image in each album.
- Add repeatable profiling for existing `DocumentFile` traversal versus direct `DocumentsContract` traversal.
- Profile sequential metadata extraction versus small bounded concurrency levels.
- Adopt direct traversal or concurrency only if measurements and compatibility checks justify it.
- Add the required Room migration and automated tests.

**Out of scope:**

- Background filesystem monitoring or automatic rescans.
- MediaStore scanning outside the user-selected folder.
- Hashing complete audio-file contents.
- UI redesign or new scan controls.
- Unbounded parallelism.
- Sacrificing metadata freshness for providers with unreliable fingerprints.
- A guaranteed fixed scan-duration target.

## Design

### Data Model and Scan State

Each stored track gains two nullable fingerprint fields:

- `fileSizeBytes`
- `lastModifiedEpochMillis`

A stored track is reusable only when both stored values are present, the provider currently returns valid values, and relative path, document URI, size, and modified time all match. Version-2 rows migrate with null fingerprints, causing one conservative metadata refresh before later scans can reuse them.

During a scan, each discovered audio document is classified as follows:

- **Unchanged:** reuse the existing title, artist, album, duration, track/disc numbers, and status; update only its last-seen scan time.
- **New or changed:** run normal metadata extraction and store the new fingerprint.
- **Unknown fingerprint:** run normal metadata extraction every time.
- **Missing or moved:** preserve current reconciliation behavior; a move is one missing path and one new path.

Album metadata and artwork are reused when all members remain unchanged. When an album's membership or file fingerprints change, its album record is rebuilt. Artwork probing inspects tracks in stable scan order until the first usable embedded image is found, then stops. Folder and manual artwork precedence remains unchanged.

The scanner's public result and progress states remain compatible with the current UI.

### Interfaces and API Surface

The scanner receives an `ExistingCatalogSnapshot` containing tracks and albums indexed by stable IDs. `CatalogRepository` supplies the current snapshot for a normal rescan and an empty snapshot for a newly selected source.

Document discovery is isolated behind a small enumerator interface. Each discovered result contains:

- Relative path
- Document URI
- File name/type
- Size, when available
- Last-modified time, when available

The current `DocumentFile` implementation remains the production baseline. A direct `DocumentsContract` implementation can be profiled against the same contract without changing scan reconciliation.

Metadata access is also isolated behind an interface so tests can count tag and artwork reads. Tag extraction and artwork probing are independently callable, allowing unchanged tracks to avoid both and allowing artwork lookup to stop after the first successful image.

Profiling records discovery, tag extraction, artwork extraction, and database persistence separately. Alternative enumeration and bounded-concurrency strategies use identical input and must produce equivalent scan results. Profiling-only implementations do not enter the production path unless measurements justify them.

If bounded concurrency is adopted, the scanner becomes coroutine-aware and uses a small fixed limit, structured cancellation, and deterministic result ordering. No executor or coroutine may outlive the scan.

### Error Handling and Edge Cases

- Missing, zero, negative, or otherwise unusable modified times make a fingerprint unreliable; those files are reread on every scan.
- Tracks currently marked `METADATA_ISSUE` are never reused from cache, even when their fingerprint matches, so transient read failures can recover on a later scan.
- A failed metadata read affects only that track and continues producing the existing actionable scan issue.
- A failed artwork probe moves to the next track in that album. Exhausting all candidates produces an album without embedded artwork and retains folder/manual artwork behavior.
- Every discovered document, including reused tracks, advances indexing progress exactly once.
- Cancellation stops discovery, metadata workers, and artwork probing promptly. No partial scan reaches Room; the previous catalog remains intact.
- Access revocation and provider failures retain the existing access-lost behavior.
- Direct traversal, if adopted, must fall back safely or remain disabled for providers that do not support the required document columns or child queries.
- Bounded metadata tasks preserve discovery order in the final catalog, regardless of completion order.
- The migration adds nullable columns without rewriting existing metadata. Existing installations perform one full metadata refresh after upgrading.
- File changes that retain identical URI, size, and modified time cannot be detected reliably without content hashing. Hashing is intentionally excluded because it would undermine the performance goal.

## Testing Strategy

Automated coverage verifies:

- Matching reliable fingerprints reuse metadata and artwork.
- Changed, new, moved, unknown-fingerprint, and metadata-error tracks are reread.
- Existing version-2 rows receive null fingerprints through the Room migration.
- Artwork probing stops immediately after the first usable image in an album.
- Albums with missing or unreadable artwork continue through candidates correctly.
- Cached and freshly read tracks produce the same ordered scan result.
- Cancellation prevents all database changes, including with concurrent workers.
- Existing scan reconciliation, source replacement, and access-loss tests remain green.

Reader and enumerator fakes expose call counts, making "unchanged means no metadata read" and "one successful artwork read per album" directly testable.

Device profiling uses the same library and device for each candidate, with warm-up runs followed by several measured runs. It compares:

- `DocumentFile` traversal against direct `DocumentsContract` traversal.
- Sequential metadata reads against concurrency limits of 2 and 4.
- Result counts and metadata equivalence, not timing alone.

A candidate is adopted only if its median time improves repeatably, its results match the baseline, cancellation still works, and it produces no provider or resource errors. Final manual checks cover an initial scan, unchanged rescan, changed tags, added/deleted files, cancellation, and revoked folder access.

## Open Questions

- Direct `DocumentsContract` traversal remains conditional on device profiling and provider compatibility results.
- The production metadata concurrency limit remains conditional on profiling sequential, two-worker, and four-worker configurations.
