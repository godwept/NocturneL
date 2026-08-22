# NocturneL Product Site and Manual Design

**Date:** 2026-08-22
**Status:** Approved

## Goal

Create a polished product website that presents NocturneL as a credible Android music player before its Google Play launch, explains its offline-first approach, and provides users with a complete, approachable manual. The site will live in the existing repository's `docs/` directory and publish through GitHub Pages at `https://godwept.github.io/NocturneL/`.

## Success Criteria

- [ ] The landing page communicates what NocturneL is within a few seconds.
- [ ] The design feels recognizably NocturneL: dark, phosphor-accented, and terminal-inspired without sacrificing readability.
- [ ] "Coming soon on Google Play" is clear but is not a dead link.
- [ ] The complete manual is reachable from the primary navigation and easy to browse on phones and desktops.
- [ ] Existing screenshots, privacy information, support contact, and GitHub repository are incorporated consistently.
- [ ] All pages work beneath the `/NocturneL/` URL path.
- [ ] The site is responsive, keyboard-accessible, and respects reduced-motion preferences.
- [ ] Content and navigation can be maintained through Jekyll layouts and Markdown without duplicating shared page structure.

## Scope

**In scope:**

- A polished responsive landing page with:
  - Hero message and "Coming soon on Google Play" status
  - Active "Read the manual" action
  - App screenshots
  - Core feature highlights
  - Offline and privacy positioning
  - Supported audio formats and Android requirements
  - Secondary GitHub link
  - Support and privacy links
- A complete manual covering:
  - Introduction and requirements
  - Getting started and selecting a music folder
  - Library scanning and organization
  - Albums, artists, search, sorting, and display modes
  - Playback controls and background playback
  - Queue management
  - Visualizers and synchronization settings
  - Favorites, play counts, recents, and history
  - Playlist creation, ordering, import, export, and backup
  - Settings
  - Supported formats and artwork behavior
  - Troubleshooting and frequently asked questions
  - Privacy, local data, deletion, and support
- Shared Jekyll layouts, navigation, footer, metadata, styling, and small progressive visual effects.
- Search-engine and social-sharing metadata.
- A helpful custom 404 page.
- Reuse of the current app screenshots and brand assets.

**Out of scope:**

- Accounts, forms, comments, analytics, cookies, or telemetry
- Online purchases or APK downloads
- A functional Google Play link before the listing exists
- Localization and multiple manual versions
- Full-text search within the manual
- A blog, changelog system, or news feed
- Heavy animation or an interactive audio visualizer
- A separate JavaScript framework or Node-based build pipeline

## Design

### Technical Approach

The site will be a custom Jekyll site contained in `docs/`. Markdown will hold the manual content, while shared Jekyll layouts will provide the page shell, navigation, footer, metadata, and reusable presentation. Custom CSS and a small amount of progressive JavaScript will create the visual identity without adding a separate framework or build pipeline.

The visual direction is terminal-inspired and highly readable. Dark surfaces, phosphor-green accents, restrained glow, grid details, and monospace labels will carry the NocturneL identity. Conventional layouts, readable body typography, clear hierarchy, and accessible interaction states will take priority over terminal simulation.

### Content and State Structure

The site will be entirely static. It will have no user data, backend, persistent browser state, tracking, cookies, or analytics.

Site-wide metadata will define:

- Product name and description
- Canonical site URL and `/NocturneL` base path
- Support email
- GitHub repository URL
- Current release status: "Coming soon on Google Play"

Shared navigation data will identify these destinations:

- Home
- Features
- Screenshots
- Manual
- Privacy
- GitHub

Landing-page content will include product positioning, hero copy, feature groups, screenshots with accessible descriptions, requirements, supported formats, and release status.

Each manual page will have consistent front matter containing its title, short description, navigation section, navigation order, and stable permalink.

Shared assets will include the existing app icon and screenshots, a site stylesheet, a minimal enhancement script, and a social-sharing image derived from the existing feature graphic.

