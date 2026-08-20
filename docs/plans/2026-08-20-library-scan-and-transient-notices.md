# Library Scan and Transient Notices Implementation Plan

**Date:** 2026-08-20  
**Design doc:** docs/specs/2026-08-20-library-scan-and-transient-notices-design.md  
**Status:** Ready for review

## Overview

Make every accepted music-folder selection begin an immediate, cancellable scan. Report a discovery stage before the document total is known and determinate indexing progress afterwards. Stage replacement-folder results until success so the prior catalog remains usable after failure or cancellation. Add a reusable five-second expiry helper for routine success notices while retaining errors and cancellation feedback.

## Tasks

### Task 1: Define structured scan progress

**Files:** `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/library/ScanProgressTest.kt`

**Test first:**

```kotlin
@Test fun indexingProgressRetainsCompletedAndTotal() {
    assertEquals(ScanProgress.Indexing(completed = 7, total = 10), ScanProgress.Indexing(7, 10))
}

@Test fun discoveryProgressHasNoPretendPercentage() {
    assertEquals(ScanProgress.Discovering, ScanProgress.Discovering)
}
```

**Implementation:**

Add a `ScanProgress` sealed interface with `Discovering` and `Indexing(completed, total)` cases. Replace `LibraryScanner.scan`'s `onProgress: (Int) -> Unit` parameter with `onProgress: (ScanProgress) -> Unit`. Emit `Discovering` before materializing `DocumentTreeWalker.walk(...)`, then emit `Indexing(count, documents.size)` once for each traversed document. Preserve the existing cancellation checks and scan result contents.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.library.ScanProgressTest`. The model tests pass.

---

### Task 2: Thread structured progress through the catalog repository

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/main/java/ca/stewark/nocturnel/library/LibraryScanner.kt`, `app/src/test/java/ca/stewark/nocturnel/data/CatalogRepositoryProgressTest.kt`

**Test first:**

```kotlin
@Test fun repositoryForwardsDiscoveryAndIndexingProgress() = runTest {
    // Use a scanner fake that emits Discovering then Indexing(1, 2).
    assertEquals(listOf(ScanProgress.Discovering, ScanProgress.Indexing(1, 2)), progress)
}
```

**Implementation:**

Change `CatalogRepository.rescan` to accept and forward `(ScanProgress) -> Unit`. Extract the shared scan-and-reconcile operation so it can scan either the stored source or a newly selected source URI without first replacing database state. Preserve existing access-lost and cancelled exceptions and the atomic completed-scan database write for the stored-source path.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.data.CatalogRepositoryProgressTest`. The repository forwards both scan stages unchanged.

---

### Task 3: Atomically commit a successful replacement source and catalog

**Files:** `app/src/main/java/ca/stewark/nocturnel/data/NocturneLDatabase.kt`, `app/src/main/java/ca/stewark/nocturnel/data/dao/LibraryDao.kt`, `app/src/main/java/ca/stewark/nocturnel/data/CatalogRepository.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/data/NocturneLDatabaseTest.kt`

**Test first:**

```kotlin
@Test fun failedReplacementDoesNotReplaceStoredSourceOrExistingCatalog() = runTest {
    // Seed the old source and album, attempt a replacement that throws, then assert both remain.
}

@Test fun completedReplacementReplacesSourceCatalogAndListeningDataAtomically() = runTest {
    // Assert the new source/catalog are visible and listening data is cleared only after commit.
}
```

**Implementation:**

Add one database transaction that, for a successful replacement scan, clears the old catalog and listening data, stores the new `LibrarySourceEntity`, and writes the new completed scan data. Have `CatalogRepository` scan a replacement URI first and call this transaction only after a completed result. Do not call the current source-selection transaction before the scan; leave persisted URI access intact but retain the old selected source/catalog if scan fails or is cancelled.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.data.NocturneLDatabaseTest`. Both replacement scenarios pass.

---

### Task 4: Model scan phase and start scanning after any accepted folder selection

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModelTest.kt`

**Test first:**

```kotlin
@Test fun initialFolderSelectionStartsOneScan() = runTest {
    // Select a URI and assert the scanner starts exactly once and state begins at Discovering.
}

@Test fun confirmedReplacementStartsOneScanAndKeepsOldSourceUntilSuccess() = runTest {
    // Confirm a different URI and assert replacement scanning begins without publishing it as source.
}

@Test fun cancellingInitialScanReturnsToSetupState() = runTest {
    // Cancel before completion and assert no source is selected.
}
```

**Implementation:**

Replace the `running/progress` pair in `RescanUiState` with explicit idle, discovering, indexing, and finished phase data that carries completed and total counts only for indexing. Route manual rescan, initial `selectFolder`, and `confirmSourceChange` through one guarded start method. Keep one active `scanJob`; ignore duplicate starts. For initial selection, publish the selected source only after a successful scan. For replacement, retain the old source/catalog until the repository commits the replacement. Map scanner cancellation to the persistent cancellation notice and preserve the old catalog.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.library.LibrarySourceViewModelTest`. The automatic-start, replacement, and cancellation tests pass.

---

### Task 5: Render initial and in-library scan progress with cancellation

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/LibraryScanStatus.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySetupScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/LibraryScreenTest.kt`

**Test first:**

