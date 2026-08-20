# NocturneL Play Store Production Release Design

**Date:** 2026-08-20
**Status:** Approved

## Goal

Release NocturneL publicly through a new personal Google Play developer account as a privacy-first, offline Android music player. The app will cost CAD $1.99, support Android 12 (API 31) and newer, target API 36, and use CI to produce signed Android App Bundles that are uploaded to Google Play manually.

## Success Criteria

- [ ] A release build supports API 31 through API 36 and targets API 36.
- [ ] CI passes unit tests, screenshot validation, lint, instrumented checks, and release-build validation.
- [ ] An intentionally triggered workflow produces a signed `.aab` without exposing signing credentials.
- [ ] The release passes Google Play's automated pre-launch checks.
- [ ] At least 12 real testers remain enrolled in the closed test for 14 consecutive days and provide usable feedback.
- [ ] Google Play grants the personal developer account production access.
- [ ] The app is publicly purchasable in Canada, the United States, the United Kingdom, Ireland, Australia, and New Zealand.
- [ ] The store listing and Data safety declaration accurately state that the app sends no user data off-device.

## Scope

**In scope:**

- Lower `minSdk` from 33 to 31 and audit Android 12 compatibility.
- Keep `compileSdk` and `targetSdk` at 36.
- Correct versioning, release build configuration, optimization, and signing.
- Disable or tightly restrict Android cloud backup so the offline/no-transmission promise remains accurate.
- Add CI quality gates, including API 31 emulator coverage, existing tests, lint, and signed `.aab` generation.
- Handle the resettable Google Play upload key securely through GitHub Actions.
- Upload releases manually to Google Play with Play App Signing enabled.
- Prepare English store copy, screenshots, a feature graphic, a privacy policy, a support contact, the Data safety form, a content rating, and required declarations.
- Run internal testing followed by the mandatory 12-person closed test.
- Set a CAD $1.99 base price and distribute in the six agreed markets.
- Document release and recovery procedures.

**Out of scope:**

- Ads, analytics, telemetry, crash-reporting services, accounts, or cloud features.
- In-app purchases and subscriptions.
- Automatic publishing from CI to Google Play.
- Android versions older than API 31.
- Wear OS, Android TV, Android Auto, or dedicated tablet features.
- Localization beyond English.
- New player features or a visual redesign.

## Design

### Product and Distribution

The permanent Play identity remains **NocturneL** with package ID `ca.stewark.nocturnel`. The first release is a paid app with a CAD $1.99 base price, intended for a general audience aged 13 and older and not marketed toward children. Initial availability covers Canada, the United States, the United Kingdom, Ireland, Australia, and New Zealand with an English store listing.

The app remains completely offline. It contains no advertising, analytics, telemetry, remote crash reporting, accounts, or runtime data transmission. The Play Store handles purchase entitlement; NocturneL does not add its own billing SDK, license server, account, or payment state.

### Application and Release State

No application data model changes are required. Library metadata, playlists, listening history, settings, and selected-folder access remain local to the device. Android cloud backup is disabled so local app data is not transmitted through backup. Existing playlist export and import remains the user-controlled mechanism for portable data.

`versionName` and a monotonically increasing `versionCode` remain committed in Gradle as the release source of truth. Each release artifact is tied to an exact Git commit and named with its version and code. Corrections always receive a higher version code; an already published version is never rebuilt in place.

Google Play App Signing holds the permanent app-signing key. CI holds only the replaceable upload keystore, alias, and passwords in encrypted GitHub secrets scoped to a protected `play-release` environment. The keystore exists only temporarily on the CI runner and is never committed, logged, or uploaded as an artifact. CI retains the signed App Bundle, its SHA-256 checksum, and the release optimization mapping file.

### CI Release Interface

An intentionally triggered GitHub Actions workflow performs the following operations:

1. Read and validate the committed version information.
2. Run unit tests, screenshot validation, release lint, instrumented-test compilation, and API 31 and API 36 emulator tests.
3. Build and validate the optimized release App Bundle.
4. Sign the bundle using credentials from the protected `play-release` environment.
5. Verify the signature.
6. Publish the `.aab`, SHA-256 checksum, and optimization mapping as restricted CI artifacts.

The workflow does not hold Google Play Developer API credentials and cannot upload, promote, halt, or publish a Play release.

