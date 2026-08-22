---
layout: manual
title: Playlists and backups
description: Create and edit playlists, move tracks, and import or export portable files.
section: Library
nav_order: 60
permalink: /manual/playlists/
---

## Create and use playlists

Open **PLY**, enter a name under **NEW PLAYLIST**, and choose **CREATE**. Each playlist offers **OPEN**, **PLAY**, **EXPORT**, and **DELETE**.

Inside **OPEN** you can:

- change the name and choose **RENAME**;
- choose **ADD TRACK**, filter available tracks, and select `+`;
- remove a track with **X**;
- drag tracks into a new order or use accessible move actions;
- play all available entries; or
- use **ADD QUEUE** to append them to current playback.

From an album, open its playlist picker to add the complete album to an existing playlist or create a new one and add it immediately. Duplicate or unavailable entries produce a notice rather than silently changing the list.

## Import and export M3U playlists

**EXPORT** writes one playlist as an **M3U8** file at a location chosen through Android's document picker. **IMPORT** accepts M3U, M3U8, plain-text playlist documents, and NocturneL ZIP bundles.

Imported paths are matched against the selected library. An entry can remain in the playlist while unavailable; it is skipped during playback and queue addition until its source file can be matched again.

## Back up every playlist

Choose **EXPORT ALL** to create a **ZIP** named `NocturneL Playlists.zip`. Import that ZIP to restore its contained playlists. These are user-controlled document files: exported copies remain where you saved them after app data is cleared or NocturneL is uninstalled, until you delete them yourself.
