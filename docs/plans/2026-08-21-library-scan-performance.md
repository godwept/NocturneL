# Library Scan Performance Implementation Plan

**Date:** 2026-08-21  
**Design doc:** docs/specs/2026-08-21-library-scan-performance-design.md  
**Status:** Ready for review

## Overview

Add conservative file fingerprints so unchanged tracks can reuse catalog metadata, separate tag and artwork reads so each dirty album stops after its first usable embedded image, and preserve the current atomic scan lifecycle. Build device-only comparison harnesses for direct `DocumentsContract` enumeration and metadata concurrency limits of one, two, and four; record the results and promote a candidate to production only when it is repeatably faster and result-equivalent on the same device and library.

## Tasks

### Task 1: Define reliable fingerprint matching

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/FileFingerprint.kt`, `app/src/test/java/ca/stewark/nocturnel/library/FileFingerprintTest.kt`

**Test first:**

```kotlin
@Test fun matchingReliableFingerprintCanBeReused() {
    assertTrue(FileFingerprint(42, 1_000).matches(FileFingerprint(42, 1_000)))
}

@Test fun unknownOrInvalidModifiedTimeIsNeverReusable() {
    assertFalse(FileFingerprint(42, null).matches(FileFingerprint(42, null)))
    assertFalse(FileFingerprint(42, 0).matches(FileFingerprint(42, 0)))
}

@Test fun changedSizeOrModifiedTimeIsNotReusable() {
    assertFalse(FileFingerprint(42, 1_000).matches(FileFingerprint(43, 1_000)))
    assertFalse(FileFingerprint(42, 1_000).matches(FileFingerprint(42, 1_001)))
}
```

**Implementation:**

Add a small immutable `FileFingerprint(fileSizeBytes: Long?, lastModifiedEpochMillis: Long?)`. Treat size as valid when it is non-null and non-negative, and modified time as valid only when it is greater than zero. `matches` must return true only when both fingerprints are valid and both values are equal. Keep URI/path comparison outside this value object because the scanner already keys tracks by path and must compare the document URI explicitly.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.FileFingerprintTest`. All validity and equality cases pass.

---

### Task 2: Add nullable fingerprint columns to tracks

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/entity/LibraryEntities.kt`, `app/src/test/java/ca/stewark/nocturnel/data/entity/TrackFingerprintFieldsTest.kt`

**Test first:**

```kotlin
@Test fun legacyConstructionDefaultsFingerprintToUnknown() {
    val track = track(fileSizeBytes = null, lastModifiedEpochMillis = null)
    assertNull(track.fileSizeBytes)
    assertNull(track.lastModifiedEpochMillis)
}

@Test fun discoveredFingerprintIsRetained() {
    val track = track(fileSizeBytes = 123, lastModifiedEpochMillis = 456)
    assertEquals(123, track.fileSizeBytes)
    assertEquals(456, track.lastModifiedEpochMillis)
}
```

**Implementation:**

Append `fileSizeBytes: Long? = null` and `lastModifiedEpochMillis: Long? = null` to `TrackEntity`. Put the fields at the end and give both Kotlin defaults so existing positional test fixtures continue compiling. Do not add fingerprint fields to album, playlist, or listening entities.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.data.entity.TrackFingerprintFieldsTest`. Both nullable and populated cases pass.

---

