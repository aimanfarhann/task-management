# DESIGN — TaskFlow · "Focus"

Visual identity and UI standards. TaskFlow's design direction is **Focus** — a refined, dark‑first workspace: a calm command center you can sit in all day. It is built on **Angular Material 3**, but the M3 tokens are overridden so nothing reads as stock Material. When in doubt, prefer restraint and precision over decoration.

The whole system flows from CSS custom properties in `frontend/src/styles.scss` — `--mat-sys-*` (Material system tokens) and `--tf-*` (our semantic tokens). **Components must consume only these tokens; never hardcode a color, font, or radius.**

---

## 1. Brand Identity

- **Name:** TaskFlow. **Personality:** focused, engineered, quietly premium. The UI should feel like a precise tool — confident, never loud.
- **Wordmark:** an iris "flow" mark (a small rounded‑square glyph with two offset bars) + "TaskFlow" set in the display face. Present in the app bar and on the auth pages.
- **Signature:** the board is the centerpiece; **metadata speaks in monospace** (ids, dates, counts, keyboard hints) — that mono voice is the through‑line that signals "tool."
- **Voice:** short, direct, active. "Create project", not "Let's get started." Errors say what happened and what to do next. Buttons keep their verb through the whole flow (a "Publish" button produces a "Published" toast).

## 2. Color

Dark is the default and the design's hero; light is a considered opt‑in (`html.tf-light`). All values live once in `styles.scss`; the table below is the source of truth.

### Surfaces — deep, desaturated blue‑slate, layered by tone (never pure black)

| Token | Dark | Role |
|---|---|---|
| `--mat-sys-surface` | `#0E1014` | app canvas |
| `--mat-sys-surface-container-low` | `#14161D` | cards, panels |
| `--mat-sys-surface-container` | `#171A22` | raised fills |
| `--mat-sys-surface-container-high` | `#1D2029` | menus, hover |
| `--mat-sys-on-surface` | `#E7E9EE` | primary text |
| `--mat-sys-on-surface-variant` | `#9AA2B1` | secondary text / metadata |
| `--mat-sys-outline-variant` (`--tf-hairline`) | `#24272F` | hairline borders |

Depth comes from **tonal layering + 1px hairline borders + a quiet shadow**, not heavy drop shadows.

### Accent — a single cool iris

`--mat-sys-primary: #8B93FF` (dark) / `#4B53D9` (light). One accent, used with restraint: primary actions, active nav, focus rings, selection. A whisper of iris glow (`--tf-ambient`, and the localized glow on the auth hero) provides atmosphere — nowhere else. **Do not introduce a second brand accent.**

### Semantic status / priority (icon + label — never color alone, §8)

`--tf-status-todo / -in-progress / -done`, `--tf-priority-high / -medium / -low`, and the 8 `--tf-tag-*` project hues. Dark values are tuned to clear WCAG AA as label text on the dark surfaces; light values use darker shades for AA on light cards.

## 3. Typography

Self‑hosted variable fonts (no CDN), bundled via `angular.json`:

| Role | Face | Token | Use |
|---|---|---|---|
| Display | **Space Grotesk** | `--tf-font-display` / `.tf-display` | wordmark, page titles, dialog titles. Tracking `-0.02em`. Used with restraint. |
| Body / UI | **Inter** | `--tf-font-body` | everything Material renders; default body |
| Metadata | **JetBrains Mono** | `--tf-font-mono` / `.tf-mono` | ids, counts, dates, due dates, keyboard hints; the `.tf-eyebrow` label |

Use the Material type scale for sizing (`--mat-sys-headline-*`, `body-*`, `label-*`). Weights: 400 / 500 / 600 / 700. Give the display face a memorable role; don't set body copy in it.

## 4. Spacing, Layout & Shape

- **8px grid.** Margins, paddings, gaps are multiples of 8 (4px allowed for icon‑to‑label gaps).
- Page container `.tf-page`: max‑width 1200px, centered, 24px padding (16px under 600px), with a 220ms enter (fade + 6px rise).
- App shell: slim (60px) translucent app bar with a hairline underline; an 92px nav rail on desktop that becomes bottom navigation under 600px. Only the content region scrolls.
- **Shape is tighter than stock M3** (tools, not toys): corners 4 / 6 / 8 / 12 / 16px; **buttons are refined rectangles (6px), not pills**. Cards/panels use `--mat-sys-corner-large` (12px).
- `.tf-surface` is the reusable panel: `surface-container-low` fill + 1px hairline + 12px radius.

## 5. Depth & Elevation

- Cards read as **hairline surfaces**: tonal fill + 1px `--tf-hairline` + `--tf-shadow-1`. Menus and dialogs add a hairline edge and `--tf-shadow-2`.
- Elevation is communicated by **tone and border**, not big shadows. Raise on interaction subtly (background tone shift), not by floating.

## 6. Motion

Motion is functional and calm. Keep durations short (100–260ms) with a confident ease (`cubic-bezier(0.2, 0.7, 0.2, 1)`).

| Interaction | Duration |
|---|---|
| Hover / press tone shift | 100–120ms |
| Route / page enter | 220ms (fade + slight rise) |
| Dialog open / close | 260 / 180ms |
| Board card drop | ~250ms |
| Loading skeleton shimmer | 1.4s loop (loading only) |

Respect `prefers-reduced-motion`: the global rule in `styles.scss` collapses animation/transition to near‑instant. No parallax, no attention‑seeking motion.

## 7. Components & Patterns

- Use Angular Material components for everything they cover; re‑theme via tokens, never fork them.
- Forms: `mat-form-field` `outline` appearance everywhere; labels always visible.
- Feedback: snackbar for success (3s, one at a time), inline for validation/action errors, confirm dialog only for destructive actions.
- **Metadata rows** (created date, counts, ids, due dates) use the mono voice (`.tf-mono`) in `--mat-sys-on-surface-variant`.
- Empty states: icon + one sentence + primary action. Loading: skeletons (shimmer) for page loads; a small spinner only for in‑place actions.
- Status/priority render as **icon + text label** via the `--tf-*` tokens; the board reads at a glance by color + shape while never relying on color alone.

## 8. Accessibility (WCAG 2.1 AA — hard requirement)

- Contrast ≥ 4.5:1 for text, ≥ 3:1 for large text / UI, **verified in both dark and light** (dark is the default, so verify it first).
- Never rely on color alone: status, priority, and active state always pair color with icon and/or text and/or a shape cue (e.g. the nav's left accent bar).
- Full keyboard support: logical order, a visible iris focus ring (`--tf-ring`) — never `outline: none` without a replacement; Escape closes dialogs/menus; a skip‑to‑content link in the shell.
- Drag‑and‑drop has a keyboard alternative (the per‑card status menu). Icon‑only controls carry `aria-label`; decorative marks/icons are `aria-hidden`; 44×44px minimum targets.
- Forms link errors with `aria-describedby`; live regions announce snackbars and async state (`aria-busy`, `role="alert"`).

## 9. Anti‑Patterns (do not do)

- No hardcoded colors, fonts, radii, or spacing in component styles — tokens only.
- No second brand accent; no rainbow gradients. The one iris glow is the only ambient effect.
- No pill buttons, no oversized radii, no heavy drop shadows — Focus is precise, not soft.
- No mixing filled/outline form‑field appearances; no custom checkboxes/radios replacing Material's.
- No disabled control without an explanation; no unexplained empty states; one snackbar at a time.
