# NocturneL Play Store Production Release Implementation Plan

**Date:** 2026-08-20  
**Design doc:** docs/specs/2026-08-20-play-store-production-release-design.md  
**Status:** Ready for review

## Overview

Prepare NocturneL for a paid public Google Play release without adding online runtime behavior. The implementation lowers the supported floor to API 31, hardens and validates the release variant, adds the required in-app privacy-policy link, publishes an accurate GitHub Pages policy, produces policy-compliant listing assets, and adds a tag-triggered CI workflow that tests and signs an App Bundle for manual Play Console upload. External Play Console work remains manual and is captured as short, verifiable checklist tasks.

## Fixed Release Decisions

- App name: NocturneL
- Package ID: ca.stewark.nocturnel
- Version for the initial candidate: versionName 0.1.0, versionCode 1
- Minimum SDK: API 31
- Compile and target SDK: API 36
- Price: CAD $1.99 paid download
- Markets: Canada, United States, United Kingdom, Ireland, Australia, New Zealand
- Language: English
- Audience: ages 13 and older; not directed to children
- Runtime privacy: offline, no ads, accounts, analytics, telemetry, remote crash reporting, or user-data transmission
- Privacy URL: https://godwept.github.io/NocturneL/privacy/
- Support email: mathew.stewart@gmail.com
- Delivery: CI builds and signs; the developer uploads and promotes manually

## Tasks

### Task 1: Lock the Android release SDK contract

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseConfigurationTest.kt, app/build.gradle.kts

**Test first:**

Create ReleaseConfigurationTest.kt. Read build.gradle.kts and assert that it contains compileSdk = 36, minSdk = 31, targetSdk = 36, versionCode = 1, and versionName = "0.1.0". Run the focused test and confirm it fails because minSdk is currently 33.

**Implementation:**

Change only minSdk from 33 to 31. Keep compileSdk, targetSdk, application ID, version name, and version code unchanged.

**Verify:** Run ./gradlew testDebugUnitTest --tests ca.stewark.nocturnel.ReleaseConfigurationTest. The focused test passes.

---

### Task 2: Enable optimized release builds

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseConfigurationTest.kt, app/build.gradle.kts, app/proguard-rules.pro

**Test first:**

Add a test that requires a release build type with isMinifyEnabled = true, isShrinkResources = true, and the default optimized ProGuard file plus app/proguard-rules.pro. Confirm the focused test fails.

**Implementation:**

Add the release build type:

~~~kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }
}
~~~

Do not add speculative keep rules. Add rules only if the release build or runtime tests demonstrate a concrete need.

**Verify:** Run ./gradlew testDebugUnitTest --tests ca.stewark.nocturnel.ReleaseConfigurationTest and ./gradlew bundleRelease. Both pass and app/build/outputs/mapping/release/mapping.txt exists.

---

### Task 3: Enforce the local-only manifest policy

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseManifestPolicyTest.kt, app/src/main/AndroidManifest.xml

**Test first:**

Create ReleaseManifestPolicyTest.kt. Read src/main/AndroidManifest.xml and assert android:allowBackup="false" is present and android.permission.INTERNET is absent. Confirm the backup assertion fails.

**Implementation:**

Change android:allowBackup from true to false. Do not add INTERNET, ACCESS_NETWORK_STATE, data extraction rules, or unrelated permissions.

**Verify:** Run ./gradlew testDebugUnitTest --tests ca.stewark.nocturnel.ReleaseManifestPolicyTest. It passes.

---

### Task 4: Specify notification-permission behavior by SDK

**Files:** app/src/test/java/ca/stewark/nocturnel/NotificationPermissionPolicyTest.kt, app/src/main/java/ca/stewark/nocturnel/NotificationPermissionPolicy.kt

**Test first:**

Write tests for a pure shouldRequest(sdkInt, alreadyGranted) function:

~~~kotlin
assertFalse(NotificationPermissionPolicy.shouldRequest(31, false))
assertFalse(NotificationPermissionPolicy.shouldRequest(32, false))
assertTrue(NotificationPermissionPolicy.shouldRequest(33, false))
assertTrue(NotificationPermissionPolicy.shouldRequest(36, false))
assertFalse(NotificationPermissionPolicy.shouldRequest(36, true))
~~~

Confirm the test fails because the policy does not exist.

**Implementation:**

Create an internal NotificationPermissionPolicy object. Return true only when sdkInt is at least Build.VERSION_CODES.TIRAMISU and alreadyGranted is false.

