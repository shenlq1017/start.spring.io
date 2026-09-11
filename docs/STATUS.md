# STATUS — start.spring.io China ecosystem fork (local sandbox)

Updated: 2026-09-11 ~15:00 Asia/Shanghai (UTC+8). **P1/P2 on `main` as `c628317`** (local only; no CloudAgent).

## Phase summary

| Phase | Topic | Status | Notes doc |
|-------|-------|--------|-----------|
| 0 | Sandbox / tooling / clone | Done | JDK 21: `/workspace/tools/jdk-21` |
| 1 | UI / client packaging | Done | `docs/phase1-ui-notes.md` |
| 2 | China / ecosystem deps (MyBatis-Plus, Hutool, …) | Done | `docs/phase2-deps-notes.md` |
| 3 | DDD six-module generator (`ddd-six-module`) | Done | `docs/phase3-multimodule-notes.md` |
| 4 | E2E verify (server + single + ddd-six zips) | Done (PASS) | `docs/phase4-e2e-verify.md` |
| **B** | **Platform Monorepo (`platform-monorepo`)** | **Done (MVP) + live zip E2E PASS** | `docs/phaseB-platform-monorepo-notes.md` |
| Preview | GH Pages static UI bundle (staged, not published) | Ready locally | `docs/gh-pages-preview/` |
| **P0** | **zh-CN i18n + Architecture radio + Explore sort/refresh** | **Done** | `docs/phase-p0-notes.md` |
| **P1** | **Template kit + multi-entity CRUD + Explore debounce** | **Done** | `docs/phase-p1-notes.md` |
| **P2** | **Polish / docs / package / smoke** | **Done** | (included in P1 notes) |

## Project Structure markers

| id | Generates |
|----|-----------|
| `ddd-six-module` | Six-module DDD microservice aggregator |
| `platform-monorepo` | Platform BOM + common + starters + services + gateway + deploy |

**Mutual exclusion:** if both selected, `PlatformMonorepoProjectDescriptionCustomizer` removes `ddd-six-module` (platform wins).

## Latest live E2E (platform-monorepo @ 18081)

- Rebuilt `start-site` (`./mvnw -pl start-site -am -DskipTests package`) — prior exec jar lacked Phase B classes.
- Server: `java -jar start-site/target/start-site-exec.jar --server.port=18081` → `/` and `/metadata/client` **200**.
- Metadata: `platform-monorepo` under **Project Structure**.
- Zip → `/workspace/verify/platform-monorepo.zip` → unpack `/workspace/verify/platform-zip`: **PASS** (`common/*`, `starters/*`, `gateway`, `deploy/compose.yaml`, no root `src/`, packaging=pom).
- Server stopped; port 18081 free.

Earlier Phase 4 (port 18080): single-module + `ddd-six-module` also **PASS** (see phase4 doc).

## GH Pages preview (local only)

- Path: **`docs/gh-pages-preview/`** (copy of `start-client/public/`; `src` not newer than `public/`, no yarn rebuild needed).
- Includes `index.html`, assets, `404.html` (= index for SPA-ish routing), and `README.md`.
- **UI-only** — API/download needs Java `start-site` backend.

## Next for the user (Windows)

1. `git pull` on `C:\project\develop\start.spring.io`
2. `./mvnw clean install -DskipTests` then `java -jar start-site/target/start-site-exec.jar --application.offline=true`
3. **Full verify** now: Architecture → Template → Entities → Explore → GENERATE (enhanced vs standard)
4. Optional: refresh GH Pages static bundle from a fresh `start-client` build

## Quick verify (Phase B unit + optional server)

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
cd /workspace/repos/start.spring.io
./mvnw -pl start-site -am -DskipTests compile
./mvnw -pl start-site -Dtest=PlatformMonorepoProjectContributorTests,DddSixModuleProjectContributorTests -Dsurefire.failIfNoSpecifiedTests=false test
# live: java -jar start-site/target/start-site-exec.jar --server.port=18081
```