### Task 3: Migrate the Room database from version 2 to 3

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/Migrations.kt`, `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseMigrationTest.kt`, `app/schemas/ca.stewark.nocturnel.data.NocturneLDatabase/3.json`

**Test first:**

```kotlin
@Test fun migrationTwoToThreePreservesTracksWithUnknownFingerprints() {
    helper.createDatabase(TEST_DB, 2).apply {
        execSQL("INSERT INTO tracks(relativePath, documentUri, albumId, title, artist, album, durationMs, trackNumber, discNumber, status, lastSeenScanEpochMillis) VALUES('a.mp3', 'content://a', 'album', 'A', 'Artist', 'Album', 1000, 1, 1, 'PLAYABLE', 1)")
        close()
    }
    helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).apply {
        query("SELECT title, fileSizeBytes, lastModifiedEpochMillis FROM tracks").use { cursor ->
            cursor.moveToFirst()
            assertEquals("A", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }
        close()
    }
}
```

**Implementation:**

Add `MIGRATION_2_3` with two `ALTER TABLE tracks ADD COLUMN ... INTEGER` statements, leaving both columns nullable and without SQL defaults. Bump `NocturneLDatabase` to version 3 and register `MIGRATION_2_3` after `MIGRATION_1_2` in `NocturneLApplication`. Generate and commit schema `3.json`; do not edit older exported schemas.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.data.NocturneLDatabaseMigrationTest`. Both 1-to-2 and 2-to-3 migrations validate.

---

### Task 4: Introduce a provider-neutral discovery contract

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryDocumentEnumerator.kt`, `app/src/test/java/ca/stewark/nocturnel/library/DiscoveredDocumentTest.kt`

**Test first:**

```kotlin
@Test fun discoveredAudioCarriesTheFingerprintWithoutAndroidDocumentObjects() {
    val document = DiscoveredDocument("Artist/Album/01.mp3", "content://track/1", "01.mp3", 42, 1_000)
    assertEquals(FileFingerprint(42, 1_000), document.fingerprint)
}
```

**Implementation:**

Define `DiscoveredDocument(relativePath: String, documentUri: String, displayName: String, fileSizeBytes: Long?, lastModifiedEpochMillis: Long?)` with a derived `fingerprint`. Define `LibraryDocumentEnumerator` with `canAccess(treeUri: String)` and `enumerate(treeUri, cancelled): List<DiscoveredDocument>`. Keep this contract free of `DocumentFile` and `Uri` so scanner tests can run as local JVM tests.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.DiscoveredDocumentTest`. The provider-neutral model test passes.

---

### Task 5: Adapt current DocumentFile traversal to the discovery contract

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/DocumentTreeWalker.kt`, `app/src/main/java/ca/stewark/nocturnel/library/DocumentFileEnumerator.kt`, `app/src/main/java/ca/stewark/nocturnel/library/LibraryTreeAccess.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/library/DocumentFileEnumeratorTest.kt`

**Test first:**

```kotlin
@Test fun enumeratorReturnsPathUriSizeAndModifiedTime() {
    val root = FakeDocumentFile.directory("root",
        FakeDocumentFile.directory("Artist",
            FakeDocumentFile.directory("Album",
                FakeDocumentFile.file("01.mp3", uri = "content://track/1", length = expectedSize, modified = expectedModified))))
    val result = enumerator(openTree = { root }).enumerate("content://tree") { false }
    assertEquals("Artist/Album/01.mp3", result.single().relativePath)
    assertTrue(result.single().documentUri.isNotBlank())
    assertEquals(expectedSize, result.single().fileSizeBytes)
    assertEquals(expectedModified, result.single().lastModifiedEpochMillis)
}
```

**Implementation:**

Make `DocumentFileEnumerator` implement `LibraryDocumentEnumerator` using the existing `LibraryTreeAccess` and recursive `DocumentTreeWalker`. Give it an internal constructor accepting `canAccess` and `openTree` functions so the instrumentation test can use a nested `FakeDocumentFile` hierarchy without a real provider. Convert each file to the new DTO and normalize provider values: preserve zero-byte file size, convert negative sizes to null, and convert non-positive modified times to null. Continue checking cancellation before opening a folder and before each child. Keep current recursive ordering and access checks unchanged.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.library.DocumentFileEnumeratorTest`. The adapter returns the expected normalized fields and respects cancellation.

---

