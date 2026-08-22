# NocturneL Product Site Review

Use this checklist for a local Jekyll build, pull-request preview, or the published GitHub Pages URL.

- URL/build:
- Commit:
- Tester:
- Date:
- Notes:

## Responsive layout

- [ ] Phone (approximately 390×844): no clipped controls or horizontal overflow.
- [ ] Tablet (approximately 768×1024): landing grids and manual navigation remain balanced.
- [ ] Desktop (approximately 1440×1000): content width and persistent manual sidebar are correct.
- [ ] Chrome: landing, manual, Privacy, and 404 pages render correctly.
- [ ] Firefox: landing, manual, Privacy, and 404 pages render correctly.
- [ ] Review both CI screenshots: `landing-phone.png` and `landing-desktop.png`.

## Keyboard and assistive technology

- [ ] Keyboard navigation reaches the skip link, header, content actions, manual navigation, and footer in a logical order.
- [ ] Every focus indicator is clearly visible against the terminal palette.
- [ ] Mobile Menu announces its expanded/collapsed state and closes with Escape.
- [ ] A screen reader identifies header, primary navigation, main, article, complementary navigation, and footer landmarks.
- [ ] Heading order is logical and heading anchor links have meaningful names.
- [ ] Every informative image has useful alt text; decorative details are hidden from assistive technology.
- [ ] Current navigation state does not rely on colour alone.

## Adaptation and fallbacks

- [ ] At 200% browser text size, content remains readable and operable.
- [ ] Long titles, email addresses, and code values cause no horizontal overflow.
- [ ] With reduced motion enabled, smooth scrolling, transitions, and glow movement are suppressed.
- [ ] With increased contrast enabled, text and borders remain distinguishable.
- [ ] With JavaScript disabled, all content and navigation remain available.
- [ ] Direct deep links to each manual topic and a heading anchor load correctly.
- [ ] An unknown route shows the branded 404 page with recovery links.

## Content and quality

- [ ] Every manual instruction matches the controls and behavior in the current app release.
- [ ] “Coming soon on Google Play” is status text, not a dead link.
- [ ] Privacy, support email, GitHub, screenshots, Android requirement, and format details are current.
- [ ] Lighthouse accessibility review has no critical failures.
- [ ] Lighthouse performance review identifies no avoidable blocking or oversized assets.
- [ ] Lighthouse SEO review finds a title, description, canonical URL, and indexable content.
