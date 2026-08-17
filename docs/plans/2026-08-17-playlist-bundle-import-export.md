# Playlist Bundle Import and Export Implementation Plan

**Date:** 2026-08-17  
**Design doc:** `docs/specs/2026-08-17-playlist-bundle-import-export-design.md`  
**Status:** Ready for review

## Overview

Add a versioned NocturneL ZIP bundle containing one portable `.m3u8` per playlist and a JSON manifest that preserves exact names. Extend the existing playlist import action to detect standalone playlists or bundles, import valid bundle members independently, preserve missing relative paths, resolve name conflicts without overwriting, and expose `EXPORT ALL` through the existing Android document-picker flow. Keep ZIP/JSON logic pure and JVM-tested, keep content-URI I/O in a document service, and make each imported playlist atomic without changing the Room schema.

## Tasks

### Task 1: Define bundle contracts and JSON dependency (2–5 min)

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleModelsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleModels.kt`

**Test first:**

Create `PlaylistBundleModelsTest` and assert the public contract shapes before defining them:

```kotlin
@Test fun bundleContractHasStableIdentityAndLimits() {
    assertEquals("nocturnel-playlists", PLAYLIST_BUNDLE_FORMAT)
    assertEquals(1, PLAYLIST_BUNDLE_VERSION)
    assertEquals("nocturnel-playlists.json", PLAYLIST_BUNDLE_MANIFEST)
    assertEquals(1_000, MAX_BUNDLE_PLAYLISTS)
    assertEquals(4 * 1024 * 1024, MAX_BUNDLE_PLAYLIST_BYTES)
    assertEquals(64L * 1024 * 1024, MAX_BUNDLE_UNCOMPRESSED_BYTES)
}

@Test fun decodeResultRetainsValidAndSkippedCounts() {
    val playlist = PlaylistBundlePlaylist("Road Trip", listOf("Artist/Album/01.flac"))
    val result = PlaylistBundleDecodeResult(listOf(playlist), skippedPlaylists = 2, skippedTracks = 3)
    assertEquals(listOf(playlist), result.playlists)
    assertEquals(2, result.skippedPlaylists)
    assertEquals(3, result.skippedTracks)
}
```

Run the test and confirm it fails because the contracts do not exist.

**Implementation:**

- Add `serializationJson = "1.7.3"` and a `kotlinx-serialization-json` library alias for `org.jetbrains.kotlinx:kotlinx-serialization-json` to the version catalog.
- Add `implementation(libs.kotlinx.serialization.json)`; do not add the serialization compiler plugin because the implementation will use JSON tree APIs rather than `@Serializable` models.
- Define `PlaylistBundlePlaylist(name: String, paths: List<String>)`, `PlaylistBundleDecodeResult(playlists, skippedPlaylists, skippedTracks)`, and `UnsupportedPlaylistBundleException`.
- Define the exact constants asserted above. Keep these transient models outside Room.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleModelsTest'`. The contract tests pass.

### Task 2: Separate portable path validation from catalog resolution (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/M3u8CodecTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/M3u8Codec.kt`

**Test first:**

Add tests proving bundle parsing retains unknown-but-safe paths while standalone import behavior remains unchanged:

```kotlin
@Test fun portableParseRetainsUnknownRelativePathsAndSkipsUnsafePaths() {
    val result = M3u8Codec.parsePortable(
        "#EXTM3U\nArtist/Album/01.flac\nmissing.flac\n../escape.flac\nC:/outside.flac\n",
    )
    assertEquals(listOf("Artist/Album/01.flac", "missing.flac"), result.paths)
    assertEquals(listOf("../escape.flac", "C:/outside.flac"), result.skipped)
}

@Test fun standaloneParseStillSkipsUnknownPaths() {
    val result = M3u8Codec.parse("known.flac\nmissing.flac", setOf("known.flac"))
    assertEquals(listOf("known.flac"), result.paths)
    assertEquals(listOf("missing.flac"), result.skipped)
}
```