**Verify:** Run ./gradlew testDebugUnitTest --tests ca.stewark.nocturnel.NotificationPermissionPolicyTest. It passes.

---

### Task 5: Gate the activity permission request

**Files:** app/src/test/java/ca/stewark/nocturnel/MainActivityTest.kt, app/src/main/java/ca/stewark/nocturnel/MainActivity.kt

**Test first:**

Extend MainActivityTest with a source-wiring assertion that MainActivity calls NotificationPermissionPolicy.shouldRequest with Build.VERSION.SDK_INT and the current permission result. Confirm it fails.

**Implementation:**

Calculate whether POST_NOTIFICATIONS is already granted, call the policy, and launch the permission contract only when the policy returns true. Preserve the existing Compose setup.

**Verify:** Run the MainActivityTest and NotificationPermissionPolicyTest classes together. Both pass.

---

### Task 6: Add a tested privacy-policy control to Settings

**Files:** app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsScreenTest.kt, app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsScreen.kt

**Test first:**

Create a Compose test that renders SettingsScreen with an onOpenPrivacyPolicy callback, finds [ PRIVACY POLICY ], clicks it, and asserts the callback ran. Confirm it fails before the parameter and button exist.

**Implementation:**

Add onOpenPrivacyPolicy: () -> Unit = {} to SettingsScreen and add BracketButton("PRIVACY POLICY", onOpenPrivacyPolicy) after the CRT effects control. Keep the default so existing previews and callers continue compiling.

**Verify:** Run ./gradlew connectedDebugAndroidTest with the test class filter for SettingsScreenTest on an emulator. The test passes.

---

### Task 7: Wire the public privacy URL

**Files:** app/src/test/java/ca/stewark/nocturnel/ui/PrivacyPolicyWiringTest.kt, app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt

**Test first:**

Create PrivacyPolicyWiringTest.kt as a source guard. Assert NocturneLApp.kt defines the exact HTTPS URL https://godwept.github.io/NocturneL/privacy/, obtains LocalUriHandler.current, and passes an onOpenPrivacyPolicy callback that opens that URL. Confirm it fails.

**Implementation:**

Add a private privacy-policy URL constant, obtain LocalUriHandler.current inside NocturneLApp, and pass a callback to SettingsScreen that calls openUri. This delegates browsing to the user's browser and does not require INTERNET permission in NocturneL.

**Verify:** Run PrivacyPolicyWiringTest and the full debug unit-test suite. Both pass.

---

### Task 8: Refresh the Settings screenshot baseline

**Files:** app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/SettingsPreview_*.png

**Test first:**

Run ./gradlew validateDebugScreenshotTest and confirm the Settings preview differs because of the new policy button. Review the generated diff to ensure no unrelated layout changed.

**Implementation:**

Record only the updated Settings preview reference using the repository's screenshot recording task. Do not accept changes to unrelated preview images.

**Verify:** Run ./gradlew validateDebugScreenshotTest. Every screenshot passes.

---

### Task 9: Add a privacy-policy content contract

**Files:** app/src/test/java/ca/stewark/nocturnel/PlayStoreMetadataTest.kt, docs/privacy/index.md

**Test first:**

Create PlayStoreMetadataTest.kt. Read ../docs/privacy/index.md and assert it identifies NocturneL, contains mathew.stewart@gmail.com, states that data is not collected or shared, explains local media-folder access, retention/deletion, disabled backup, and the 13+ audience. Confirm it fails because the page is absent.

**Implementation:**

Write a clearly titled Privacy Policy with:

- Last updated date
- NocturneL identification and support contact
- Local access to user-selected audio files, metadata, and artwork
- No collection, sharing, sale, advertising, analytics, accounts, or network transmission
- Local storage of catalog, playlists, history, settings, and persisted folder permission
- Retention until the user clears data or uninstalls
- User-controlled playlist export/import
- Disabled Android cloud backup
- General 13+ audience statement
- Policy-change and contact sections

Do not claim that Google Play itself collects no purchase/account data; state that NocturneL does not receive or process Play payment details.

**Verify:** Run ./gradlew testDebugUnitTest --tests ca.stewark.nocturnel.PlayStoreMetadataTest.privacyPolicyIsComplete. It passes.

---

### Task 10: Prepare GitHub Pages routing

**Files:** docs/_config.yml, docs/index.md, docs/privacy/index.md

