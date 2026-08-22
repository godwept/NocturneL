---
layout: manual
title: Privacy and local data
description: Understand what NocturneL accesses, stores, clears, exports, and never transmits.
section: Privacy
nav_order: 110
permalink: /manual/privacy-and-data/
---

## Offline by design

NocturneL has **no internet permission**. No accounts, No ads, No analytics, No telemetry, and no remote crash reporting are included. The app cannot upload your library or listening activity.

It reads audio files, metadata, and artwork only from the folder you authorize through Android's picker and its descendants.

## Data stored on the device

The local database and preferences contain the selected folder reference, catalog, playlists, listening history, favorites, play counts, playback/resume state, manual artwork references, and settings. Android cloud backup is disabled.

Use **CLEAR HISTORY + COUNTS** to remove qualified-play history and counts while preserving favorites and resume. Changing the source folder clears the broader library-linked set after confirmation.

## Clear data and uninstall

Clearing NocturneL's storage in Android settings or uninstalling the app removes its private local data. Files you explicitly exported through Android's document picker—including M3U8 playlists and ZIP backups—remain in the location you chose until you remove them.

## Google Play and support

When a Google Play listing becomes available, Google Play will process acquisition or payment separately. NocturneL does not receive Play account or payment details.

Read the formal [Privacy Policy]({{ '/privacy/' | relative_url }}) or email [{{ site.support_email }}](mailto:{{ site.support_email }}) with privacy questions.
