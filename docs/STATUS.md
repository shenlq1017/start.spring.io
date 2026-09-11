# STATUS — start.spring.io China ecosystem fork (local sandbox)

Updated: 2026-09-11 ~17:00 Asia/Shanghai (UTC+8). **P5 on `main`** (local only; no CloudAgent).

## Phase summary

| Phase | Topic | Status | Notes doc |
|-------|-------|--------|-----------|
| 0 | Sandbox / tooling / clone | Done | JDK 21: `/workspace/tools/jdk-21` |
| 1–4 / B | UI, deps, DDD, platform, E2E | Done | prior docs |
| **P0** | zh-CN + Architecture + Explore | Done | `docs/phase-p0-notes.md` |
| **P1/P2** | Template kit + multi-entity CRUD | Done | `docs/phase-p1-notes.md` |
| **P3** | Deps i18n, layout, Entities UX, gen gaps | Done | `docs/phase-p3-notes.md` |
| **P4** | Divider/widths, jitter, metadata density, packaging=jar, Entities Explore-editor | **Done** | `docs/phase-p4-notes.md` |
| **P5** | Entities polish, home divider left ~55%, Control alignment, no single-app spacer hole | **Done** | `docs/phase-p5-notes.md` |

## Project Structure markers

| id | Generates |
|----|-----------|
| `ddd-six-module` | Six-module DDD microservice aggregator |
| `platform-monorepo` | Platform BOM + common + starters + services + gateway + deploy |

## Next for the user (Windows)

1. `git pull` on Windows clone
2. `./mvnw clean install -DskipTests` then `java -jar start-site/target/start-site-exec.jar --application.offline=true`
3. Verify: divider ~mid-left; metadata label/value aligned; single-app has no blank hole under fields
4. Entities list: 编辑/移除 tight to name; open editor → no 添加依赖 floater; 返回/ESC; green checkboxes; rounded panes
5. Confirm `packaging=jar`; Explore debounce + entities JSON still work
