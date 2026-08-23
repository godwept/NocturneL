---
layout: manual
title: Getting started
description: Choose a music folder, complete the first scan, and learn the main navigation.
section: Setup
nav_order: 10
permalink: /manual/getting-started/
---

## Requirements

NocturneL requires **Android 12 or newer** and a folder containing music you own. It does not need an account or network connection.

On Android versions that use notification permission, NocturneL asks for permission to show playback controls. Allowing it lets the foreground playback notification appear; your local library remains usable if you decline.

## Choose the library folder

1. Open NocturneL and select **CHOOSE MUSIC FOLDER**.
2. In Android's system folder picker, navigate to the folder containing your music.
3. Confirm access to that folder.

NocturneL retains read access through Android's Storage Access Framework. It scans only the selected folder and its descendants—never unrelated storage.

> Choose the closest common parent folder for the albums you want in one library.

## Complete the first scan

The setup screen first reports **DISCOVERING FILES**, then shows indexing progress. You can select **CANCEL** during the scan. Cancelling preserves the last completed catalog; on first setup you can choose or scan the folder again.

After scanning, playable albums appear in the library. An empty result usually means the chosen folder has no supported audio candidates or the device cannot read them. See [Troubleshooting]({{ '/manual/troubleshooting/' | relative_url }}).

## Learn the main navigation

The primary navigation below the header uses five labels:

- **LIB** — album library
- **SEA** — local search
- **ART** — artists
- **PLY** — playlists
- **NOW** — current playback and queue

Use the **Settings** gear at the top right for rescanning, appearance, privacy, and related preferences.

Selecting a destination closes any open album, artist, playlist picker, or queue editor and returns to that destination's top level.
