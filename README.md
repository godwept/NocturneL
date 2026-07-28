# NocturneL

NocturneL is an offline, terminal-themed Android music player for a user-selected local music folder.

## Build and install

This repository is designed to build in GitHub Actions because the local machine does not have Android tooling. Each successful workflow uploads `nocturnel-debug-apk`; download it to the Pixel 7 and allow your browser/files app to install unknown apps when prompted.

For local development, install JDK 17 and the Android SDK, then run `./gradlew.bat testDebugUnitTest` or `./gradlew.bat assembleDebug`.

## Music folders

Choose a single root folder in Android's system picker. NocturneL scans only that folder and its descendants when you explicitly request a rescan. Album art resolves from embedded artwork, then `cover.jpg`, `folder.jpg`, `albumart.jpg`, or `front.jpg` in the album folder, then a terminal placeholder.

Playlists import and export as relative-path UTF-8 `.m3u8` files, so they remain portable when placed alongside the same music library.
