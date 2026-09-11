# Phase P4 — UI density, divider, packaging, Entities editor

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **Divider / column widths** — `.colset-main` flex split **65–68% left / 32–35% right**; background divider moved from `center` to **65–68%** so the gutter tracks the split. Inner Project|Language gutter tightened. At ≥1600px (covers 1920×1080), Project/Boot/Architecture/Template radio rows use `flex-wrap: nowrap`.
2. **Architecture switch jitter** — Template and Entities live in **reserved slots** (`min-height` spacers when hidden) so show/hide does not thrash Metadata position. Entities home list is short, which further reduces jump.
3. **Project Metadata density** — smaller label width (72px), 13px inputs/radios, less left margin; matches Project/Language density.
4. **Packaging / Config / Java** — Packaging control **removed** from UI. State + `getProject` / `getShareUrl` **always force `jar`** (never war). Config format + Java sit on **one row** (`.colset-meta-row`).
5. **Entities UX** — Homepage: **name list only** (chip rows + add/remove/edit). Full editor is an **Explore-like overlay** (`EntitiesEditor`, same `explorer` CSSTransition). Interior uses home Initializr controls (underline inputs, primary links) — no separate dark card skin. Multi-entity tabs in the left rail; Done/ESC closes. Edits write through `values.entities`; Explore debounce / GENERATE still send `entities=` JSON (swagger/description included).

## CSS split values

| Viewport | Left | Right | Divider `background-position` |
|----------|------|-------|-------------------------------|
| default  | 68%  | 32%   | 68% |
| ≥1280px  | 66%  | 34%   | 66% |
| ≥1600px  | 65%  | 35%   | 65% |
| ≤1000px  | stack | stack | none |

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
(cd start-client && yarn test)
./mvnw -pl start-client,start-site -am -DskipTests package
```

## Gaps / follow-ups

- Focus trap in EntitiesEditor is Escape + body-scroll-lock only (same as Explore; Explore has no full focus-trap library).
- Reserved Template/Entities slot heights are fixed minima; very tall entity lists still grow the page when the editor is closed (list-only home keeps this small).
- Packaging still appears in metadata API lists; UI simply never exposes or sends war.
