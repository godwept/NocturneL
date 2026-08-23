# Play Store Screenshot Refresh Design

**Date:** 2026-08-23
**Status:** Approved

## Goal

Refresh every public screenshot surface with the same five-image sequence: Library, Album, Spectrum Bands, Radar Visualizer, and Now Playing with album artwork. The Play Store graphics contract, product website, and GitHub README will present the supplied phone captures consistently while using richer, accessible descriptions on the web surfaces.

## Success Criteria

- [ ] All three public surfaces reference all five supplied phone screenshots in numeric filename order.
- [ ] No references remain to the retired Queue or previous Now Playing screenshot files.
- [ ] Every phone screenshot exists, remains 1080x1920, and satisfies the existing opaque 24-bit PNG contract.
- [ ] Website and README labels accurately distinguish the two visualizer modes and album-art view.
- [ ] The website screenshot gallery remains usable at desktop and phone widths with five cards.

## Scope

**In scope:**

- Update the Play Store graphics manifest/contract for the five new phone screenshots.
- Update the website screenshot gallery to show all five images in the agreed order.
- Update the GitHub README screenshot section to show all five images in the same order.
- Add richer captions and accurate alt text based on what each image depicts.
- Verify image dimensions, file references, website rendering checks, and the absence of stale screenshot references.

**Out of scope:**

- Tablet screenshot changes.
- Editing or regenerating the supplied images.
- Changing the Play Store feature graphic or app icon.
- Altering broader website copy, styling, or application behavior unless a minimal layout adjustment is required for the five-card gallery.
- Uploading assets to Google Play Console or deploying the website.

## Design

### Asset and Presentation Model

The five PNG files in `docs/play-store/listing/graphics/phone` are the single source of truth. Their numeric filename prefixes define the authoritative order:

| File | Presentation label |
|---|---|
| `01-library.png` | Library |
| `02-album.png` | Album detail |
| `03-vis1.png` | Spectrum bands |
| `04-vis2.png` | Radar visualizer |
| `05-now-playing-album.png` | Now Playing with album artwork |

Each surface will reference these files directly. The graphics contract will carry concise subjects and Play Store-compatible alt text, while the website and README may use slightly richer captions appropriate to their audiences. No new asset registry or generated metadata layer is needed.

### Public Presentation

The product website will retain its existing screenshot-card pattern and responsive grid, adding a fifth card without introducing a separate gallery component. Captions will be:

- `01 / Library` — album collection grid.
- `02 / Album detail` — tracks and album actions.
- `03 / Spectrum bands` — vertical terminal equalizer.
- `04 / Radar visualizer` — circular radial display.
- `05 / Album artwork` — Now Playing with cover art.

The GitHub README will retain its compact table presentation, expanded to five columns so every screenshot remains visible at a glance. Alt text will describe both the screen and its distinctive visual content while avoiding track-specific promotional language.

### Validation and Edge Cases

The update will guard against broken references to deleted files, incorrect visualizer ordering, inconsistent captions, missing or incorrectly sized images, and alt text that exceeds the graphics contract's 140-character limit. The website must not overflow or wrap awkwardly at supported desktop and phone widths, and tablet assets must remain untouched.

The supplied PNGs are final assets. If an image-format check reveals that an image violates the existing opaque 24-bit PNG requirement, implementation will report it rather than silently re-encoding it.

## Testing Strategy

- Confirm all five files exist and are exactly 1080x1920.
- Confirm the screenshots are opaque 24-bit PNGs as required by the graphics contract.
- Search the repository for stale references to the retired Queue and Now Playing filenames.
- Check that Play Store documentation, website markup, and README use the same five-file order.
- Validate that all alt text remains concise and under 140 characters.
- Run the existing product-site checks or build process available in the repository.
- Render or inspect the website at desktop and phone widths to confirm the five-card gallery remains readable and does not overflow.
- Review the final diff to ensure no unrelated files changed.

## Open Questions

None.
