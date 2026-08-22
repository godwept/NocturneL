# Portrait Orientation Lock Implementation Plan

**Date:** 2026-08-21  
**Design doc:** docs/specs/2026-08-21-portrait-orientation-lock-design.md  
**Status:** Ready for review

## Overview

Request portrait orientation for NocturneL's single launcher activity using the standard Android manifest attribute. A focused manifest policy test will protect the declaration, while Android 16's default large-screen behavior will remain unchanged because no restricted-resizability compatibility property will be added.

## Tasks

### Task 1: Add a failing portrait-orientation manifest policy test

**Files:** `app/src/test/java/ca/stewark/nocturnel/ReleaseManifestPolicyTest.kt`

**Test first:**

Add this test to `ReleaseManifestPolicyTest` without changing the manifest yet:

```kotlin
@Test fun mainActivityIsRestrictedToPortraitOrientation() {
    assertTrue("android:screenOrientation=\"portrait\"" in manifest)
}
```

This assertion follows the existing source-level manifest policy test style and verifies only the approved orientation declaration.

**Implementation:**

No production implementation belongs in this task. Run the focused test and confirm that the new assertion fails because `MainActivity` does not yet declare `android:screenOrientation="portrait"`.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ReleaseManifestPolicyTest"` from the repository root. The new test must fail for the missing portrait declaration while the existing backup and internet-permission policy test remains unchanged.

---

### Task 2: Declare portrait orientation on MainActivity

**Files:** `app/src/main/AndroidManifest.xml`

**Test first:**

Use the failing `mainActivityIsRestrictedToPortraitOrientation` test from Task 1. Do not add runtime orientation code or a large-screen compatibility property.

**Implementation:**

Change the existing `MainActivity` declaration to include the portrait orientation attribute:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:screenOrientation="portrait">
```

Leave the intent filter and every other manifest element unchanged.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ReleaseManifestPolicyTest"` from the repository root. Both tests in `ReleaseManifestPolicyTest` must pass.

---

### Task 3: Run regression and scope checks

**Files:** `docs/specs/2026-08-21-portrait-orientation-lock-design.md`, `docs/plans/2026-08-21-portrait-orientation-lock.md`, `app/src/test/java/ca/stewark/nocturnel/ReleaseManifestPolicyTest.kt`, `app/src/main/AndroidManifest.xml`

**Test first:**

No additional test is required. The focused policy test added in Task 1 covers the only production behavior introduced by this change.

**Implementation:**

Do not make further production changes. Inspect the final diff and confirm there is no `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`, runtime `requestedOrientation` assignment, orientation preference, or unrelated edit.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest` from the repository root and require the complete unit test suite to pass. Then run `git status --short` and `git diff --check`; confirm that only the approved design document, this plan, the manifest policy test, and the manifest are changed and that the diff has no whitespace errors.

## Definition of Done

- [ ] All tasks completed in order
- [ ] The portrait policy test was observed failing before the manifest change
- [ ] The focused manifest policy tests pass
- [ ] The complete unit test suite passes (`./gradlew.bat :app:testDebugUnitTest`)
- [ ] No unplanned files modified
- [ ] No Android 16 large-screen compatibility opt-out added
- [ ] Feature behaves as described in the design doc
