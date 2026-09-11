# STATUS — start.spring.io China ecosystem fork (local sandbox)

Updated: 2026-09-11 ~22:30 Asia/Shanghai (UTC+8). **P10: CRUD P1+P2 + Explore/Generate parity** on `main` (local only; no CloudAgent).

## Phase summary

| Phase | Topic | Status | Notes doc |
|-------|-------|--------|-----------|
| 0 | Sandbox / tooling / clone | Done | JDK 21: `/workspace/tools/jdk-21` |
| 1–4 / B | UI, deps, DDD, platform, E2E | Done | prior docs |
| **P0** | zh-CN + Architecture + Explore | Done | `docs/phase-p0-notes.md` |
| **P1/P2** | Template kit + multi-entity CRUD | Done | `docs/phase-p1-notes.md` |
| **P3** | Deps i18n, layout, Entities UX, gen gaps | Done | `docs/phase-p3-notes.md` |
| **P4** | Divider/widths, jitter, metadata density, packaging=jar, Entities Explore-editor | Done | `docs/phase-p4-notes.md` |
| **P5** | Entities polish, home divider left ~55%, Control alignment, no single-app spacer hole | Done | `docs/phase-p5-notes.md` |
| **P6** | Add→select entity, 中文描述 order, Swagger labels, deps column, radio min-width, list actions right, button borders, dark mode | Done | `docs/phase-p6-notes.md` |
| **P7** | Java radios one line, template→selected deps kits, dark mode thorough pass | Done | `docs/phase-p7-notes.md` |
| **P8** | Config/Java gap, real DDD CRUD + swagger + module URLs, H2 smoke | Done | `docs/phase-p8-notes.md` |
| **P9** | CRUD review (zh) + pagination P0 | Done | `docs/gen-code-review-zh.md` |
| **P10** | **All P1+P2 CRUD + Explore/Generate zip parity** | **Done** | `docs/phase-p10-notes.md` |

## Project Structure markers

| id | Generates |
|----|-----------|
| `ddd-six-module` | Six-module DDD microservice aggregator |
| `platform-monorepo` | Platform BOM + common + starters + services + gateway + deploy |

## Template kits (UI selected deps)

| Template | Selected deps (right panel) |
|----------|-----------------------------|
| `ddd-standard` | web, validation, mybatis-plus, postgresql, flyway, knife4j |
| `ddd-enhanced` | + **easyexcel** |
| `platform-standard` | web, validation, mybatis-plus, postgresql, knife4j |
| `platform-enhanced` | + flyway |

## Next for the user (Windows)

1. `git pull` on Windows clone
2. `./mvnw clean install -DskipTests` then `java -jar start-site/target/start-site-exec.jar --application.offline=true`
3. 企业DDD增强 + User → Explore tree **must match** Generate zip (ReadMapper, Snowflake, `@Validated` page, `contract.common.page.PageResult`)
4. Review: `docs/gen-code-review-zh.md`, `docs/phase-p10-notes.md`; smoke zip `docs/p10-smoke-demo-service.zip`
