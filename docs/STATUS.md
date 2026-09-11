# STATUS — start.spring.io China ecosystem fork (local sandbox)

Updated: 2026-09-11 ~16:30 Asia/Shanghai (UTC+8). **P3 on `main`** (local only; no CloudAgent).

## Phase summary

| Phase | Topic | Status | Notes doc |
|-------|-------|--------|-----------|
| 0 | Sandbox / tooling / clone | Done | JDK 21: `/workspace/tools/jdk-21` |
| 1–4 / B | UI, deps, DDD, platform, E2E | Done | prior docs |
| **P0** | zh-CN + Architecture + Explore | Done | `docs/phase-p0-notes.md` |
| **P1/P2** | Template kit + multi-entity CRUD | Done | `docs/phase-p1-notes.md` |
| **P3** | Deps i18n, layout, Entities UX, gen gaps | **Done** | `docs/phase-p3-notes.md` |

## Project Structure markers

| id | Generates |
|----|-----------|
| `ddd-six-module` | Six-module DDD microservice aggregator |
| `platform-monorepo` | Platform BOM + common + starters + services + gateway + deploy |

## Next for the user (Windows)

1. `git pull` on Windows clone
2. `./mvnw clean install -DskipTests` then `java -jar start-site/target/start-site-exec.jar --application.offline=true`
3. Verify: Architecture → Template → Metadata → Entities (bottom) → Explore → GENERATE
4. Confirm zip has `{artifact}-bootstrap/.../*Application.java` and entity `*Controller.java` for `ddd-enhanced`
