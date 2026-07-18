# DESIGN — TaskFlow

Visual identity and UI standards. TaskFlow follows **Material Design 3** implemented with **Angular Material**. When in doubt, defer to Material guidance rather than inventing custom patterns.

---

## 1. Brand Identity

- **Name:** TaskFlow
- **Personality:** Calm, organized, dependable. The UI should feel like a tidy desk — never busy, never loud.
- **Logo/wordmark:** Text wordmark "TaskFlow" in the display font, primary color. No icon logo for MVP.
- **Voice:** Short, direct microcopy. "Create project", not "Let's get started creating your very first project!". Errors say what happened and what to do next.

## 2. Color

Built on a Material 3 theme generated from the seed color. Use Angular Material theming (`define-theme`) — never hardcode hex values in component styles; always consume theme tokens / CSS variables.

| Token | Value | Usage |
|---|---|---|
| Seed / Primary | `#4355B9` (indigo) | Buttons, active states, links, wordmark |
| Secondary | derived by M3 | Chips, secondary actions |
| Tertiary | derived by M3 | Selection highlights, accents |
| Error | `#BA1A1A` | Destructive actions, validation errors |
| Surface | derived by M3 | Cards, dialogs, backgrounds |

### Semantic status colors (task domain)

Rendered as **label text** (not tinted backgrounds), so the light values are the darker
700-level shades that clear WCAG AA (§8, ≥4.5:1) on card surfaces (`surface-container-low`).
Dark-mode variants are defined alongside them and verified separately.

| Meaning | Light | Notes |
|---|---|---|
| Status TODO | neutral gray `#4B5563` | Never use primary for TODO |
| Status IN_PROGRESS | blue `#1D4ED8` | |
| Status DONE | green `#15803D` | |
| Priority HIGH | red `#B91C1C` | Icon + label, never color alone |
| Priority MEDIUM | amber `#B45309` | |
| Priority LOW | gray `#4B5563` | |

Project color tags: fixed palette of 8 muted hues offered at project creation — no free color picker.

### Dark mode
- Support light and dark via M3 color schemes; respect `prefers-color-scheme`, with a manual toggle persisted in `localStorage`.
- Semantic status colors get dark-mode variants with equivalent contrast; define both once as CSS custom properties.

## 3. Typography

- **Font:** Roboto (Material default), self-hosted via `@fontsource/roboto` — no CDN dependency.
- **Scale:** Use the Material type scale. Do not invent font sizes.

| Role | Usage |
|---|---|
| `headline-small` | Page titles |
| `title-medium` | Card titles, dialog titles, section headers |
| `body-medium` | Default body text |
| `body-small` | Metadata (dates, counts) |
| `label-large` | Buttons, tabs |

- Line length for descriptions: max ~72 ch.
- No font weights outside 400 / 500 / 700.

## 4. Spacing & Layout

- **Grid unit: 8 px.** All margins, paddings, and gaps are multiples of 8 (4 px allowed for icon-to-label gaps only).
- Page content max-width: 1200 px, centered, 24 px side padding (16 px under 600 px viewport).
- **Layout shell:** top app bar + navigation rail (desktop) / bottom navigation or drawer (mobile). Content area scrolls; shell does not.
- Cards: 16 px internal padding, 16 px gap between cards.
- Board columns: fixed 320 px width, horizontal scroll on overflow.
- Density: default Material density (0). Do not use compact density for MVP.

## 5. Elevation & Shape

- Follow M3 elevation levels: cards at level 1, raised on drag (level 3), dialogs level 3, app bar level 0 with border.
- Corner radius: Material defaults (cards 12 px, buttons full/20 px, dialogs 28 px). Never override per-component.

## 6. Motion

Use Material motion tokens. Motion communicates state change — never decoration.

| Interaction | Duration | Easing |
|---|---|---|
| Hover/press states | 100 ms | standard |
| Dialog open/close | 300 ms / 200 ms | emphasized decelerate / accelerate |
| Route transitions | 200 ms fade-through | standard |
| Board card drop | 250 ms | emphasized |
| Snackbar in/out | 200 ms | standard |

- Respect `prefers-reduced-motion`: replace movement with opacity-only transitions.
- No looping, parallax, or attention-seeking animation anywhere.

## 7. Components (Angular Material usage)

- Use Angular Material components for everything they cover: buttons, form fields, dialogs, snackbars, menus, tables, tabs, chips, tooltips, progress indicators.
- Custom components allowed only for domain UI (task card, board column, priority badge) — and they must consume theme tokens.
- Forms: `mat-form-field` with `outline` appearance everywhere. Labels always visible (no placeholder-as-label).
- Feedback: snackbar for success (3 s, bottom center), inline for validation errors, dialog only for destructive confirmation.
- Empty states: icon + one sentence + primary action button. Every list/board has one.
- Loading: skeleton placeholders for initial page loads; small spinner only for in-place actions. Never a full-screen spinner.

## 8. Accessibility (WCAG 2.1 AA — hard requirement)

- Contrast ≥ 4.5:1 for text, ≥ 3:1 for large text and UI components. Verify semantic colors in both themes.
- Never rely on color alone: status and priority always pair color with icon and/or text label.
- Full keyboard support: logical tab order, visible focus ring (M3 focus indicator, never `outline: none` without replacement), Escape closes dialogs/menus.
- Drag-and-drop must have a keyboard alternative (status change via task menu).
- All interactive elements: accessible names (`aria-label` where text is absent), 44×44 px minimum touch targets.
- Forms: errors linked with `aria-describedby`, announced via live region.
- Images/icons that are decorative: `aria-hidden="true"`.
- Test with a screen reader (NVDA) on the three core flows: login, create task, change status.

## 9. Anti-Patterns (do not do)

- No hardcoded colors, font sizes, or spacing values in component SCSS — theme tokens only
- No mixing of filled/outline form field appearances
- No custom scrollbars, no custom checkboxes/radios replacing Material's
- No toast stacking — one snackbar at a time
- No disabled buttons without an explanation (tooltip or helper text)