Confirm `parsePortable` is absent and the new test fails.

**Implementation:**

- Extract the current slash normalization and safe-relative-path checks into one private helper.
- Add `parsePortable(text)` that ignores comments/blank lines, normalizes safe relative paths, retains paths without consulting the catalog, and reports unsafe paths.
- Keep `parse(text, knownPaths)` by filtering the portable result against `knownPaths` and adding unknown paths to `skipped` in input order.
- Keep `encode` unchanged so every ZIP member remains a standard UTF-8 `.m3u8`.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*M3u8CodecTest'`. Existing and new tests pass.

### Task 3: Encode and decode the versioned manifest (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleManifestCodecTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleManifestCodec.kt`

**Test first:**

Test the exact schema and rejection rules:

```json
{
  "format": "nocturnel-playlists",
  "version": 1,
  "playlists": [
    { "name": "Road Trip", "entry": "playlists/0001-road-trip.m3u8" }
  ]
}
```

The test class must assert:

- Names containing quotes, Unicode, and newlines round-trip through JSON.
- Ordered records remain ordered.
- Missing/wrong `format`, unsupported `version`, non-array `playlists`, missing record fields, and more than 1,000 records throw `UnsupportedPlaylistBundleException`.
- Entry paths outside `playlists/`, absolute paths, backslashes, directory entries, and any `..` segment are rejected per record rather than trusted.

Run the test and confirm the codec is absent.

**Implementation:**

- Implement an internal `PlaylistBundleManifestCodec` using `kotlinx.serialization.json.Json`, `buildJsonObject`, and `parseToJsonElement`; do not introduce annotated serialization DTOs.
- Define an internal manifest record containing only `name` and `entry`.
- Encode the exact keys `format`, `version`, and `playlists`.
- Decode strictly enough to reject an unsupported bundle header while returning invalid record details so the ZIP decoder can skip individual bad playlist declarations.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleManifestCodecTest'`. All manifest tests pass.

### Task 4: Generate deterministic portable ZIP entry names (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodecTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodec.kt`

**Test first:**

Add focused helper tests:

```kotlin
@Test fun entryNamesAreUniqueReadableAndPortable() {
    assertEquals("playlists/0001-road-trip.m3u8", bundleEntryName(0, "Road Trip"))
    assertEquals("playlists/0002-road-trip.m3u8", bundleEntryName(1, "Road Trip"))
    assertEquals("playlists/0003-untitled-playlist.m3u8", bundleEntryName(2, "<>:\"/\\|?*"))
}
```

Also cover leading/trailing dots and spaces, repeated separators, Unicode fallback, and very long names. Confirm the helper is absent.

**Implementation:**

- Implement `bundleEntryName(index, name)` with a one-based four-digit prefix.
- Lowercase the readable portion, replace characters outside ASCII letters/digits with one `-`, trim separators/dots/spaces, cap the readable portion at 80 characters, and fall back to `untitled-playlist`.
- Treat numeric prefixes, not the sanitized name, as the uniqueness mechanism.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleCodecTest.entryNames*'`. The naming tests pass.

### Task 5: Stream bundle encoding (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodecTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodec.kt`

**Test first:**

Encode two same-named playlists plus an empty playlist into a `ByteArrayOutputStream`, inspect with `ZipInputStream`, and assert:

- `nocturnel-playlists.json` is the first entry.
- Three distinct files exist beneath `playlists/`.
- Each entry is UTF-8 `M3u8Codec.encode(paths)` content.
- The manifest preserves both exact duplicate names and maps them to distinct entries.
- The empty playlist contains a valid `#EXTM3U` document.
- The caller-owned output stream remains open.

Confirm the encode operation does not exist.

**Implementation:**

- Add `PlaylistBundleCodec.encode(playlists: List<PlaylistBundlePlaylist>, output: OutputStream)`.
- Precompute manifest records using `bundleEntryName`, write the manifest first, then each `.m3u8` with `ZipOutputStream` and explicit UTF-8 bytes.
- Reject more than 1,000 source playlists before writing.
- Finish the ZIP without closing the caller-owned stream.
- Do not add timestamps or unrelated metadata to the manifest.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleCodecTest'`. Encoding tests pass.

