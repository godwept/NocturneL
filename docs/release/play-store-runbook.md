# NocturneL Play Store Runbook

Never record identity documents, legal address, phone number, tax details, banking information, tester email addresses, keystores, or passwords in this repository.

Automation agents must also read [`closed-test-agent-guide.md`](closed-test-agent-guide.md) for version/tag sequencing, GitHub artifact retrieval, and the known local Windows testing blockers.

## Account setup evidence

- [ ] Personal developer account created
- [ ] Identity verification complete
- [ ] Android device verification complete
- [ ] Public developer contact verified
- [ ] Payments profile, tax information, and payout method complete
- [ ] CAD paid-app pricing is available

## App record

- [ ] Name is NocturneL
- [ ] Package is ca.stewark.nocturnel
- [ ] Default language is English (United States)
- [ ] Type is App and category is Music & Audio
- [ ] App is marked paid before public availability
- [ ] Play App Signing is enabled with a Google-managed app-signing key
- [ ] Privacy, audience, Data safety, content rating, and app-access declarations are complete
- [ ] CAD $1.99 and only CA, US, GB, IE, AU, NZ are enabled

## Build a candidate

1. Increment versionCode for every corrected or subsequent upload and set the intended versionName.
2. Run ordinary CI and require every check to pass.
3. Confirm no existing protected play/* tag has the proposed versionCode.
4. Create the tag play/<versionName>-<versionCode>, for example play/0.1.0-1.
5. Push the tag and approve the protected play-release environment.
6. Download the CI artifact and record its Git commit.
7. Verify the AAB checksum with sha256sum --check and verify its signature with jarsigner -verify -strict.
8. Confirm mapping.txt and the dumped merged manifest are present.

The CI artifact is the candidate. Promote the same approved artifact between Play tracks; never rebuild a version in place.

## Internal testing

1. Upload the signed AAB to Internal testing.
2. Review Play's pre-launch report and app bundle warnings.
3. Install from the internal opt-in URL. A debug-signed copy with the same package may need to be uninstalled first because its signature differs.
4. Run the Pixel 7 and Android 12 release checklists.
5. Resolve every material finding with a higher version before promotion.

## Closed testing

1. Promote the approved internal release to Closed testing.
2. Create paid-app promo codes and test redemption before starting the official window.
3. Invite 15–20 people and ensure at least 12 testers remain opted in for 14 continuous days.
4. Collect meaningful feedback using the closed-test template.
5. Summarize engagement, coverage, feedback, and changes without committing tester identity data.

## Production access and release

1. Apply for production access only after Play shows the testing requirement complete.
2. Answer recruitment, engagement, feedback, value, audience, expected installs, changes, and readiness questions accurately.
3. After approval, promote the exact closed-tested candidate to Production.
4. Recheck price, countries, listing, policy status, and release notes before submitting for review.

## Recovery

- To stop a bad rollout, halt the release in Play Console immediately.
- Android and Play do not support a versionCode rollback. Fix the issue, use a higher versionCode, repeat all gates, and submit a corrective higher version.
- For an upload-key loss or compromise, follow signing.md and request a Play upload-key reset.