**Test first:**

Extend PlayStoreMetadataTest to assert docs/_config.yml and docs/index.md exist and the landing page links to /NocturneL/privacy/. Confirm it fails.

**Implementation:**

Add a minimal Jekyll configuration naming the site NocturneL and a docs landing page linking to the privacy policy. Keep plans and specifications unchanged.

**Verify:** Run the focused metadata test. After pushing, enable GitHub Pages from the main branch /docs folder and open https://godwept.github.io/NocturneL/privacy/ in a signed-out browser. It must return the rendered policy without authentication or geographic restriction.

---

### Task 11: Add exact English store-listing copy

**Files:** app/src/test/java/ca/stewark/nocturnel/PlayStoreMetadataTest.kt, docs/play-store/listing/en-US.md

**Test first:**

Add assertions for app title NocturneL, a non-empty short description no longer than 80 characters, a non-empty full description no longer than 4,000 characters, and initial release notes. Confirm the test fails.

**Implementation:**

Write these exact listing fields:

- Title: NocturneL
- Short description: An offline, terminal-themed music player for your local library
- Full description:

~~~text
NocturneL is an offline music player for the audio files you already own.

Choose a folder on your device, scan your library, and browse albums and artists through a focused terminal-inspired interface.

Features:
• Local folder access through Android's system picker
• Album, artist, search, favorites, and listening history views
• Editable playback queue and gapless playback support
• Local playlists with M3U import/export and ZIP backup
• Album artwork and responsive terminal visualizers
• Background playback with media and lock-screen controls

NocturneL has no ads, accounts, analytics, or internet access. Your library information, playlists, history, and settings remain on your device.
~~~

- Release notes: Initial release of NocturneL, an offline terminal-themed player for local music libraries.

**Verify:** Run the focused listing-copy test. It passes.

---

### Task 12: Document Play declarations, pricing, and markets

**Files:** app/src/test/java/ca/stewark/nocturnel/PlayStoreMetadataTest.kt, docs/play-store/declarations.md

**Test first:**

Add assertions that declarations.md records CAD 1.99; CA, US, GB, IE, AU, and NZ; Music & Audio; ages 13–15, 16–17, and 18+; no ads; no app account; unrestricted app access; no collected/shared data; and the approved privacy URL and support email. Confirm it fails.

**Implementation:**

Create the declaration worksheet with those exact answers. Add reminders to answer the live content-rating questionnaire truthfully, declare the foreground media and notification permissions, and re-audit all transitive SDKs before changing the Data safety answer.

**Verify:** Run PlayStoreMetadataTest. All metadata tests pass.

---

### Task 13: Add automated Play asset validation

**Files:** app/src/test/java/ca/stewark/nocturnel/PlayStoreAssetsTest.kt, docs/play-store/listing/graphics/README.md

**Test first:**

Create image tests using ImageIO for these exact paths:

- icon.png: 512x512, PNG with alpha, at most 1,024 KB
- feature-graphic.png: 1024x500, PNG without alpha
- phone/01-library.png through 04-queue.png: 1080x1920, PNG without alpha
- tablet/01-library.png through 04-queue.png: 1920x1080, PNG without alpha

Give each asset its own test method so assets can be completed independently. Confirm the focused tests report the missing files.

**Implementation:**

Create README.md listing the file contract, capture subject, and alt text under 140 characters for every image. State that screenshots must contain only rights-cleared music metadata/artwork, no device frame, no promotional price, and no unrelated notifications.

**Verify:** Run the README/contract test only; it passes while individual missing-image tests remain intentionally red until Tasks 14–23.

---

### Task 14: Export the Play Store icon

**Files:** docs/assets/nocturnel-icon-source.png, docs/play-store/listing/graphics/icon.png

**Test first:**

Run PlayStoreAssetsTest.storeIcon and confirm it reports a missing icon.

**Implementation:**

Export the existing NocturneL icon at 512x512 as a 32-bit PNG with an alpha channel. Preserve the artwork and safe area; add no badges, price, or store branding.

**Verify:** Run PlayStoreAssetsTest.storeIcon. It passes.

---

### Task 15: Create the feature graphic

**Files:** docs/assets/nocturnel-icon-source.png, docs/play-store/listing/graphics/feature-graphic.png

**Test first:**

Run PlayStoreAssetsTest.featureGraphic and confirm it reports a missing image.

**Implementation:**

