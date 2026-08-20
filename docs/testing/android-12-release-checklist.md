# Android 12 release checklist

- [ ] Install the Play Internal testing build on Android 12/API 31 as a fresh install.
- [ ] Confirm Android does not show a POST_NOTIFICATIONS runtime permission prompt.
- [ ] Choose a local music folder, restart the app, and confirm folder access persists.
- [ ] Scan a populated, empty, and partially malformed folder and confirm actionable results.
- [ ] Browse the library, search, and open album and artist views.
- [ ] Play, pause, seek, skip, and edit a playlist and the playback queue.
- [ ] Confirm background and lock-screen playback plus the media notification controls.
- [ ] Confirm wired/Bluetooth disconnect and audio focus interruption behavior.
- [ ] Force process death during paused and active playback, then verify restoration.
- [ ] Restart the device and confirm the app opens without stale playback state or a crash.
- [ ] Perform an upgrade from the preceding closed-test version without losing the catalog, playlists, history, or settings.
- [ ] In airplane mode, scan, browse, and play local files.
- [ ] Open Settings, tap PRIVACY POLICY, and confirm the browser opens the approved public policy.
- [ ] Confirm playlist export/import remains the explicit portability path while cloud backup is disabled.