```kotlin
@Test fun initialDiscoveryShowsStatusAndCancel() { /* assert DISCOVERING FILES and CANCEL */ }

@Test fun indexingShowsCompletedTotalAndDeterminateProgress() { /* assert INDEXING 4 OF 10 */ }

@Test fun activeScanDisablesRescanAndCancelInvokesCallback() { /* assert disabled rescan and one cancel */ }
```

**Implementation:**

Create `LibraryScanStatus`, a terminal-styled composable that shows `DISCOVERING FILES...` for discovery and a Material 3 determinate progress indicator plus `INDEXING <completed> OF <total> FILES` for indexing. Include a `CANCEL` button. In `NocturneLApp`, render this state instead of `LibrarySetupScreen` during an initial scan, and render it above the existing library content during later scans. Pass `onCancelRescan` through `SettingsScreen`; keep its Rescan action disabled while active.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.ui.LibraryScreenTest`. The initial and later-scan UI tests pass.

---

### Task 6: Add the reusable transient-notice lifecycle

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/components/TransientNotice.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/components/TransientNoticeStateTest.kt`

**Test first:**

```kotlin
@Test fun successNoticeExpiresAfterFiveSeconds() = runTest { /* advanceTimeBy(5_000) and assert null */ }

@Test fun replacementSuccessRestartsTheFiveSecondTimer() = runTest { /* publish twice and assert latest remains */ }

@Test fun errorAndCancellationNoticesDoNotExpire() = runTest { /* advance time and assert present */ }
```

**Implementation:**

Add a UI-layer notice state holder with text, `NoticeSeverity`, and `transient` flag. Its publish method cancels any prior expiry job, schedules a 5,000 ms clear only for routine success (`INFO`) notices, and leaves warnings/errors and scan cancellation persistent. Expose a read-only state and an explicit replacement/clear operation; use an injectable coroutine scope or dispatcher so timer tests are deterministic.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.components.TransientNoticeStateTest`. All expiry and replacement cases pass under virtual time.

---

### Task 7: Apply transient notices to scan, playlist, and listening outcomes

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/library/LibrarySourceViewModel.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModel.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/listening/ListeningViewModel.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playlist/PlaylistViewModelNoticeTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/listening/ListeningViewModelNoticeTest.kt`

**Test first:**

```kotlin
@Test fun successfulPlaylistImportPublishesAnExpiringInfoNotice() = runTest { /* assert transient info */ }

@Test fun playlistImportFailurePublishesAPersistentErrorNotice() = runTest { /* assert non-transient error */ }

@Test fun successfulHistoryClearPublishesAnExpiringInfoNotice() = runTest { /* assert transient info */ }
```

**Implementation:**

Replace each view model's permanent success-string field with the shared notice state. Classify successful rescans, playlist create/rename/delete/import/export, and history-clear outcomes as transient info. Classify source access failures, playlist transfer failures, listening failures, and scan cancellations as persistent warning/error notices. Have `NocturneLApp` pass the current notice to `TerminalScaffold`; remove duplicated playlist and settings notice rendering so each action result has one display location. Do not convert static empty-state or confirmation text into transient notices.

**Verify:** Run `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.playlist.PlaylistViewModelNoticeTest --tests ca.stewark.nocturnel.ui.listening.ListeningViewModelNoticeTest`. Success and failure classifications pass.

---

### Task 8: Update notice and scan UI regression coverage

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistIndexScreenTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/LibraryScreenTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/TerminalUiSourceGuardTest.kt`

**Test first:**

```kotlin
@Test fun playlistIndexDoesNotRenderItsOwnPermanentTransferNotice() { /* assert notice is absent from index */ }

@Test fun terminalScaffoldRendersTheCurrentSharedNotice() { /* assert one shared status location */ }
```

**Implementation:**

Replace the existing playlist test that asserts a permanent in-screen notice with coverage for shared scaffold rendering. Extend existing UI/source-guard expectations only where necessary to protect the new scan status and prevent reintroducing the old permanent playlist notice. Keep all unaffected static notices unchanged.

**Verify:** Run `./gradlew.bat connectedDebugAndroidTest --instrumentation-arg class=ca.stewark.nocturnel.ui.playlist.PlaylistIndexScreenTest --instrumentation-arg class=ca.stewark.nocturnel.ui.LibraryScreenTest` and `./gradlew.bat testDebugUnitTest --tests ca.stewark.nocturnel.ui.TerminalUiSourceGuardTest`.

---

### Task 9: Run the full suite and conduct the release-path manual checks

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

```text
No additional automated test; this task runs the complete suite after the preceding tests are added.
```

**Implementation:**

Add a manual-check section for: initial folder selection immediately entering discovery/indexing; cancelling initial discovery and indexing; changing a populated library folder then cancelling/failing without losing the old catalog; a successful replacement clearing the old library/listening state; and ordinary success notices clearing after five seconds while errors and cancellation remain visible.

**Verify:** Run `./gradlew.bat testDebugUnitTest`, `./gradlew.bat connectedDebugAndroidTest`, and the relevant screenshot-test command used by this project. Install the debug build and complete the added manual checks with a large local library.

## Definition of Done

- [ ] All tasks completed in order
- [ ] All tests pass (`./gradlew.bat testDebugUnitTest`, `./gradlew.bat connectedDebugAndroidTest`, and relevant screenshot tests)
- [ ] No unplanned files modified
- [ ] Folder selection starts a scan, scan progress/cancellation is accurate, replacement scans preserve prior data until success, and routine success notices expire after five seconds as described in the design doc