### Task 6: Decode a valid bundle and round-trip it (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodecTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodec.kt`

**Test first:**

Add a round-trip test using duplicate names, Unicode, an empty playlist, ordered paths, and one missing-but-safe path:

```kotlin
val original = listOf(
    PlaylistBundlePlaylist("Road Trip", listOf("A/02.flac", "A/01.flac", "missing.flac")),
    PlaylistBundlePlaylist("Road Trip", emptyList()),
    PlaylistBundlePlaylist("日本語", listOf("B/01.mp3")),
)
assertEquals(original, decode(encode(original)).playlists)
```

Assert skipped counts are zero and the caller-owned input stream remains open. Confirm decode is absent.

**Implementation:**

- Add `PlaylistBundleCodec.decode(input: InputStream): PlaylistBundleDecodeResult` using `ZipInputStream`.
- Read ZIP members into bounded byte arrays while counting actual uncompressed bytes; do not extract files.
- Require one valid `nocturnel-playlists.json`, then resolve only manifest-declared playlist entries.
- Decode manifest and `.m3u8` bytes with a strict UTF-8 decoder (`CodingErrorAction.REPORT`).
- Parse playlist content with `M3u8Codec.parsePortable` so safe missing paths survive.
- Return playlists in manifest order.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleCodecTest'`. The round-trip test passes.

### Task 7: Make bundle decoding best-effort (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleCorruptionTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodec.kt`

**Test first:**

Construct small ZIP fixtures and assert:

- One valid and one missing declared `.m3u8` imports the valid playlist and reports one skipped playlist.
- Invalid UTF-8 or unreadable playlist content skips only that playlist.
- Unsafe track paths increment `skippedTracks` while safe paths from the same playlist import.
- Duplicate physical ZIP entry names, duplicate manifest entry mappings, directory entries, unsupported compression/encryption, and undeclared files are never imported.
- A malformed/missing manifest rejects the entire document with `UnsupportedPlaylistBundleException`.

Run the tests and confirm at least the mixed-validity cases fail.

**Implementation:**

- Track physical ZIP names and manifest mappings independently.
- Count a playlist as skipped if its declaration is invalid, missing, duplicated, a directory, unreadable, or cannot be parsed.
- Continue processing other manifest records whenever the ZIP stream remains readable.
- Ignore undeclared entries except for total-size accounting.
- Convert unrecoverable ZIP structure/encryption errors into `UnsupportedPlaylistBundleException` rather than leaking `ZipException` to the UI.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleCorruptionTest'`. All partial-import cases pass.

### Task 8: Enforce ZIP bomb limits from bytes actually read (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistBundleLimitsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistBundleCodec.kt`

**Test first:**

Use test-only injectable limits (defaulting to production constants) to avoid allocating tens of megabytes. Assert:

- A playlist one byte over its per-entry limit is skipped and another valid playlist imports.
- Total bytes one byte over the aggregate limit reject the bundle.
- More than the configured playlist count rejects the manifest.
- Falsified `ZipEntry.size` metadata cannot bypass counting because limits use bytes read from the stream.

Confirm the decoder currently accepts at least one oversized fixture.

**Implementation:**

- Add an internal `PlaylistBundleLimits` value used by tests and a default instance backed by the approved constants.
- Stop reading an oversized playlist member after its limit, mark it skipped, and continue only when the stream can safely advance.
- Abort the complete bundle when actual cumulative uncompressed bytes exceed 64 MB.
- Never allocate an array based solely on ZIP metadata.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistBundleLimitsTest'`. All limit tests pass.

### Task 9: Resolve imported playlist names without overwriting (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/playlist/PlaylistImportNamingTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistImportNaming.kt`

**Test first:**

Cover exact and case-insensitive conflicts:

