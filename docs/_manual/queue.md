---
layout: manual
title: Queue management
description: Add, jump, reorder, remove, undo, and clear upcoming tracks.
section: Listening
nav_order: 40
permalink: /manual/queue/
---

## Add music to the queue

Use **ADD QUEUE** from an album or playlist to append its playable tracks. The plus action beside an individual track adds only that track. Unavailable playlist entries are skipped and the notice reports how many tracks were queued.

Open **NOW**, then choose **QUEUE**. The editor separates the current selection from **UPCOMING** items.

## Edit upcoming playback

- Select an upcoming row to jump to that occurrence.
- Drag a row by its reorder handle to move it.
- With accessibility controls, use the same handle's move-up and move-down actions instead of a drag.
- Remove an item with its **X** action.
- Select **UNDO** immediately after removal to restore it.

Undo expires when you leave the editor or make an incompatible queue change.

## Clear upcoming items

Choose **CLEAR UPCOMING**, then **CONFIRM CLEAR**. The current item and already-played history remain; upcoming items are removed. If **REPEAT ALL** is active, clearing upcoming playback also disables it. Editing an explicitly ordered queue can disable **SHUFFLE** so the displayed order remains authoritative.

If playback changes while an edit is being applied, NocturneL reports **QUEUE CHANGED · TRY AGAIN**. Return to the refreshed queue and repeat the action instead of assuming the earlier row position still exists.
