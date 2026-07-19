# TaskFlow — Frontend

Angular 22 SPA for TaskFlow, built with Tailwind CSS v4 + spartan-ng (headless components on Angular CDK) and Lucide icons.
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

The "Focus" theme lives in `src/tailwind.css`: Tailwind v4 (CSS-first, processed
natively by the Angular build — no PostCSS config) plus the shadcn design tokens
(`--background`, `--foreground`, `--primary`, `--card`, `--border`, `--radius`, …)
and our `--tf-*` semantic tokens (status/priority + the 8 project colorTag hues).
Dark-first: `core/layout/theme.store.ts` toggles `.dark` on `<html>` (light is the
token default); the choice is persisted in `localStorage` and applied pre-boot by a
small script in `index.html` to avoid a flash. spartan-ng "helm" components are
vendored under `src/app/ui/` (shadcn model — generated into the repo) and styled
with these tokens.

## Third-party dependencies (RULES.md §27)

- `@fontsource-variable/{inter,space-grotesk,jetbrains-mono}` — self-host the body,
  display, and mono faces per DESIGN.md §3 (no CDN); bundled via the angular.json `styles` array.
- `@spartan-ng/brain` + `@spartan-ng/cli` — headless CDK-based primitives and the
  generator for the helm components in `src/app/ui/`.
- `@ng-icons/core` + `@ng-icons/lucide` — Lucide icon set; the app's icons are
  registered once in `shared/icons.ts` and provided at the app root.
- `ngx-sonner` — the toast library behind the single `<hlm-toaster>` in the shell.
- `clsx` / `tailwind-merge` / `class-variance-authority` / `tw-animate-css` — utilities the helm components depend on.
- `eslint-config-prettier` — keeps ESLint from fighting Prettier formatting.

## Testing notes

Unit tests run headlessly on vitest + jsdom (Angular CLI default runner).
`src/test-setup.ts` replaces Node's built-in non-functional `localStorage`
global with an in-memory `Storage` implementation.