The only transient interface state will be the mobile navigation's open or closed state. Manual navigation will remain usable without JavaScript, and content will use normal links with stable URLs.

### Pages and Navigation Interfaces

Public routes will be stable and human-readable:

- `/NocturneL/` — product landing page
- `/NocturneL/manual/` — manual overview and table of contents
- `/NocturneL/manual/getting-started/`
- `/NocturneL/manual/library/`
- `/NocturneL/manual/playback/`
- `/NocturneL/manual/queue/`
- `/NocturneL/manual/visualizers/`
- `/NocturneL/manual/playlists/`
- `/NocturneL/manual/listening-activity/`
- `/NocturneL/manual/settings/`
- `/NocturneL/manual/formats-and-artwork/`
- `/NocturneL/manual/troubleshooting/`
- `/NocturneL/manual/privacy-and-data/`
- `/NocturneL/privacy/` — formal privacy policy
- `/NocturneL/404.html` — branded not-found page

The landing page will use a compact global header with links to Features, Screenshots, Manual, Privacy, and GitHub. Same-page links will scroll to landing-page sections; Manual and Privacy will open dedicated pages. GitHub will appear as a secondary navigation link rather than a primary call to action.

Manual pages will provide:

- Persistent section navigation on wider screens
- A compact table of contents on phones
- Previous and next topic links
- A visible link back to the landing page
- Clear heading anchors for sharing specific instructions
- Callouts for notes, warnings, and troubleshooting tips

Jekyll will provide shared layouts for the landing page, manual pages, and general policy or error pages. All internal URLs and asset references will use Jekyll's base-path-aware URL handling so the site functions correctly at `/NocturneL/`.

### Error Handling and Edge Cases

- Without JavaScript, all content, links, screenshots, and manual navigation will remain available. Only optional visual polish and enhanced mobile-menu behavior may be reduced.
- "Coming soon on Google Play" will be displayed as status text, not an inactive or misleading link.
- Missing screenshots will retain descriptive alternative text without blocking surrounding content.
- Direct links to manual pages and heading anchors will work independently of landing-page navigation.
- Unknown URLs will display a branded 404 page with links to Home, Manual, Privacy, and GitHub.
- Internal links and assets will remain correct under GitHub Pages' `/NocturneL/` base path.
- Long headings, narrow phone screens, large text settings, and keyboard navigation will not cause content loss or horizontal scrolling.
- Motion and glow effects will be reduced when the visitor requests reduced motion or greater contrast.
- Navigation will communicate the current page without relying on color alone.
- External GitHub and email links will be visibly identifiable.
- Playback compatibility wording will acknowledge that actual format support can vary with Android device codecs.
- The manual will describe current app behavior. Features that are not yet shipped will not be documented as available.

## Testing Strategy

Automated checks will verify:

- The Jekyll site builds successfully.
- Every manual page has the required title, description, permalink, section, and navigation order.
- Manual navigation contains every topic exactly once.
- Internal links and image references resolve correctly beneath `/NocturneL/`.
- Required public routes exist after the build.
- No placeholder Google Play URL is exposed.
- The privacy and support links remain available.
- Existing Android build and test behavior is unaffected.

Browser-level review will cover:

- Phone, tablet, and desktop layouts
- Current Chrome and Firefox behavior
- Keyboard-only navigation and visible focus states
- Screen-reader landmarks, heading order, and image descriptions
- Increased text size and narrow-screen overflow
- Reduced-motion behavior
- The site with JavaScript disabled
- Direct entry into individual manual pages and the 404 page
- Basic Lighthouse accessibility, performance, and SEO checks

The landing page's main visual states will also receive lightweight screenshot checks at representative phone and desktop widths to catch accidental layout or branding regressions.

## Open Questions

- None. A custom domain, localization, manual versioning, full-text search, and a live Google Play link are deferred until they become necessary.
