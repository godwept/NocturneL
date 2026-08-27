# Closed-Test Release Guide for Automation Agents

This guide is for future coding agents preparing NocturneL Play Store closed-test candidates. Read it with `play-store-runbook.md` and `signing.md` before changing a release version or creating a tag.

## Authority and safety

- Do not push commits, create or push tags, approve a GitHub environment, upload to Play Console, or message testers unless the user explicitly authorizes that external action.
- Never commit signing keys, passwords, GitHub secret values, tester identities, promo codes, tax/payment data, or Play Console screenshots containing private account data.
- Treat every Play upload as irreversible with respect to `versionCode`. A rejected or broken candidate must be replaced with a higher code; never rebuild or retag a previously uploaded code.
- The signed GitHub Actions artifact is the candidate. A locally built `app-release.aab` is unsigned by design and must not be uploaded to Play Console.

## Candidate preparation

1. Inspect the current branch, worktree, remote, versions, and existing Play tags:

   ```powershell
   git status --short
   git branch --show-current
   git remote -v
   git tag --list 'play/*-*' --sort=-version:refname
   rg -n "releaseVersion(Code|Name)" app/build.gradle.kts
   ```

2. Preserve user-owned worktree changes. Confirm every file intended for the release and do not sweep unrelated edits into the candidate.
3. Increment `releaseVersionCode` in `app/build.gradle.kts`. Keep or intentionally change `releaseVersionName` according to the release series.
4. Update the pinned expectations in `app/src/test/java/ca/stewark/nocturnel/ReleaseConfigurationTest.kt` in the same change. Write the new expectation first and confirm it fails before changing the build file.
5. Run the applicable feature tests and the release gates:

   ```powershell
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat validateDebugScreenshotTest
   .\gradlew.bat assembleDebugAndroidTest
   .\gradlew.bat lintRelease
   .\gradlew.bat assembleDebug
   .\gradlew.bat bundleRelease
   .\gradlew.bat printReleaseVersion
   git diff --check
   ```

6. Run connected tests when a device or suitable AVD is actually available. Otherwise report them as pending; do not imply that Android-test compilation executed the tests.
7. Complete the relevant device checklist under `docs/testing/`. Hardware-specific Bluetooth and vehicle checks remain manual.
8. Review the final diff and status. Commit the complete candidate to `main`, then push `main` before creating the release tag.

## Tag and GitHub artifact flow

1. Form the lightweight tag as `play/<versionName>-<versionCode>`, for example `play/0.1.0-6`.
2. Before creating it, validate the proposed name and monotonic code with `.github/scripts/validate-release-version.sh`, passing all prior `play/*-*` tags. The unit suite also covers this validator.
3. Confirm the proposed tag does not already exist locally or remotely.
4. Create the lightweight tag on the exact committed candidate and push only that tag:

   ```powershell
   git tag play/<versionName>-<versionCode>
   git push origin play/<versionName>-<versionCode>
   ```

5. The tag triggers `.github/workflows/play-release.yml`. Do not create a GitHub Release; the workflow artifact is the delivery mechanism.
6. Watch all `Play release candidate` jobs. The workflow must pass:

   - Unit tests, screenshot validation, release lint, Android-test compilation, and unsigned bundle build.
   - Instrumented tests on API 31 and API 36 emulators.
   - The protected `play-release` packaging job.

7. If the packaging job waits for the protected environment, the user or authorized repository owner must approve the `play-release` deployment. Agents must not claim the artifact is ready while this approval is pending.
8. Download the artifact named `nocturnel-<versionName>-<versionCode>-play-release` within its 30-day retention period. It contains:

   - `nocturnel-<versionName>-<versionCode>.aab` — signed with the Play upload key.
   - The `.aab.sha256` checksum.
   - `mapping.txt`.
   - `merged-manifest.xml`.

9. Verify the checksum and signature before upload. Upload the signed AAB to Internal testing first, review the pre-launch report, run the device checklists, and then promote the exact same artifact to Closed testing.
10. If any protected tag workflow fails materially, fix the problem under a higher `versionCode` and create a new tag. Do not move a protected Play tag or reuse an already uploaded code.

## Current local Windows environment

These facts were verified on 2026-08-26 and may change. Recheck them briefly instead of assuming they remain true.

### Available

- Android SDK: `C:\Program Files (x86)\Android\android-sdk` from `local.properties`.
- Installed platforms: API 34, 35, and 36.
- Installed build-tools: 35.0.0 and 36.0.0.
- `platform-tools` and `emulator` directories exist.
- One system image is installed: API 36 `google_apis_playstore` x86_64.
- JDK 21 exists at `C:\Program Files\Android\openjdk\jdk-21.0.8`.
- The SDK above is sufficient for ordinary compilation, unit tests, screenshots, lint, debug assembly, and unsigned bundle generation once an exact JDK 17 is available.

### Blockers and time-saving guidance

- `java` on PATH resolves to Java 8. Gradle 9 cannot run with it.
- JDK 11 and JDK 21 are present, but the project pins `kotlin { jvmToolchain(17) }`; JDK 21 can launch Gradle but does not satisfy the requested Java 17 compiler toolchain.
- No persistent JDK 17 was installed. Install an exact JDK 17 with user/admin approval, or use a portable JDK 17 ZIP outside the repository and set `JAVA_HOME` only for the build process. Avoid MSI administrative extraction as a portable workaround; it created restrictive ACLs and costly cleanup in the 2026-08-26 session.
- `adb` and `emulator` are not on PATH. Use:

  ```text
  C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe
  C:\Program Files (x86)\Android\android-sdk\emulator\emulator.exe
  ```

- No Android device is normally attached and no persistent local AVD is configured. The installed API 36 image can be used to create a temporary AVD for release debugging; delete it when the session ends.
- The API 31 platform/system image needed to mirror the workflow's minimum-SDK emulator job is not installed locally.
- Launching the Windows emulator from a restricted coding-agent sandbox can fail on named-pipe permissions. Use an approved unsandboxed command or rely on GitHub Actions rather than repeatedly retrying the same sandboxed launch.
- Gradle needs access to the user Gradle cache under `C:\Users\stewark2\.gradle`; restricted agents may need approval to run Gradle outside the workspace sandbox.
- `assembleDebugAndroidTest` only compiles/packages instrumented tests. It does not execute them. Use `connectedDebugAndroidTest` only when `adb devices` shows a usable target.
- If a Compose instrumented test sets `mainClock.autoAdvance = false`, explicitly advance frames after later clicks before asserting recomposed UI. Candidate 6 exposed this harness issue on both CI emulator jobs; the full API 36 suite passed after the test clock was advanced.
- A successful local `bundleRelease` produces an unsigned AAB. This is expected; the protected GitHub `package` job signs and verifies the upload artifact.
- If a temporary JDK starts a Gradle daemon, run `gradlew --stop` before deleting that JDK so Windows does not retain locks on its DLLs and module files.

## Handoff report

At the end of release preparation, report:

- Commit SHA, version name/code, and Play tag.
- Which local and CI checks passed, failed, or remain pending.
- Whether the protected environment still needs approval.
- Exact artifact name and retention period.
- Whether checksum/signature verification was performed.
- Manual device, Bluetooth, vehicle, Play pre-launch, and console-upload steps still owed.
