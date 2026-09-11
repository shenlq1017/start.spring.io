# Phase P11 — Explore tree indent + concrete Services + smoke zip refresh

Date: 2026-09-11 (Asia/Shanghai)

## Scope

1. **Explore tree indentation / hierarchy** — children under deep packages (e.g. `…/application/{advice,assembler,config,controller,service}`) looked unindented; expand felt chaotic; two `service` folders looked like sibling roots.
2. **Prefer concrete Service classes** — no Interface + `impl/` for application Query/Application services.
3. **Upgrade committed smoke zips** that drifted from the current generator.

## 1) Explore tree (start-client)

| Cause | Fix |
|-------|-----|
| CSS only defined `.level-0`…`.level-7`; DDD trees reach depth 8+ under `application` | SCSS `@for $i from 0 through 24` with `$range: 16px`; `Tree.js` sets inline `marginLeft: depth * 16 + 6` |
| Sort used folder names still carrying trailing `/` | Normalize display name before sort; dirs first, then files; `localeCompare` |
| Direct-child filter used `split('/')` length quirks | `isDirectChild` via `filter(Boolean)` segments; skip empty relativePath; dedupe by path |
| `findRoot` fragile when zip omitted dir entries / lacked single wrapper | Prefer unique top-level segment; skip `META-INF`; synthesize missing dir nodes in `createTree` |
| Two `service` folders | **Not a flatten bug**: `…/application/service` (CRUD) vs `…/domain/service` (package-info placeholder) vs `src/test/…/service` — distinct paths; indent makes hierarchy obvious |

Self-verify: `Zip.js` unit test `keeps DDD hierarchy: dirs first, nested service not flattened` builds an enhanced-style zip tree and asserts paths / sort / no duplicate path collapse.

## 2) Concrete Services (generator)

- `UserApplicationService` was already a concrete `@Service`.
- `UserQueryService` is now a **concrete `@Service`** (ReadMapper CQRS body inlined). **Removed** `UserQueryService` interface + `service/impl/UserQueryServiceImpl`.
- Controller still injects `UserQueryService` / `UserApplicationService` concrete types.
- Feign unchanged (HTTP API, not app-service interfaces).
- Behavior preserved: CRUD, ReadMapper `Page.of` / `selectSummaryPage`, Snowflake, EasyExcel when enabled (P10).

## 3) Smoke zips

Replaced (same fresh wrapped content):

- `docs/p11-smoke-demo-service.zip` (canonical P11)
- `docs/p10-smoke-demo-service.zip`
- `docs/p8-smoke-demo-service.zip`

Each zip has root folder `demo-service/` + explicit directory entries (Explore-friendly). No Order/Activity sample zips were present under `docs/`.

## Verify

```bash
(cd start-client && yarn test --testPathPattern='Zip|ApiUtils' --watchAll=false)
./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddEnterpriseSmokeIT,DddSixModuleProjectContributorTests test
# Generated: /tmp/p8-smoke-demo → mvn -DskipTests package SUCCESS
# Zip: docs/p11-smoke-demo-service.zip (also refreshed p8/p10)
```

## Files

- Client: `Zip.js`, `Tree.js`, `explore.scss`, `__tests__/Zip.js`
- Generator: `DddCrudSliceGenerator.java`, tests, `readme.mustache`
- Docs: this file, `STATUS.md`, smoke zips