### Task 6: Separate tag reads from artwork reads

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/MediaMetadataReader.kt`, `app/src/main/java/ca/stewark/nocturnel/library/AndroidMediaMetadataReader.kt`, `app/src/test/java/ca/stewark/nocturnel/library/AndroidMediaMetadataReaderTest.kt`

**Test first:**

```kotlin
@Test fun tagReadDoesNotReturnArtworkBytes() {
    val session = FakeMetadataSession(tags = fixtureTags, artwork = expectedArtwork)
    val tags = reader(session).readTags("content://fixture").getOrThrow()
    assertEquals("Fixture title", tags.title)
    assertEquals(0, session.artworkReads)
}

@Test fun artworkReadReturnsEmbeddedBytesIndependently() {
    val session = FakeMetadataSession(tags = fixtureTags, artwork = expectedArtwork)
    assertArrayEquals(expectedArtwork, reader(session).readArtwork("content://fixture").getOrThrow())
    assertEquals(1, session.artworkReads)
}
```

**Implementation:**

Define `MediaMetadataReader` with `readTags(documentUri: String): Result<ReadMetadata>` and `readArtwork(documentUri: String): Result<ByteArray?>`. Remove `embeddedArtwork` from `ReadMetadata`. Add internal `MetadataSession` and session-factory adapters around `MediaMetadataRetriever` so local tests can count metadata and artwork access without a binary media fixture; the public production constructor still accepts `Context`. Implement both reader methods with a short-lived session, parsing and setting the URI only in the Android factory. Keep the existing tag parsing and `use` cleanup behavior.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.AndroidMediaMetadataReaderTest`. Tag reads never access artwork, artwork reads do, and both close their session.

---

### Task 7: Add existing-catalog snapshot and scanner test fakes

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/ExistingCatalogSnapshot.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun emptySnapshotHasNoTracksOrAlbums() {
    assertTrue(ExistingCatalogSnapshot.Empty.tracksByPath.isEmpty())
    assertTrue(ExistingCatalogSnapshot.Empty.albumsById.isEmpty())
}
```

**Implementation:**

Add `ExistingCatalogSnapshot` with `tracksByPath` and `albumsById`, plus an `Empty` instance and a factory from track/album lists. In `LibraryScannerTest`, add reusable fake implementations of `LibraryDocumentEnumerator` and `MediaMetadataReader` that record tag/artwork URIs and can return configured results. Add entity factory methods with named arguments so later tests remain readable.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest`. The snapshot and fake harness compile and pass.

---

### Task 8: Refactor LibraryScanner onto the injectable contracts

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun baselineScanPreservesOrderProgressAndFallbackMetadata() {
    val result = scanner.scan("content://tree", 10, ExistingCatalogSnapshot.Empty, onProgress = progress::add)
    assertEquals(discoveredPaths, result.tracks.map { it.relativePath })
    assertEquals(listOf(ScanProgress.Discovering, ScanProgress.Indexing(1, 2), ScanProgress.Indexing(2, 2)), progress)
}
```

**Implementation:**

Define a `LibraryScanEngine` interface containing `canAccess` and `scan`, then make `LibraryScanner` implement it so repository tests can inject a fake. Change `LibraryScanner` to depend on `LibraryDocumentEnumerator` and `MediaMetadataReader`. Add `existingCatalog: ExistingCatalogSnapshot = ExistingCatalogSnapshot.Empty` to `scan`. Replace all `DocumentFile` property reads with DTO fields, retaining candidate filtering, folder-cover selection, fallback metadata, issue text, ordering, progress, cancellation, album IDs, and scan outcomes. Wire `DocumentFileEnumerator(treeAccess)` and `AndroidMediaMetadataReader(this)` in `NocturneLApplication`.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest --tests ca.stewark.nocturnel.library.ScanProgressTest`. Baseline behavior is unchanged behind the new contracts.

---