### Google Play Handoff

The developer performs the Play handoff manually:

1. Download the CI artifacts and verify the checksum.
2. Upload the signed bundle to Play's internal-testing track.
3. Review the pre-launch report and test installation and playback.
4. Promote the approved build to the closed-testing track.
5. Supply paid-app promo codes to closed testers.
6. Keep at least 12 real testers opted in continuously for at least 14 days and collect meaningful feedback.
7. Address material findings with a higher-version release and repeat affected checks.
8. Apply for production access with an accurate summary of tester activity, feedback, and resulting changes.
9. Promote an approved build to production after access is granted.

### Android Compatibility Contract

The app supports API 31 and newer while targeting API 36. Runtime notification permission requests are gated to Android 13/API 33 and newer. Foreground media playback, media notifications, Storage Access Framework folder access, persisted URI permissions, background playback, and restoration behavior are verified on the minimum and current SDK levels.

Release validation inspects the final merged manifest for unexpected permissions, exported components, backup behavior, and network access. The final app must not request the internet permission or introduce a data-transmitting SDK.

### Public Interfaces and Compliance

Public-facing interfaces are limited to:

- An English Google Play store listing.
- Required phone and tablet screenshots and a feature graphic.
- A publicly accessible privacy-policy page stating the app's local-only data handling.
- A support email address.
- Accurate Data safety, content-rating, target-audience, app-access, advertising, and other Play declarations.

The privacy policy and Data safety answers must describe the shipped artifact, including all transitive SDK behavior, rather than relying solely on intended application behavior.

### Failure Handling and Edge Cases

- Any failed test, lint error, signing error, bundle validation error, or API 31 compatibility failure blocks release artifact publication.
- Missing signing secrets fail clearly without printing secret values.
- CI rejects duplicate, decreasing, or invalid version codes.
- Debug-installed copies may need to be uninstalled before installing the Play-signed build because their signatures differ; tester instructions document this.
- Disabling cloud backup means playlists, history, and settings do not transfer automatically to a replacement device. Playlist export and import provides explicit portability.
- A tester who opts out resets their continuous 14-day count. More than 12 testers should be recruited to allow for attrition.
- Promo-code redemption and regional availability are verified before the official closed-test period begins.
- Play pre-launch warnings and tester-reported crashes block promotion until reviewed and resolved or explicitly documented as non-applicable.
- A rejected submission is corrected with a new version rather than bypassing the policy finding.
- A bad production release is halted in Play Console and replaced with a higher-version corrective release; published versions cannot be rolled backward.
- Loss or compromise of the CI upload key uses Google Play's upload-key reset process. The permanent signing key remains managed by Play App Signing.

## Testing Strategy

### Automated Release Gates

- Run the existing JVM unit-test suite.
- Validate the existing Compose screenshot suite.
- Run Android lint against the release variant.
- Compile the complete instrumented-test suite.
- Run instrumented tests on API 31 and API 36 emulators.
- Run Room migration tests against committed schemas.
- Assemble, validate, sign, and verify the release App Bundle.
- Inspect sources and the merged release manifest for unexpected internet permission, enabled backup, exported components, or data-transmitting dependencies.

### Manual Device and Play Testing

- Fresh install and first-run folder selection.
- Library scanning with supported, malformed, and empty folders.
- Playback while foregrounded, backgrounded, and screen-locked.
- Media notifications and controls on Android 12 and Android 13 or newer.
- Audio-focus changes, interruptions, wired headphones, and Bluetooth.
- Process death, device restart, and playback restoration.
- Playlist creation, editing, export, import, and database migration.
- Airplane-mode operation to confirm full offline functionality.
- Upgrade from the previous closed-test build without data loss.
- Review of Play's pre-launch report across its available device configurations.
- Verification of store price, country availability, promo codes, privacy disclosures, and device eligibility.

Closed testers receive a short, repeatable test checklist and a support email for feedback. Feedback, reproducible issues, and resulting changes are recorded so the production-access application can accurately describe tester engagement and production readiness.

## Open Questions

- The public URL and hosting location for the privacy policy will be selected during implementation planning.
- The support email address will be supplied during Play Console setup.
- The exact set and device framing of store-listing screenshots will be finalized after release UI validation.