```kotlin
@Test fun choosesNextAvailableCaseInsensitiveSuffix() {
    val used = mutableSetOf("Favorites", "favorites (2)")
    assertEquals("Favorites (3)", uniqueImportedPlaylistName("Favorites", used))
    assertEquals("Road Trip", uniqueImportedPlaylistName("Road Trip", used))
}

@Test fun reservesNamesDuringOneBundleImport() {
    val used = mutableSetOf("Mix")
    assertEquals("Mix (2)", uniqueImportedPlaylistName("Mix", used))
    assertEquals("Mix (3)", uniqueImportedPlaylistName("Mix", used))
}
```

Confirm the resolver is absent.

**Implementation:**

- Implement one locale-independent, case-insensitive resolver using `lowercase(Locale.ROOT)` for comparisons.
- Trim imported names and fall back to `Imported playlist` when blank.
- Reserve the returned name in the supplied set before returning so duplicates within one bundle advance suffixes.
- Try the exact name, then `name (2)`, `name (3)`, and upward; never rename or replace an existing playlist.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistImportNamingTest'`. Naming tests pass.

### Task 10: Create a playlist and its entries atomically (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/LibraryDao.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistRepository.kt`

**Test first:**

Add an in-memory Room test that calls `PlaylistRepository.createWithEntries("Backup", listOf("present.flac", "missing.flac"))` and asserts one playlist plus both ordered entries exist. Add a failure-path test using a deliberately throwing transaction collaborator only if Room cannot induce a safe write failure; the contract is that no playlist is observable until the transaction completes.

Also assert a new snapshot query returns all playlists in the same name order as the existing Flow query.

Confirm `createWithEntries` and the snapshot query are absent.

**Implementation:**

- Add `suspend fun allPlaylists(): List<PlaylistEntity>` with `SELECT * FROM playlists ORDER BY name`.
- Add a Room `@Transaction` DAO method that inserts a playlist, maps ordered paths to `PlaylistEntryEntity`, saves non-empty entries, and returns the generated ID.
- Add `PlaylistRepository.createWithEntries(name, paths)` delegating to that transaction and applying the existing blank-name fallback.
- Do not change entities, database version, or schema.

**Verify:** Run `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.data.NocturneLDatabaseTest` on an attached device/emulator. The new database tests pass.

### Task 11: Add pure import orchestration and summaries (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistImportCommandTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistTransferCommands.kt`

**Test first:**

Create a command with injected suspend lambdas for existing names, known catalog paths, and atomic playlist creation. Assert:

- A standalone `.m3u8` keeps current known-path filtering and uses its suggested filename as the playlist name.
- A bundle retains safe unknown paths.
- Existing and within-bundle conflicts become `(2)` and `(3)` case-insensitively.
- Empty playlists are created.
- One creation failure increments skipped playlists and does not stop later records.
- Codec `skippedPlaylists`/`skippedTracks` counts are included in the final summary.
- The exact primary message is `Imported X playlist(s), Y track(s); skipped A playlist(s), B track(s).`

Confirm the command is absent.

**Implementation:**

- Define a sealed `PlaylistImportPayload` with `Standalone(suggestedName, text)` and `Bundle(PlaylistBundleDecodeResult)`.
- Define `PlaylistImportSummary(importedPlaylists, importedTracks, skippedPlaylists, skippedTracks)` with the exact message above.
- Implement `PlaylistImportCommand` that parses standalone input with `knownPaths`, consumes bundle records directly, resolves names sequentially, and invokes atomic `createWithEntries` once per valid playlist.
- Catch failures per bundle playlist; reject a failed standalone import as one failed operation.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistImportCommandTest'`. All orchestration tests pass.

### Task 12: Add pure export-all collection and summaries (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistExportCommandTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistTransferCommands.kt`

**Test first:**

Inject ordered playlists and per-ID paths, then assert:

- Every playlist is collected, including empty playlists.
- Duplicate names remain distinct records.
- Entry ordering is unchanged.
- A path-loading failure fails the whole export before document writing.
- The exact success message is `Exported X playlist(s).`

