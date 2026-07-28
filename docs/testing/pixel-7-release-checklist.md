# Pixel 7 release checklist

- [ ] Install a fresh `nocturnel-debug-apk` artifact from GitHub Actions.
- [ ] Choose the intended local music folder and confirm it survives an app restart.
- [ ] Run an explicit rescan and check the added/missing/skipped summary.
- [ ] Confirm `cover.jpg` or `folder.jpg` appears for an album without embedded art.
- [ ] Confirm a missing/untagged track receives a readable folder/filename fallback.
- [ ] Create a playlist; import and export a relative-path `.m3u8` file.
- [ ] Start an album track, lock the phone, and verify notification/lock-screen/headset controls.
- [ ] Disconnect headphones/Bluetooth and confirm audio focus behavior is acceptable.
- [ ] In airplane mode, rescan, browse, and play local files.
- [ ] Test known intended gapless album transitions; report device/codec behavior without enabling silence trimming.
