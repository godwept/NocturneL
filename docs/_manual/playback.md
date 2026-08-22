---
layout: manual
title: Playback
description: Start music, use Now Playing controls, and understand background playback.
section: Listening
nav_order: 30
permalink: /manual/playback/
---

## Start playback

Select a track from an album or search result, or use **PLAY** on an album or playlist. Starting an album or playlist replaces the active queue with its playable tracks. **SHUFFLE** on an album starts that album in shuffled order.

Open **NOW** to see the track title, artist, album, elapsed time, duration, favorite state, play count, and current visual display.

## Transport controls

- **Play or pause** with the centre transport button.
- Use **Previous** and **Next** to move through the queue.
- Drag the seek bar to a new position in the current track.
- Toggle **SHF** to shuffle the upcoming order.
- Select repeat to cycle **Repeat off**, **Repeat all**, and **Repeat one**.
- Use the star control to add or remove the current track from favorites.

## Background and system controls

Playback runs through Android's media playback service, so it can continue in the background. The playback notification and lock screen expose the media controls supported by Android and the device.

NocturneL responds to audio focus changes from calls, navigation, or another media app by pausing, ducking, resuming, or restoring volume as Android requests. Exact behavior can vary with the competing app and device.

## Restoring playback

NocturneL saves playback state locally and attempts to restore the queue, current item, position, shuffle, and repeat state after normal app recreation. Files that moved, disappeared, or lost folder access cannot be restored until the library is available again.