Use the existing icon as the visual reference for a 1024x500 terminal-themed graphic. Keep the focal content centered, avoid tiny detail, device imagery, price, ranking claims, calls to action, and Play branding. Export as an opaque 24-bit PNG.

**Verify:** Run PlayStoreAssetsTest.featureGraphic and visually inspect the result at thumbnail size. Both checks pass.

---

### Task 16: Capture the phone Library screenshot

**Files:** docs/play-store/listing/graphics/phone/01-library.png

**Test first:**

Run PlayStoreAssetsTest.phoneLibrary and confirm it reports the missing file.

**Implementation:**

Capture the populated Library landing screen from a 1080x1920 API 36 phone emulator using rights-cleared fixture metadata. Remove unrelated status-bar notifications and export an opaque PNG without framing or overlays.

**Verify:** Run PlayStoreAssetsTest.phoneLibrary and compare the image to the current release UI.

---

### Task 17: Capture the phone Album screenshot

**Files:** docs/play-store/listing/graphics/phone/02-album.png

**Test first:** Run PlayStoreAssetsTest.phoneAlbum and confirm it fails for the missing file.

**Implementation:** Capture a populated album detail view at 1080x1920 with rights-cleared metadata and artwork.

**Verify:** Run PlayStoreAssetsTest.phoneAlbum and visually verify track titles and controls are legible.

---

### Task 18: Capture the phone Now Playing screenshot

**Files:** docs/play-store/listing/graphics/phone/03-now-playing.png

**Test first:** Run PlayStoreAssetsTest.phoneNowPlaying and confirm it fails for the missing file.

**Implementation:** Capture Now Playing at 1080x1920 during local playback, showing the terminal visualizer or rights-cleared artwork without copyrighted third-party imagery.

**Verify:** Run PlayStoreAssetsTest.phoneNowPlaying and visually verify that playback state is coherent.

---

### Task 19: Capture the phone Queue screenshot

**Files:** docs/play-store/listing/graphics/phone/04-queue.png

**Test first:** Run PlayStoreAssetsTest.phoneQueue and confirm it fails for the missing file.

**Implementation:** Capture a populated editable queue at 1080x1920 with clear current and upcoming tracks.

**Verify:** Run PlayStoreAssetsTest.phoneQueue and visually verify the queue is readable and not clipped.

---

### Task 20: Capture the tablet Library screenshot

**Files:** docs/play-store/listing/graphics/tablet/01-library.png

**Test first:** Run PlayStoreAssetsTest.tabletLibrary and confirm it fails for the missing file.

**Implementation:** Capture the populated Library on a tablet emulator at 1920x1080 landscape using the same rights-cleared fixture content.

**Verify:** Run PlayStoreAssetsTest.tabletLibrary and inspect for stretching, clipping, or unusable touch targets.

---

### Task 21: Capture the tablet Album screenshot

**Files:** docs/play-store/listing/graphics/tablet/02-album.png

**Test first:** Run PlayStoreAssetsTest.tabletAlbum and confirm it fails for the missing file.

**Implementation:** Capture album detail on the tablet emulator at 1920x1080.

**Verify:** Run PlayStoreAssetsTest.tabletAlbum and inspect the large-screen layout.

---

### Task 22: Capture the tablet Now Playing screenshot

**Files:** docs/play-store/listing/graphics/tablet/03-now-playing.png

**Test first:** Run PlayStoreAssetsTest.tabletNowPlaying and confirm it fails for the missing file.

**Implementation:** Capture Now Playing on the tablet emulator at 1920x1080 with rights-cleared content.

**Verify:** Run PlayStoreAssetsTest.tabletNowPlaying and inspect the visualizer/artwork bounds.

---

### Task 23: Capture the tablet Queue screenshot

**Files:** docs/play-store/listing/graphics/tablet/04-queue.png

**Test first:** Run PlayStoreAssetsTest.tabletQueue and confirm it fails for the missing file.

**Implementation:** Capture the populated queue on the tablet emulator at 1920x1080.

**Verify:** Run the complete PlayStoreAssetsTest class. Every image test passes.

---

### Task 24: Expose deterministic release version metadata

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseConfigurationTest.kt, app/build.gradle.kts

**Test first:**

Add a source assertion that build.gradle.kts registers printReleaseVersion and prints versionName and versionCode separated by a single space. Confirm it fails.

**Implementation:**

