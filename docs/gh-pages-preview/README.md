# GH Pages preview (UI-only static bundle)

**Updated:** 2026-09-11 (Asia/Shanghai). Local sandbox staging only — **not published**.

## What this is

A copy of `start-client/public/` (Phase 1 webpack production build) ready to publish as GitHub Pages later.

- Includes `index.html`, hashed JS/CSS/fonts, and a `404.html` (= `index.html`) for SPA-ish client routing on Pages.
- **UI only.** Project generation / `/starter.zip` / `/metadata/client` need the Java `start-site` backend. Opening this folder (or Pages) alone will show the form but downloads and live dependency metadata will fail unless an API is configured/proxied.

## How to publish later (deferred — human)

When you return to a machine with GitHub auth:

1. Prefer publishing **from this folder** (`docs/gh-pages-preview/`) via repo Settings → Pages → Deploy from a branch/folder, **or**
2. Copy/push contents to a `gh-pages` branch and enable Pages on that branch.
3. Optionally point the UI at a live Initializr API (env/config) if the fork supports a remote metadata base URL.

Do **not** expect zip generation from static hosting alone.

## Rebuild if sources change

```bash
export PATH="$(npm prefix -g)/bin:$PATH"
cd start-client
yarn install   # if needed
yarn build     # or the project's prod webpack script
# then re-copy public/ → docs/gh-pages-preview/
```

## Local preview

Serve the folder with any static server, e.g. `npx serve docs/gh-pages-preview` — still UI-only without the Java backend on another port.
