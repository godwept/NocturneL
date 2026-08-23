<p align="center">
  <img src="docs/play-store/listing/graphics/feature-graphic.png" alt="NocturneL terminal music visualizer" width="900">
</p>

<h1 align="center">NocturneL</h1>

<p align="center">
  An offline, terminal-themed Android music player for the library you already own.
</p>

<p align="center">
  <a href="https://github.com/godwept/NocturneL/actions/workflows/android.yml"><img src="https://github.com/godwept/NocturneL/actions/workflows/android.yml/badge.svg" alt="Android CI"></a>
  <img src="https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white" alt="Android 12 or newer">
  <img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin and Jetpack Compose">
  <img src="https://img.shields.io/badge/network-not%20required-39FF88" alt="No network required">
</p>

NocturneL turns a folder of local audio files into a focused, phosphor-green listening experience. Pick a music folder, scan it, and browse your collection without accounts, ads, analytics, or an internet connection.

## Screenshots

| Library | Album | Spectrum bands | Radar visualizer | Album artwork |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/play-store/listing/graphics/phone/01-library.png" alt="NocturneL album library in a phosphor-green terminal grid" width="180"> | <img src="docs/play-store/listing/graphics/phone/02-album.png" alt="NocturneL album detail with track list and local playback actions" width="180"> | <img src="docs/play-store/listing/graphics/phone/03-vis1.png" alt="NocturneL Now Playing with a green terminal spectrum-band visualizer" width="180"> | <img src="docs/play-store/listing/graphics/phone/04-vis2.png" alt="NocturneL Now Playing with a neon circular radar visualizer" width="180"> | <img src="docs/play-store/listing/graphics/phone/05-now-playing-album.png" alt="NocturneL Now Playing with album artwork and playback controls" width="180"> |

## Highlights

- **Your music, on your device** — grant access to one folder through Android's system picker; NocturneL scans only that folder and its descendants.
- **A library built for browsing** — move between album and artist views, search your collection, switch between grid and cover-flow layouts, and sort by artist, title, year, or play count.
- **Full playback control** — background and lock-screen playback, gapless transitions, shuffle, repeat, seeking, and a drag-reorderable queue.
- **Terminal visualizers** — cycle between album art, a circular radar, and responsive spectrum bands with adjustable sync and phosphor afterglow.
- **Playlists that stay portable** — create and reorder local playlists, import or export M3U/M3U8 files, or back up every playlist in a single ZIP.
- **Listening activity** — keep favorites, play counts, recently played albums, and listening history locally on the device.
- **Artwork fallbacks** — use embedded artwork or common cover files such as `cover.jpg`, `folder.jpg`, `albumart.jpg`, and `front.jpg`.

Supported audio file extensions are MP3, M4A, AAC, OGG, Opus, WAV, and FLAC. Actual playback support can vary with the device's Android media codecs.

## Privacy by design

NocturneL has no internet permission. It contains no advertising, analytics, telemetry, remote crash reporting, or user accounts. Library metadata, playlists, history, favorites, playback state, and settings remain on your device, and Android cloud backup is disabled.

See the full [privacy policy](https://godwept.github.io/NocturneL/privacy/) for details.

## Build from source

### Requirements

- JDK 17
- Android SDK with API 36 installed
- Android Studio, or the included Gradle wrapper

Clone the repository and build a debug APK:

```bash
git clone https://github.com/godwept/NocturneL.git
cd NocturneL
./gradlew assembleDebug
```

On Windows, use `./gradlew.bat assembleDebug` instead. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run the same core checks used by CI:

```bash
./gradlew testDebugUnitTest validateDebugScreenshotTest assembleDebugAndroidTest lintRelease
```

Every successful [Android CI run](https://github.com/godwept/NocturneL/actions/workflows/android.yml) also publishes a `nocturnel-debug-apk` workflow artifact for signed-in GitHub users.

## Tech stack

- Kotlin and Jetpack Compose
- AndroidX Media3 / ExoPlayer and MediaSession
- Room for the on-device catalog, playlists, and listening data
- Android Storage Access Framework for user-controlled file access
- Coil for album artwork
- JUnit, AndroidX Test, and Compose screenshot tests

## Project layout

| Path | Purpose |
|---|---|
| `app/src/main` | Application, playback, library, persistence, and Compose UI code |
| `app/src/test` | JVM unit and contract tests |
| `app/src/androidTest` | On-device integration tests |
| `app/src/screenshotTest` | Compose visual regression tests |
| `docs` | Privacy, release, testing, store-listing, and design documentation |

## Support

Questions, privacy requests, and support inquiries can be sent to [nocturnelapp@gmail.com](mailto:nocturnelapp@gmail.com).