Hoist the two release version values into local constants used by defaultConfig. Register printReleaseVersion so ./gradlew -q :app:printReleaseVersion outputs exactly:

~~~text
0.1.0 1
~~~

Do not derive the version code from a CI run number.

**Verify:** Run ./gradlew -q :app:printReleaseVersion and the focused ReleaseConfigurationTest.

---

### Task 25: Add release checks to ordinary CI

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/android.yml

**Test first:**

Create ReleaseWorkflowContractTest.kt. Assert android.yml runs lintRelease and bundleRelease in addition to the existing unit, screenshot, instrumented-compilation, and debug APK steps. Confirm it fails.

**Implementation:**

Add release lint and unsigned release-bundle assembly steps before the debug APK upload. Keep permissions at contents: read and do not reference signing or Play credentials.

**Verify:** Run ReleaseWorkflowContractTest locally where possible, then confirm the Android CI workflow passes on a branch push.

---

### Task 26: Add the protected release workflow skeleton

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/play-release.yml

**Test first:**

Add assertions that play-release.yml:

- Runs only for play/* tags and workflow reruns
- Uses contents: read
- Uses JDK 17 and Gradle setup
- Declares quality, device-tests, and package jobs
- Assigns only the package job to the play-release environment
- Makes package depend on both preceding jobs
- Contains no Play Developer API credential or upload command

Confirm the test fails because the workflow is absent.

**Implementation:**

Create the workflow skeleton with checkout fetch-depth 0. The quality job runs testDebugUnitTest, validateDebugScreenshotTest, lintRelease, assembleDebugAndroidTest, and bundleRelease. Leave signing and packaging for later tasks.

**Verify:** Run ReleaseWorkflowContractTest and validate the YAML syntax with GitHub's workflow parser on push.

---

### Task 27: Add API 31 and API 36 emulator gates

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/play-release.yml

**Test first:**

Require a matrix containing API levels 31 and 36 and a connectedDebugAndroidTest command. Confirm it fails.

**Implementation:**

In device-tests, use a two-entry API matrix with the Android emulator runner, x86_64 Google APIs images, a phone profile, disabled animations, and ./gradlew connectedDebugAndroidTest. Do not allow package to run when either matrix entry fails.

**Verify:** Run the source contract test, push a temporary non-release workflow branch if needed, and confirm both emulator jobs pass before creating a release tag.

---

### Task 28: Enforce release-tag version monotonicity

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/play-release.yml, docs/release/play-store-runbook.md

**Test first:**

Require the workflow to call :app:printReleaseVersion, validate a tag shaped play/0.1.0-1, and reject a version code less than or equal to every other play/* tag. Confirm it fails.

**Implementation:**

Add a preflight step that:

1. Reads versionName and versionCode from printReleaseVersion.
2. Requires GITHUB_REF_NAME to equal play/versionName-versionCode.
3. Lists all other play/* tags, parses their trailing numeric version codes, and fails unless the current code is greater than the maximum.
4. Exposes sanitized version/name outputs to the package job.

Document protecting play/* tags from update or deletion in GitHub repository settings. Rerunning the same workflow run remains allowed.

**Verify:** Add shell-level fixture cases in the workflow contract test or extract the comparison to a checked-in script with unit fixtures. Verify codes 1 after no prior tag passes, 2 after 1 passes, and 1 after 1 or 2 fails.

---

### Task 29: Validate the bundle and merged manifest

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/play-release.yml

**Test first:**

Require bundletool 1.18.3, its SHA-256 a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29, bundletool validate, and bundletool dump manifest. Require checks for package ID, min SDK 31, target SDK 36, allowBackup false, and absence of android.permission.INTERNET. Confirm it fails.

**Implementation:**

Download the official bundletool-all-1.18.3.jar release asset into RUNNER_TEMP, verify its checksum before execution, validate app-release.aab, dump the base manifest, and fail the job when any release contract differs. Copy the dumped manifest into the staging directory for audit.

**Verify:** Run ReleaseWorkflowContractTest and confirm a workflow run produces a successful bundletool validation. Temporarily changing one expected manifest value on a branch must make the audit step fail.

---

### Task 30: Sign and package the release artifacts

**Files:** app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt, .github/workflows/play-release.yml

**Test first:**

Require these environment secrets only in the protected package job:

- NOCTURNEL_UPLOAD_KEYSTORE_BASE64
- NOCTURNEL_UPLOAD_KEY_ALIAS
- NOCTURNEL_UPLOAD_STORE_PASSWORD
- NOCTURNEL_UPLOAD_KEY_PASSWORD

Require explicit non-empty checks, RUNNER_TEMP keystore decoding, jarsigner environment-based password arguments, jarsigner -verify -strict, SHA-256 generation after signing, mapping.txt inclusion, and a 30-day artifact retention. Assert that no .jks or .keystore path appears in upload-artifact. Confirm it fails.

**Implementation:**

Add fail-fast secret checks without echoing values. Decode the keystore only to RUNNER_TEMP, sign a versioned copy named nocturnel-versionName-versionCode.aab with SHA-256/RSA, verify it strictly, generate the checksum, include mapping.txt and the dumped manifest, and upload only those four outputs. Delete the temporary keystore in an always-running cleanup step.

**Verify:** Run the contract test. After secrets are configured, run the workflow and download the artifact; sha256sum --check succeeds and jarsigner -verify -strict succeeds.

---

### Task 31: Document upload-key creation and recovery

**Files:** docs/release/signing.md

**Test first:**

Extend PlayStoreMetadataTest to require signing.md to contain Play App Signing, a separate upload key, RSA 4096, validity 10000 days, all four secret names, offline backup guidance, certificate fingerprint export, and the upload-key reset procedure. Confirm it fails.

**Implementation:**

Document:

1. Generate nocturnel-upload.jks outside the repository with keytool -genkeypair, alias nocturnel-upload, RSA 4096, and 10,000-day validity.
2. Store an offline encrypted backup and passwords separately.
3. Export the public certificate and record SHA-256.
4. Base64-encode the keystore for the GitHub environment secret.
5. Add the alias and two passwords as separate secrets.
6. Never commit the keystore or paste secrets into workflow YAML or logs.
7. Use Google Play's upload-key reset process if compromised or lost.

**Verify:** Run the signing documentation test and confirm git check-ignore reports the chosen local .jks path as ignored.

---

### Task 32: Write the manual release and recovery runbook

**Files:** app/src/test/java/ca/stewark/nocturnel/PlayStoreMetadataTest.kt, docs/release/play-store-runbook.md

**Test first:**

Require the runbook to cover version bump, release tag, CI gates, checksum verification, internal upload, pre-launch report, promotion, closed testing, production application, production promotion, halt, corrective higher-version release, and debug-signature uninstall guidance. Confirm it fails.

**Implementation:**

Write the commands and Play Console navigation for each stage. State that the same approved bundle should be promoted between tracks, not rebuilt. State that rollback means halting and publishing a higher version, never decrementing versionCode.

**Verify:** Run the metadata test and dry-run the runbook through artifact download without performing a Play upload.

---

### Task 33: Convert the device checklist to Play release testing

**Files:** docs/testing/pixel-7-release-checklist.md, docs/testing/android-12-release-checklist.md

**Test first:**

Add metadata assertions requiring both checklists to mention Play/internal installation, fresh install, upgrade, airplane mode, background and lock-screen playback, notification behavior, audio focus, folder permissions, scanning, playlists, process death, restart, and privacy-link opening. Confirm they fail.

**Implementation:**

Update the Pixel 7 checklist to use the internal Play build instead of the debug APK. Add a focused Android 12/API 31 checklist covering the same minimum-SDK risks and confirming no Android 13 notification-permission prompt appears.

**Verify:** Run the metadata test and execute both checklists against the candidate before closed testing.

---

### Task 34: Add a privacy and dependency audit checklist

**Files:** docs/release/privacy-audit.md

**Test first:**

Require privacy-audit.md to list runtime dependencies, merged permissions, exported components, backup state, Data safety consistency, privacy URL accessibility, and airplane-mode functionality. Confirm it fails.

**Implementation:**

Create a release-by-release audit with checkboxes. Record the current dependency families—AndroidX, Compose, Room, Media3, Coil, DocumentFile, and Kotlin serialization—and require re-review whenever dependencies change. Do not claim that dependencies are safe without inspecting the shipped bundle.

**Verify:** Complete the checklist against the signed candidate and attach the dumped manifest path to the release notes.

---

### Task 35: Create the closed-test feedback kit

**Files:** docs/release/closed-test-guide.md, docs/release/closed-test-feedback-template.md

**Test first:**

Require the guide to state 12 testers, 14 continuous days, a recommended pool of 15–20, Google-account opt-in, paid-app promo-code redemption, feature checklist, support email, no committed tester email addresses, and feedback/change summaries. Confirm it fails.

**Implementation:**

Create a one-page tester guide and a feedback template with pseudonymous tester ID, device/API, tested workflows, issues, usability notes, and follow-up version. Store actual tester emails only in Play Console or another private location.

**Verify:** Send the guide to one internal tester and confirm they can opt in, redeem a code, install, and submit feedback without extra explanation.

---

### Task 36: Run the complete repository release gate

**Files:** no new files

**Test first:**

Record the expected command set before running it:

~~~text
./gradlew testDebugUnitTest
./gradlew validateDebugScreenshotTest
./gradlew lintRelease
./gradlew assembleDebugAndroidTest
./gradlew bundleRelease
~~~

**Implementation:**

Run the commands in CI with JDK 17 and Android tooling. Fix only failures caused by this release work. Do not modify unrelated features or silently accept screenshot changes.

**Verify:** All commands pass, the worktree contains only planned files, and the unsigned local release bundle is generated.

---

### Task 37: Create and verify the personal Play Console account

**Files:** docs/release/play-store-runbook.md

**Test first:**

Open the account-setup section of the runbook and confirm the required evidence fields are blank: account type, identity verification, Android-device verification, public developer contact, and payments-profile status.

**Implementation:**

Create a personal Play Console account, complete identity and device verification, and record completion dates without committing identity documents, legal address, phone number, banking details, or tax data.

**Verify:** The Play Console dashboard shows the personal account as verified and able to create an app.

---

### Task 38: Configure the merchant payments profile

**Files:** docs/release/play-store-runbook.md

**Test first:**

Confirm the runbook marks paid-app merchant setup, tax information, payout method, and CAD base currency as incomplete.

**Implementation:**

Complete the Play merchant/payments prompts using private information directly in Play Console. Do not place financial information in the repository.

**Verify:** Play Console allows creation of a paid app and accepts a CAD price.

---

### Task 39: Create the permanent Play app record

**Files:** docs/release/play-store-runbook.md

**Test first:**

Verify the package ID, title, default language, app/game classification, paid status, audience, and support email against the Fixed Release Decisions section.

**Implementation:**

Create NocturneL as an English app, mark it paid before any public availability, use ca.stewark.nocturnel, accept Play App Signing with a Google-managed app-signing key, and retain the separate CI upload key.

**Verify:** App integrity shows Play App Signing enabled, the package ID is exact, and App pricing shows paid rather than free.

---

### Task 40: Complete the store listing and declarations

**Files:** docs/play-store/listing/en-US.md, docs/play-store/declarations.md, docs/play-store/listing/graphics/

**Test first:**

Run PlayStoreMetadataTest and PlayStoreAssetsTest, then compare every prepared answer and asset with the live Play form before saving.

**Implementation:**

Enter the approved copy, upload the icon, feature graphic, phone screenshots, and tablet screenshots, add alt text, set Music & Audio, select the 13+ age bands, declare no ads/accounts/data collection or sharing, add the policy URL and support email, complete content rating, and enable only CA, US, GB, IE, AU, and NZ at the CAD $1.99 base price/local equivalents.

**Verify:** Play Console reports no incomplete required listing or App content tasks, and the preview matches the repository source material.

---

### Task 41: Configure protected CI signing

**Files:** .github/workflows/play-release.yml, docs/release/signing.md

**Test first:**

Before adding secrets, trigger no release tag and confirm ordinary CI cannot access the play-release environment. Review the workflow to ensure only package uses that environment.

**Implementation:**

Create the play-release GitHub environment, require manual approval if the repository plan supports it, add the four encrypted secrets, and configure play/* tag protection. Do not add a Play service account.

**Verify:** Run the extracted version-validation fixtures from Task 28 and confirm ordinary branch CI still has no access to release secrets. Use only the real release tag in Task 42.

---

### Task 42: Produce the first signed candidate

**Files:** app/build.gradle.kts, .github/workflows/play-release.yml

**Test first:**

Confirm versionName is 0.1.0, versionCode is 1, the worktree is clean, ordinary CI is green, all asset tests pass, and no existing play/* tag has code 1 or higher.

**Implementation:**

Create and push the protected tag play/0.1.0-1. Let CI build, test, validate, and sign. Download the resulting release artifact.

**Verify:** Check the SHA-256 file, strict JAR signature, mapping file, dumped manifest, exact Git SHA, API 31/36 emulator results, and absence of the keystore from artifacts and logs.

---

### Task 43: Upload and test on the internal track

**Files:** docs/release/play-store-runbook.md, docs/testing/pixel-7-release-checklist.md, docs/testing/android-12-release-checklist.md

**Test first:**

Confirm the signed candidate checksum matches the CI record and all Play setup fields are complete.

**Implementation:**

Upload the signed AAB to Internal testing, finish Play App Signing enrollment, wait for Play processing, install from the Play opt-in link, and run both device checklists. Review every pre-launch report finding.

**Verify:** Internal installation succeeds, purchases are not charged to internal testers, both checklists pass, and all material pre-launch findings are resolved or documented.

---

### Task 44: Start the paid closed test

**Files:** docs/release/closed-test-guide.md, docs/release/play-store-runbook.md

**Test first:**

Confirm at least 15 willing testers are available, at least 12 use eligible Google accounts, all target countries are enabled, promo codes redeem successfully, the feedback channel works, and the candidate passed internal testing.

**Implementation:**

Promote the approved internal build to the closed track, create and distribute paid-app promo codes, send the opt-in link and guide, and record the start time only after at least 12 testers are opted in.

**Verify:** Play Console shows at least 12 opted-in testers and no one had to pay. Check the count daily without committing tester identities.

---

### Task 45: Complete and summarize the 14-day closed test

**Files:** docs/release/closed-test-feedback-template.md, docs/release/play-store-runbook.md

**Test first:**

Before applying, verify that at least 12 testers have remained continuously opted in for the preceding 14 days and that feedback covers the primary workflows.

**Implementation:**

Summarize tester engagement, devices/API levels, feature coverage, feedback, bugs, and changes. If a material fix is required, increment versionCode, repeat the release gates, promote the corrected build, and update the summary.

**Verify:** Play Console enables Apply for production and the written answers are supported by retained, non-PII test evidence.

---

### Task 46: Apply for production access

**Files:** docs/release/play-store-runbook.md

**Test first:**

Review the application answers against the closed-test summary, current app behavior, Data safety form, and production-readiness gates. Confirm there are no contradictions.

**Implementation:**

Submit the production-access application with accurate descriptions of recruitment, engagement, feedback, resulting changes, target audience, value proposition, expected installs, and readiness.

**Verify:** Record the submission date and Play decision. If more testing is requested, follow the stated finding instead of submitting unchanged answers.

---

### Task 47: Publish the first production release

**Files:** docs/release/play-store-runbook.md

**Test first:**

Confirm production access is granted, the exact closed-tested artifact is selected, pricing and six-market availability are correct, policy checks are clear, and the public listing preview is accurate.

**Implementation:**

Promote the approved candidate to Production and submit it for review. Do not rebuild the artifact or expand countries during this step.

**Verify:** After approval, view the listing signed out from each available regional storefront where practical, purchase/install through a non-developer account, and confirm the installed version, price, playback, privacy link, and support contact.

## Definition of Done

- [ ] All tasks completed in order
- [ ] All new production behavior has tests written before implementation
- [ ] All JVM, screenshot, lint, instrumented compile, API 31 emulator, and API 36 emulator gates pass
- [ ] The signed AAB passes bundletool and signature verification
- [ ] The merged release manifest has min SDK 31, target SDK 36, backup disabled, and no internet permission
- [ ] Signing credentials are absent from the repository, artifacts, and logs
- [ ] GitHub Pages serves the approved privacy policy publicly and the in-app link opens it
- [ ] Store copy, declarations, graphics, pricing, and market selection match the approved design
- [ ] The internal and closed testing requirements are complete with meaningful feedback
- [ ] Google grants production access and approves the release
- [ ] No unrelated files were modified
- [ ] The delivered behavior matches the approved design and adds nothing outside scope

## Authoritative References

- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- New personal-account testing requirements: https://support.google.com/googleplay/android-developer/answer/14151465
- Test-track behavior for paid apps: https://support.google.com/googleplay/android-developer/answer/9845334
- User Data and privacy-policy requirements: https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- Store asset requirements: https://support.google.com/googleplay/android-developer/answer/9866151
- Google Play pricing rules: https://support.google.com/googleplay/android-developer/answer/6334373
- Android app signing and Play App Signing: https://developer.android.com/studio/publish/app-signing
- Bundletool releases: https://github.com/google/bundletool/releases
