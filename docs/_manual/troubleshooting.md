---
layout: manual
title: Troubleshooting and FAQ
description: Resolve common library, playback, playlist, queue, and visualizer problems.
section: Help
nav_order: 100
permalink: /manual/troubleshooting/
---

## “No playable albums yet”

Confirm that the selected folder contains supported file extensions, then use **RESCAN LIBRARY**. If you selected a single album folder by mistake, use **CHANGE MUSIC FOLDER** and choose the common parent folder.

## Folder access was lost

Android can revoke document-tree permission after files move, storage changes, or system cleanup. Choose the folder again when prompted. If it is a different source, NocturneL asks before clearing data tied to the previous library.

## A scan is slow, fails, or was cancelled

Large libraries and files with expensive metadata can take time. Leave NocturneL open until indexing finishes, or cancel to preserve the last completed catalog. Retry **RESCAN LIBRARY**. A persistent failure should be reported with the folder/storage type and approximate file count.

## A file appears unsupported or will not play

Check that its extension is MP3, M4A, AAC, OGG, Opus, WAV, or FLAC. Android device codecs ultimately determine playback. Try another file encoded in the same format to distinguish file damage from device support.

## Metadata or artwork is missing

Correct the file's embedded tags and rescan, or use **SET COVER** on the album. You can also add `cover.jpg`, `folder.jpg`, `albumart.jpg`, or `front.jpg` to the album folder and rescan.

## Playback controls do not appear in notifications

On Android versions that require notification permission, allow notifications for NocturneL in system settings. Playback can continue without the visible notification controls, subject to Android background-service rules.

## A playlist entry is unavailable

The referenced path no longer matches a playable file in the selected library. Restore the file/path, select the correct music folder, or remove and re-add the track. Exported playlists do not copy audio files.

## The visualizer says “SIGNAL UNAVAILABLE” or looks late

Signal availability depends on the active playback route. For timing, use the −/+ sync controls and select the label to reset. Recalibrate after changing phones, wired outputs, or Bluetooth devices.

## The queue says “QUEUE CHANGED · TRY AGAIN”

Playback changed while the editor was applying an action. Use the refreshed queue and repeat the edit. This protects the wrong occurrence from being moved or removed.

## Contact support

Email [nocturnelapp@gmail.com](mailto:nocturnelapp@gmail.com) with your Android version, device model, NocturneL version, what you expected, and the exact notice shown. Do not send private audio files unless explicitly requested and you choose to do so.
