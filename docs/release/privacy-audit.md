# Release Privacy Audit

Complete this against the signed candidate, not only the source tree.

- [ ] Runtime dependencies remain limited to the reviewed AndroidX, Compose, Room, Media3, Coil, DocumentFile, and Kotlin serialization families.
- [ ] No dependency adds analytics, advertising, telemetry, remote crash reporting, accounts, or network transmission.
- [ ] The dumped merged manifest is retained with the release artifact.
- [ ] Merged permissions contain no android.permission.INTERNET or ACCESS_NETWORK_STATE.
- [ ] Every exported component is required and appropriately protected.
- [ ] Android cloud backup is disabled and android:allowBackup is false.
- [ ] The app works in airplane mode after local folder selection.
- [ ] The public privacy policy is accessible while signed out.
- [ ] The in-app PRIVACY POLICY control opens the approved URL.
- [ ] Play Data safety says no data collected or shared and remains consistent with the shipped SDKs.
- [ ] The store listing, privacy policy, and in-app behavior make the same claims.
