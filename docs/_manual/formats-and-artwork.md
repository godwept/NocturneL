---
layout: manual
title: Formats, metadata, and artwork
description: Learn which files are scanned and how tags and cover images are resolved.
section: Library
nav_order: 90
permalink: /manual/formats-and-artwork/
---

## Candidate audio formats

NocturneL scans these filename extensions, case-insensitively:

- **MP3** (`.mp3`)
- **M4A** (`.m4a`)
- **AAC** (`.aac`)
- **OGG** (`.ogg`)
- **Opus** (`.opus`)
- **WAV** (`.wav`)
- **FLAC** (`.flac`)

An accepted extension makes a file a scan candidate. Actual decoding depends on the device's Android media codecs, so a candidate can still be unplayable on a particular phone.

## Metadata fallbacks

NocturneL prefers embedded title, album, artist, track number, year, and artwork metadata when available. Missing text tags fall back to the file and folder path: the filename becomes the track title, its parent folder becomes the album, and the next parent becomes the artist. Numbered filenames such as `01 - Track Name.flac` can supply a track number and cleaned title.

## Artwork order

Artwork is resolved in this order:

1. A **manual** image chosen with **SET COVER**
2. **Embedded** audio artwork
3. A recognized **folder** image
4. The generated terminal **placeholder**

Recognized folder-cover filenames are `cover.jpg`, `folder.jpg`, `albumart.jpg`, and `front.jpg`, matched without regard to case. Selecting a manual image for an album makes it the first choice on future loads while Android retains access.
