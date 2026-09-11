# Phase P0 — Chinese i18n + Architecture radio + Explore polish

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **zh-CN i18n** on the existing builder (`Fields.js` / Dependency / Loading / Dialog help), via `start-client/src/i18n/zh.js` + `t()`.
2. **Architecture** radio between Spring Boot and Project Metadata (no redesign):
   - `arch-single` → 单应用 (default) — no marker deps
   - `arch-ddd-service` → 业务微服务 → injects `ddd-six-module`
   - `arch-platform` → 平台工程 → injects `platform-monorepo`
3. Markers **hidden** from Dependencies picker / selected list; ids kept in metadata for generation.
4. Explore: fresh zip on click; **debounced regenerate** when architecture changes while panel open.
5. Explore tree sort: **directories first**, then files; `localeCompare` within each group (`Zip.js`).
6. Light质感 SCSS only (primary button shadow, selected radio ring) — Metropolis / existing grid kept.
7. `application.yml` display names for markers → 业务微服务 / 平台工程 (ids unchanged).

## Not in P0

- Multi-entity CRUD generator UI (P1)
- Template kit dropdown (skip / P1)

## Architecture → dependency map

| Radio value | Label | `dependencies=` on generate/explore |
|-------------|-------|-------------------------------------|
| `arch-single` | 单应用 | neither marker |
| `arch-ddd-service` | 业务微服务 | `ddd-six-module` |
| `arch-platform` | 平台工程 | `platform-monorepo` |

Implemented in `applyArchitectureDependencies()` (`Architecture.js`), used by `getProject` and `getShareUrl`. Share/URL restore: marker in deps → derive architecture and strip markers from `values.dependencies`.

## Explore sort

`createTree` children sort: folder before file; then `filename.localeCompare`.

## Files touched

- `start-client/src/i18n/zh.js` **(new)**
- `start-client/src/components/utils/Architecture.js` **(new)**
- `start-client/src/components/common/builder/Fields.js`
- `start-client/src/components/common/builder/Loading.js`
- `start-client/src/components/common/dependency/{Dependency,List,Dialog}.js`
- `start-client/src/components/reducer/Initializr.js`
- `start-client/src/components/utils/ApiUtils.js`
- `start-client/src/components/utils/Zip.js`
- `start-client/src/components/Application.js`
- `start-client/src/styles/_modern.scss`
- `start-site/src/main/resources/application.yml`
- `docs/phase-p0-notes.md` **(this file)**

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$(npm prefix -g)/bin:$PATH"
cd /workspace/repos/start.spring.io/start-client
yarn test
# optional: yarn webpack --mode production --config webpack.prod.js
```

Optional smoke (if `start-site-exec.jar` present): metadata still lists marker ids; `starter.zip?dependencies=ddd-six-module` / `platform-monorepo` still generate.

## Build / verify result (this run)

- `yarn test`: **5 suites / 53 tests passed**
- `yarn webpack --mode production --config webpack.prod.js`: **success** (pre-existing size-limit warnings only)
- Smoke (`start-site-exec.jar` @ 18082): metadata still exposes marker ids `ddd-six-module` / `platform-monorepo`; `starter.zip?dependencies=ddd-six-module` → zip OK (~52KB). Display names in **source** `application.yml` are Chinese; packaged jar still has English until next `./mvnw -pl start-site -am -DskipTests package`.
