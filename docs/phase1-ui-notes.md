# Phase 1 — Start.spring.io UI refresh (start-client)

Date: 2026-09-11 (Asia/Shanghai)

## Summary

Same-stack visual refresh of the React + webpack Initializr client to the
**Dark Mode OLED** design system in `design-system/startspringio/MASTER.md`
(slate `#0F172A`, accent green `#22C55E`, IBM Plex Sans + JetBrains Mono).
Existing JS behavior is unchanged (metadata fetch, dependency dialog, explore
zip, download, favorites, history, share, theme toggle).

## Changed files

### Design tokens & styles
- `start-client/src/styles/_variables.scss` — palette, fonts, radii, spacing, shadows aligned to MASTER.md
- `start-client/src/styles/_tokens.scss` **(new)** — CSS custom properties (`--color-*`, `--space-*`, `--shadow-*`) + `prefers-reduced-motion`
- `start-client/src/styles/_modern.scss` **(new)** — additive UI refresh: primary Generate CTA, chips, dialog, header, focus/hover/cursor, dark OLED refinements
- `start-client/src/styles/_mixins.scss` — stronger focus-visible rings
- `start-client/src/styles/_fonts.scss` — Metropolis kept as offline fallback; Google Fonts loaded from HTML
- `start-client/src/styles/_main.scss` — minor radius / nav color tweaks
- `start-client/src/styles/app.scss` — wires `tokens` + `modern` into the cascade

### Theme / entry
- `start-client/src/components/utils/Theme.js` — dark-first default when no `springtheme` in localStorage (still respects explicit user choice + light system preference)
- `start-client/src/components/reducer/App.js` — `defaultAppContext.theme` → `'dark'`
- `start-client/static/index.html` — Google Fonts preconnect/link, early theme script, OLED FOUC background
- `start-client/webpack.common.js` — PWA/`theme-color` → `#22C55E`, background `#0F172A`

### Notes
- `docs/phase1-ui-notes.md` — this file

## Static build output (GH Pages–ready)

Webpack production `output.path` is:

```
start-client/public/
```

`webpack.prod.js` sets `publicPath: './'` so relative asset URLs work on GitHub Pages.
Entry HTML: `start-client/public/index.html`.

Copy or publish the entire `start-client/public/` folder for a static deploy.
(API calls still need a backend or proxy — UI-only export.)

## How to run locally

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$(npm prefix -g)/bin:$PATH"
cd /workspace/repos/start.spring.io/start-client
yarn install
yarn start          # webpack-dev-server
# or
yarn build          # tests + prod build → public/
# skip tests if needed:
yarn webpack --mode production --config webpack.prod.js
```

## Build result (this run)

- `yarn build`: **success**
- Jest: **5 suites / 53 tests passed**
- Webpack 5.110.3 compiled with size-limit warnings only (pre-existing bundle size)
- Design tokens verified in bundle (`#22c55e`, `#0f172a`, `--color-accent`, font families)

## Design checklist (phase 1)

| Item | Status |
|------|--------|
| Dark OLED slate + green accent | Done (tokens + modern layer) |
| Light theme still works via Theme.js | Done (light tokens retained) |
| IBM Plex Sans + JetBrains Mono | Done (Google Fonts + Metropolis fallback) |
| Primary Generate button green CTA | Done (`.button.primary`) |
| Chips `white-space: nowrap` + ellipsis | Done |
| `cursor: pointer` on clickables | Done |
| 150–300ms transitions | Done (`0.15s` / `0.2s` / `0.3s`) |
| Visible focus / `focus-visible` | Done |
| `prefers-reduced-motion` | Done in `_tokens.scss` |
| No emoji icons | Unchanged (existing SVG icons) |
| React + webpack stack preserved | Yes |

## Remaining gaps

1. **Full component redesign** — phase 1 is token + overlay refresh; builder “cards” are visual polish, not a new layout system.
2. **Explore / favorites / history / share SCSS** (`explore.scss`, `favorite.scss`, `history.scss`, `share.scss`) still use older radii/colors in places; they inherit dark tokens but were not fully restyled.
3. **Self-host fonts** — Google Fonts CDN is used; for fully offline GH Pages, vendor woff2 under `src/fonts/` and swap the `<link>`.
4. **Chinese copy** — chip nowrap/ellipsis is ready; longer CN labels in the form may still need label-column width tweaks in `_responsive.scss`.
5. **Backend for static export** — `public/` is UI-only; metadata/generate endpoints need the Spring app or a mock.
6. **Light-mode secondary button tokens** in MASTER.md (`border: #1E293B`) are partially mapped; light secondary buttons still use foreground outline style from the original Initializr.
7. **No git push** performed (per task constraints).

## Not touched (intentionally)

- Vite rewrite / stack change
- CloudAgent / Cursor quota APIs
- GitHub push / `gh` auth
- JS feature logic (Initializr reducers, zip explore, etc.)