### Task 9: Reuse a matching playable track

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun unchangedPlayableTrackSkipsTagReadAndAdvancesProgress() {
    val result = scanner(existingPlayableTrack(size = 42, modified = 1_000)).scan(...)
    assertTrue(reader.tagUris.isEmpty())
    assertEquals(10, result.tracks.single().lastSeenScanEpochMillis)
    assertEquals(42, result.tracks.single().fileSizeBytes)
    assertEquals(ScanProgress.Indexing(1, 1), progress.last())
}
```

**Implementation:**

Index the snapshot by relative path. Reuse a track only when its relative path lookup succeeds, its stored `documentUri` equals the discovered URI, its status is `PLAYABLE`, and `FileFingerprint.matches` succeeds. Copy the cached track with the current scan epoch and current fingerprint values. Do not call `readTags` for a reused track.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest`. The unchanged case records zero tag reads and correct progress.

---

### Task 10: Conservatively reread non-reusable tracks

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun changedUnknownAndPriorMetadataIssueTracksAreReread() {
    // Cover changed size, changed modified time, unknown modified time, URI change,
    // and a matching METADATA_ISSUE track.
    assertEquals(expectedUris, reader.tagUris)
}

@Test fun freshMetadataFailureStoresCurrentFingerprintAndIssue() {
    assertEquals("METADATA_ISSUE", result.tracks.single().status)
    assertEquals(42, result.tracks.single().fileSizeBytes)
    assertEquals(1_000, result.tracks.single().lastModifiedEpochMillis)
    assertEquals("Could not read media metadata", result.issues.single().message)
}
```

**Implementation:**

Route every non-reusable audio document through `readTags`. Store the discovered fingerprint on both successful and failed track records, while continuing to force every `METADATA_ISSUE` record through the reader on later scans. Preserve fallback metadata and current per-track issue behavior. A changed URI, addition, or move must never reuse the old record.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest`. All conservative reread cases pass.

---

### Task 11: Model clean and dirty albums

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/AlbumScanPolicy.kt`, `app/src/test/java/ca/stewark/nocturnel/library/AlbumScanPolicyTest.kt`

**Test first:**

```kotlin
@Test fun albumIsCleanOnlyWhenMembershipAndEveryTrackAreReusable() {
    assertTrue(isAlbumClean(currentPaths, previousPaths, reusablePaths = currentPaths))
    assertFalse(isAlbumClean(currentPaths + "03.mp3", previousPaths, reusablePaths = currentPaths))
    assertFalse(isAlbumClean(currentPaths, previousPaths + "gone.mp3", reusablePaths = currentPaths))
}
```

**Implementation:**

Add a pure album policy that compares the current and previous relative-path sets for an album and requires every current member to be reusable. A new, changed, moved, removed, unknown-fingerprint, or metadata-error member makes the album dirty. Keep this policy independent of Android APIs and artwork extraction.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.AlbumScanPolicyTest`. Membership and reuse cases pass.

---

### Task 12: Reuse clean albums and rebuild dirty album metadata

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun cleanAlbumReusesStoredMetadataAndArtwork() {
    assertEquals(existingAlbum, result.albums.single())
    assertTrue(reader.artworkUris.isEmpty())
}

@Test fun addedChangedOrRemovedMemberRebuildsAlbum() {
    assertEquals("Updated album", result.albums.single().title)
}
```

**Implementation:**

Group current audio documents and previous tracks by album ID, apply `AlbumScanPolicy`, and copy the existing `AlbumEntity` unchanged for clean albums. Rebuild dirty albums in stable discovery order using the first current track's freshly read tags when available, otherwise its reused track fields plus the existing album year when applicable. Continue setting scanned `manualArtworkUri` to null so the DAO remains the single place that preserves manual artwork.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest`. Clean albums are byte-for-byte reused and dirty metadata is rebuilt deterministically.

---

### Task 13: Stop artwork probing after the first usable image

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun dirtyAlbumStopsAfterFirstUsableArtwork() {
    reader.artworkResults = mapOf("uri:1" to null, "uri:2" to byteArrayOf(1), "uri:3" to byteArrayOf(2))
    assertArrayEquals(byteArrayOf(1), result.albums.single().embeddedArtwork)
    assertEquals(listOf("uri:1", "uri:2"), reader.artworkUris)
}