Confirm the export command is absent.

**Implementation:**

- Implement `PlaylistExportCommand` with injected `allPlaylists` and `paths` functions.
- Return `List<PlaylistBundlePlaylist>` only after all paths load successfully so callers never write a partial backup.
- Define `PlaylistExportSummary(exportedPlaylists)` and its exact message.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistExportCommandTest'`. Export collection tests pass.

### Task 13: Stream standalone and bundle documents through ContentResolver (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/playlist/PlaylistDocumentServiceTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/PlaylistDocumentService.kt`, `app/src/main/java/ca/stewark/nocturnel/playlist/M3u8DocumentService.kt`

**Test first:**

Using instrumentation `cacheDir` files opened through `ContentResolver`/`Uri`, assert:

- ZIP magic bytes select `PlaylistBundleCodec.decode` even when the filename has no `.zip` suffix.
- Plain UTF-8 selects `PlaylistImportPayload.Standalone` and derives a suggested name without the last extension.
- `writeBundle` produces a decodable ZIP.
- `writeM3u8` preserves existing single-playlist output.
- Missing input/output streams and malformed bundles produce domain failures and close opened streams.

Confirm the new service is absent.

**Implementation:**

- Replace `M3u8DocumentService` with `PlaylistDocumentService(ContentResolver)` and update its filename accordingly.
- Preserve `readM3u8`/`writeM3u8` behavior for individual exports.
- Add `readImport(uri)` using a buffered stream with `mark/reset`; detect ZIP by `PK\u0003\u0004`, `PK\u0005\u0006`, or `PK\u0007\u0008`, otherwise read strict UTF-8 text.
- Add `writeBundle(uri, playlists)` delegating to `PlaylistBundleCodec.encode` without buffering the entire ZIP.
- Resolve a display name with `DocumentFile.fromSingleUri` and fall back to `uri.lastPathSegment` for standalone suggested names.
- Always close resolver-owned streams with `use`.

**Verify:** Run the focused connected test class. All document-service tests pass.

### Task 14: Wire transfer commands into PlaylistViewModel (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistTransferMessageTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModel.kt`

**Test first:**

Add pure message/state tests for these exact outcomes:

- Import success/partial success uses the command summary message.
- `Playlist import failed`, `Playlist export failed`, `Playlist import cancelled`, and `Playlist export cancelled` are distinct.
- Export-all success reports its playlist count.
- Existing single-playlist success remains `Playlist exported`.

Confirm cancellation and bulk-export states do not exist.

**Implementation:**

- Replace the old document service with `PlaylistDocumentService`.
- Build import/export commands from `dao::allPlaylists`, `dao::allTracks`, `repository::paths`, and `repository::createWithEntries`.
- Change `import(uri)` to call `readImport` then the import command.
- Keep `export(playlistId, uri)` as an individual `.m3u8` export.
- Add `exportAll(uri)` that collects all records before opening/writing the destination and then calls `writeBundle`.
- Add explicit `importCancelled()` and `exportCancelled()` message setters; cancellation performs no repository or document operation.
- Map exceptions only to the approved concise failure messages.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistImportCommandTest' --tests '*PlaylistExportCommandTest' --tests '*PlaylistTransferMessageTest'`. All transfer-state tests pass.

### Task 15: Add IMPORT and EXPORT ALL to the playlist index (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistIndexScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`

**Test first:**

Extract/test a stateless playlist-index composable and assert:

- `[ IMPORT ]` and `[ EXPORT ALL ]` are displayed.
- Clicking each invokes only its corresponding callback.
- `[ EXPORT ALL ]` remains enabled when the playlist list is empty so an empty complete backup can be created.
- Per-playlist `[ EXPORT ]`, `[ OPEN ]`, `[ PLAY ]`, and `[ DELETE ]` remain present.
- Existing create behavior and notices remain visible.

Confirm the composable/actions are absent.

**Implementation:**

