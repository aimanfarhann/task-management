# TaskFlow — Frontend

Angular 22 + Angular Material SPA for TaskFlow (M1: auth, projects, member management).
See `../context/` for the binding PRD, DESIGN, ARCHITECTURE, SCHEMA, and RULES documents.

## Commands

```bash
npm start          # dev server on http://localhost:4200, /api proxied to http://localhost:8080
npm run lint       # ESLint (angular-eslint flat config + eslint-config-prettier)
npm test           # vitest (jsdom, headless, single run)
npm run build      # production build to dist/taskflow
npm run format     # Prettier over src/**/*.{ts,html,scss}
```

## Structure (ARCHITECTURE.md §4.1)

```
src/app/
├── core/        # singletons: auth store/interceptor/guards, typed API clients, layout shell
├── features/    # auth (login/register), projects (list, detail, dialogs)
└── shared/      # dumb reusable UI: confirm-dialog, empty-state, color-dot, skeleton
```

State is signal stores only (no NgRx). Components never call HttpClient directly;
typed API clients live in `core/api/`.

## Theming

Material 3 theme generated from seed `#4355B9` (`mat.define-theme` with
`use-system-variables`); palettes in `src/theme/_theme-colors.scss` come from
`ng generate @angular/material:theme-color`. Light/dark schemes: default follows
`prefers-color-scheme`, manual toggle persisted in `localStorage`
(`core/layout/theme.store.ts` toggles `.tf-dark` on `<html>`). Semantic
status/priority colors and the 8 project colorTag hues are CSS custom
properties in `src/styles.scss` with dark variants.

## Third-party dependencies (RULES.md §27)

- `@fontsource/roboto` — self-hosts Roboto per DESIGN.md §3 (no CDN); bundled via
  the angular.json `styles` array.
- `material-icons` — self-hosts the Material Icons ligature font `<mat-icon>` uses
  by default, replacing the Google Fonts CDN link that `ng add @angular/material` inserts.
- `eslint-config-prettier` — keeps ESLint from fighting Prettier formatting.

## Testing notes

Unit tests run headlessly on vitest + jsdom (Angular CLI default runner).
`src/test-setup.ts` replaces Node's built-in non-functional `localStorage`
global with an in-memory `Storage` implementation.