@Test fun artworkFailuresContinueToTheNextTrack() {
    reader.artworkResults = mapOf("uri:1" to failure(), "uri:2" to byteArrayOf(1))
    assertEquals(listOf("uri:1", "uri:2"), reader.artworkUris)
}
```

**Implementation:**

For each dirty album, call `readArtwork` in stable track order until a successful non-null, non-empty byte array is returned. Treat exceptions, null, and empty arrays as no usable artwork and continue. Do not probe clean albums. Preserve folder-art URI selection independently, and do not turn artwork-only failures into track metadata issues.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest`. Artwork call counts prove early termination and fallback behavior.

---

### Task 14: Supply the existing snapshot from CatalogRepository

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/dao/LibraryDao.kt`, `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/CatalogRepositoryIncrementalTest.kt`

**Test first:**

```kotlin
@Test fun sameSourceRescanPassesStoredTracksAndAlbumsToScanner() = runTest {
    repository.rescan()
    assertEquals(listOf(storedTrack), scanner.capturedSnapshot.tracksByPath.values.toList())
    assertEquals(listOf(storedAlbum), scanner.capturedSnapshot.albumsById.values.toList())
}

@Test fun replacementSourcePassesAnEmptySnapshot() = runTest {
    repository.scanSelectedSource("content://new", "New")
    assertSame(ExistingCatalogSnapshot.Empty, scanner.capturedSnapshot)
}
```

**Implementation:**

Add `suspend fun allAlbums(): List<AlbumEntity>` to `LibraryDao` and change the `CatalogRepository` constructor dependency from concrete `LibraryScanner` to `LibraryScanEngine`. Load tracks and albums before scanning a same-source rescan, construct one `ExistingCatalogSnapshot`, pass it to `scan`, and reuse its track list for reconciliation rather than querying twice. Pass `Empty` for initial or replacement sources. Preserve access checks, source-change detection, transactions, and exception mapping. The instrumentation test uses an in-memory database and a fake `LibraryScanEngine` that captures its snapshot and returns a configured completed result.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.data.CatalogRepositoryIncrementalTest`. Same-source and replacement snapshot behavior passes.

---

### Task 15: Verify fingerprint persistence and atomic cancellation

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/CatalogRepositoryIncrementalTest.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerTest.kt`

**Test first:**

```kotlin
@Test fun completedScanPersistsTrackFingerprint() = runTest {
    dao.replaceCompletedScan(albums, listOf(track(fileSizeBytes = 42, lastModifiedEpochMillis = 1_000)), report, emptyList())
    assertEquals(42, dao.track(path)?.fileSizeBytes)
    assertEquals(1_000, dao.track(path)?.lastModifiedEpochMillis)
}

@Test fun cancellationDuringArtworkReturnsCancelledWithoutPartialResult() {
    // Cancel from the fake artwork reader and assert the scanner outcome is CANCELLED.
}

@Test fun cancelledScanLeavesTheStoredCatalogUnchanged() = runTest {
    // Seed the in-memory database, return CANCELLED from the fake scan engine,
    // then assert the source, album, and track rows are unchanged.
}
```

**Implementation:**

Add cancellation checks before each tag read and artwork probe, and after discovery/processing before returning `COMPLETED`. Keep repository persistence gated on `COMPLETED`; do not introduce per-file writes. Confirm the Room batch insert includes the new entity fields automatically, and use the repository fake-engine test to prove a cancelled result cannot mutate the prior catalog.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerTest` and `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.data.NocturneLDatabaseTest`. Cancellation and persistence cases pass.

---