- Extract the index branch into an internal `PlaylistIndexScreen` receiving state and callbacks; keep detail navigation in `PlaylistsScreen`.
- Rename the visible `IMPORT M3U8` label to `IMPORT`.
- Add `EXPORT ALL` beside `IMPORT` without adding selection UI.
- Add an `OpenDocument` launcher accepting `audio/x-mpegurl`, `application/vnd.apple.mpegurl`, `text/plain`, `application/zip`, and `application/x-zip-compressed`.
- Add a `CreateDocument("application/zip")` launcher with default filename `NocturneL Playlists.zip`.
- Route null import/export-all results to the corresponding cancellation methods.
- Leave the existing per-playlist create-document flow and `.m3u8` filename unchanged.

**Verify:** Compile and run `PlaylistIndexScreenTest` on an attached device/emulator.

### Task 16: Lock document-picker wiring and user-facing documentation (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistTransferWiringTest.kt`, `README.md`, `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Add a small source-wiring guard that reads `PlaylistsScreen.kt` and asserts it contains:

```kotlin
assertTrue("application/zip" in source)
assertTrue("application/x-zip-compressed" in source)
assertTrue("NocturneL Playlists.zip" in source)
assertTrue("viewModel.exportAll" in source)
assertTrue("viewModel.importCancelled" in source)
assertTrue("viewModel.exportCancelled" in source)
```

Confirm the guard fails before the completed UI wiring.

**Implementation:**

- Update README playlist portability text to explain individual `.m3u8` export and all-playlist ZIP backup/import.
- Extend the Pixel 7 checklist with: create five playlists including one empty/duplicate name, export all, delete/import, verify ordering and conflict suffixes, and verify a ZIP with one malformed playlist imports the remaining valid playlists.
- Do not add cloud backup, selective export, settings, or artwork behavior.

**Verify:** Run `./gradlew testDebugUnitTest --tests '*PlaylistTransferWiringTest'` and inspect the two documentation updates.

### Task 17: Run the full release checks (2–5 min)

**Files:** No new files expected; inspect the complete diff.

**Test first:**

Before declaring completion, run the focused suites once without up-to-date assumptions and confirm a deliberately failing assertion is not left in any new test. No production changes belong in this task.

**Implementation:**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files (x86)\Android\openjdk\jdk-17.0.14'
$env:ANDROID_HOME='C:\Program Files (x86)\Android\android-sdk'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat lintDebug assembleDebug
git diff --check
git status --short
```

If a device/emulator is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Then complete the new manual ZIP round-trip checklist. If the repository's existing Media3 `UnstableApi` lint violations still block `lintDebug`, report them separately and do not hide them with a baseline or unrelated suppression.

**Verify:** All new unit tests pass, Android tests compile, the debug APK assembles, no new lint finding is introduced, `git diff --check` is clean, and only planned files plus the already-active scroll/visualizer changes are modified.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every production behavior was introduced after its failing test.
- [ ] `EXPORT ALL` writes every playlist, including empty and duplicate-named playlists, to one ZIP.
- [ ] The ZIP contains a valid versioned manifest and independently usable UTF-8 `.m3u8` entries.
- [ ] Import automatically handles standalone `.m3u8`/`.m3u` files and NocturneL ZIP bundles.
- [ ] Exact names and path order round-trip; name conflicts become `(2)`, `(3)`, and onward without overwriting.
- [ ] Bundle imports preserve safe missing paths and skip unsafe paths.
- [ ] Mixed valid/invalid bundles import valid playlists and report concise counts.
- [ ] ZIP entry, per-playlist, total-size, traversal, malformed-manifest, and corrupt-content protections are tested.
- [ ] Playlist creation plus entries is atomic and requires no Room migration.
- [ ] Existing individual import/export and per-playlist actions remain functional.
- [ ] All unit tests pass; Android tests compile and pass when a device is available.
- [ ] Debug assembly passes and no new lint issue is introduced.
- [ ] `git diff --check` passes.
- [ ] No unplanned files are modified.
- [ ] The Pixel 7 ZIP export/delete/import round trip is completed before release.