### Task 16: Add a direct DocumentsContract profiling enumerator

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/library/profile/DocumentsContractProfileEnumerator.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/library/profile/DocumentsContractProfileEnumeratorTest.kt`

**Test first:**

```kotlin
@Test fun directEnumeratorMatchesDocumentFileResults() {
    val baseline = DocumentFileEnumerator(treeAccess).enumerate(treeUri, cancelled = { false })
    val direct = DocumentsContractProfileEnumerator(contentResolver).enumerate(treeUri, cancelled = { false })
    assertEquals(normalize(baseline), normalize(direct))
}
```

**Implementation:**

Create an androidTest-only recursive enumerator using `DocumentsContract.getTreeDocumentId`, `buildChildDocumentsUriUsingTree`, and `buildDocumentUriUsingTree`. Query document ID, display name, MIME type, size, and last modified in one cursor per directory; close every cursor and check cancellation between rows and before recursion. Normalize output exactly like `DocumentFileEnumerator`. Keep this candidate outside `main` until the profiling gate passes.

**Verify:** Run the dedicated test against the selected real-library tree as described in Task 18. Baseline and direct normalized path/URI/fingerprint sets match.

---

### Task 17: Add bounded metadata profiling runners

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/library/profile/MetadataProfileRunner.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/library/profile/MetadataProfileRunnerTest.kt`

**Test first:**

```kotlin
@Test fun limitsOneTwoAndFourReturnResultsInInputOrder() = runTest {
    for (limit in listOf(1, 2, 4)) {
        val result = runner.readTags(documents, parallelism = limit)
        assertEquals(documents.map { it.documentUri }, result.map { it.documentUri })
    }
}

@Test fun failedReadCancelsSiblingWork() = runTest {
    // Configure one fake read to throw and assert no worker survives the runner scope.
}
```

**Implementation:**

Create an androidTest-only structured-concurrency runner using `coroutineScope`, indexed inputs, `async(Dispatchers.IO)`, and a `Semaphore` for limits 1, 2, and 4. Restore discovery order by index before comparison. Provide separate tag and artwork timing operations; never share a `MediaMetadataRetriever` between workers.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.library.profile.MetadataProfileRunnerTest`. Ordering, limits, failure, and scope cleanup pass.

---

### Task 18: Build and run the device profiling gate

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/library/profile/LibraryScanPerformanceProfileTest.kt`, `docs/testing/library-scan-performance-profile.md`

**Test first:**

```kotlin
@Test fun candidatesProduceEquivalentResultsAndReportStageMedians() = runTest {
    assumeTrue(InstrumentationRegistry.getArguments().getString("scanProfile") == "true")
    // Load the app's selected source, warm each candidate once, measure five runs,
    // assert normalized equivalence, and print discovery/tag/artwork/persistence medians.
}
```

**Implementation:**

Add a profiling-only instrumentation test guarded by `scanProfile=true`. Read the currently selected source from the target app database and fail with an actionable message if none is configured. Warm each candidate once, then measure five runs using `SystemClock.elapsedRealtimeNanos`; compare normalized discovery output and metadata results before reporting medians. Time baseline/direct discovery, tag limits 1/2/4, artwork probing, and a completed-scan transaction into a temporary in-memory Room database. Do not mutate the user's catalog. Add a results document containing device/build, library counts, all raw runs, medians, equivalence/errors, and explicit `ADOPT` or `REJECT` decisions for direct traversal and concurrency.

**Verify:** Install the debug app, select the real music folder, then run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.library.profile.LibraryScanPerformanceProfileTest --instrumentation-arg scanProfile=true`. Copy the reported measurements into `docs/testing/library-scan-performance-profile.md`; every candidate must be result-equivalent before it can be marked `ADOPT`.

---

### Task 19: Apply the direct-traversal decision

**Files:** `docs/testing/library-scan-performance-profile.md`; conditional when marked `ADOPT`: `app/src/main/java/ca/stewark/nocturnel/library/DocumentsContractEnumerator.kt`, `app/src/main/java/ca/stewark/nocturnel/library/FallbackDocumentEnumerator.kt`, `app/src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/library/DocumentsContractEnumeratorTest.kt`

**Test first:**

```kotlin
@Test fun unsupportedDirectProviderFallsBackToDocumentFileEnumeration() {
    val result = FallbackDocumentEnumerator(failingDirect, baseline).enumerate(treeUri) { false }
    assertEquals(baselineDocuments, result)
}
```

**Implementation:**

If Task 18 marks direct traversal `REJECT`, record the reason and make no production-code change in this task. If marked `ADOPT`, move the proven direct implementation into `main` and translate only provider capability or query-shape failures into a dedicated `DirectEnumerationUnsupportedException`. Wrap it with `FallbackDocumentEnumerator` that retries the complete scan with `DocumentFileEnumerator` only for that exception, and wire the fallback wrapper in `NocturneLApplication`. Never merge partial direct results with fallback results, and do not mask cancellation or access revocation as an unsupported-provider fallback.

**Verify:** For `ADOPT`, run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.library.DocumentsContractEnumeratorTest` and rerun the profile equivalence test. For `REJECT`, verify the results document names the measured reason and `NocturneLApplication` still uses `DocumentFileEnumerator`.

---

### Task 20: Apply the bounded-concurrency decision

**Files:** `docs/testing/library-scan-performance-profile.md`; conditional when limit 2 or 4 is marked `ADOPT`: `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/test/java/ca/stewark/nocturnel/library/LibraryScannerConcurrencyTest.kt`

**Test first:**

```kotlin
@Test fun concurrentReadsPreserveDiscoveryOrder() = runTest {
    assertEquals(discoveredPaths, scanner.scan(...).tracks.map { it.relativePath })
}

@Test fun cancellationStopsAllMetadataWorkersAndReturnsNoCompletedResult() = runTest {
    // Block fake readers, cancel the scan, release them, and assert every child completed/cancelled.
}
```

**Implementation:**

If Task 18 marks every concurrent candidate `REJECT`, record the reason and keep sequential production reads. If limit 2 or 4 is marked `ADOPT`, make `LibraryScanner.scan` suspend, run only required tag reads through structured concurrency with the selected fixed semaphore limit, restore discovery order before album construction, and keep artwork probing ordered and stop-on-first. Update `CatalogRepository` to call the suspend scanner directly from its existing IO context. Do not expose a runtime setting or use unbounded/default parallelism.

**Verify:** For `ADOPT`, run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.LibraryScannerConcurrencyTest --tests ca.stewark.nocturnel.library.LibraryScannerTest` and rerun the device profile. For `REJECT`, verify the results document records the decision and production scanning remains sequential.

---

### Task 21: Run regression and manual scan checks

**Files:** `docs/testing/pixel-7-release-checklist.md`, `docs/testing/library-scan-performance-profile.md`

**Test first:**

```text
No new production behavior is introduced in this task; it executes the complete automated and manual verification accumulated above.
```

**Implementation:**

Add checklist entries for: first post-upgrade scan populating fingerprints; an unchanged second scan performing no tag/artwork reads for reliable providers; unknown timestamps rereading conservatively; tag changes refreshing metadata; added, moved, and deleted files reconciling correctly; artwork falling through to the first usable track; cancellation preserving the old catalog; and revoked access retaining the existing error flow. Record before/after scan timings for the same library and device without asserting a fixed target.

**Verify:** Run `./gradlew.bat testDebugUnitTest`, `./gradlew.bat connectedDebugAndroidTest`, and `./gradlew.bat validateDebugScreenshotTest`. Install the debug build and complete the new checklist entries on the profiled device. Confirm `git status --short` contains only planned files plus the user's pre-existing unrelated changes.

## Definition of Done

- [ ] All tasks completed in order, with conditional profiling decisions recorded explicitly.
- [ ] All new production code has test-first coverage.
- [ ] All unit, instrumentation, migration, and screenshot tests pass.
- [ ] Unchanged reliable tracks perform no metadata or artwork reads.
- [ ] Unknown fingerprints and prior metadata failures are reread conservatively.
- [ ] Dirty albums stop artwork probing after the first usable image.
- [ ] Cancellation, access loss, ordering, and atomic catalog persistence remain correct.
- [ ] Direct traversal and bounded concurrency are enabled only if the recorded device evidence meets the approved gate.
- [ ] No unplanned files are modified.
- [ ] The feature behaves exactly as described in the approved design document.
