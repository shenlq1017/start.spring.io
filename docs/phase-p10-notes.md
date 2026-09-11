# Phase P10 — CRUD P1+P2 + Explore/Generate parity

Date: 2026-09-11 (Asia/Shanghai)

## Scope

1. **All remaining P1 + P2** from `docs/gen-code-review-zh.md` implemented in `DddCrudSliceGenerator` / DDD templates (not doc-only).
2. **Explore vs Generate zip parity** (high priority bug).

## Explore ↔ Generate parity (cause + fix)

| Cause | Fix |
|-------|-----|
| Explore debounce signature only watched `architecture\|template\|entities` — **deps / meta / boot / java / packageName** changes left a **stale blob** while Generate fetched fresh | Expand signature to include deps + meta/boot/language/java/artifact/packageName/description |
| Explore kept old **tree** while `blob=null` during refresh (looked like different content) | Clear tree → Loading when blob null while open |
| `getProject` / `getShareUrl` duplicated template/entities encoding (drift risk) | Shared `buildTemplateEntitiesQuery` + `buildStarterQuery` used by Generate/Explore |
| Native `<form>` submit lacked `preventDefault` (race with AJAX Generate) | `Form.js` always `preventDefault` then call handler |

Client tests: `Explore/Generate query parity` in `ApiUtils.js` asserts template/entities/arch marker shared.

## P1 delivered

| Item | Implementation |
|------|----------------|
| ReadMapper CQRS | `*ReadMapper` + XML; `QueryServiceImpl` uses `Page.of` + `selectSummaryPage` / `selectDetailById`; writes stay on domain Repository |
| PageResult placement | `contract.common.page.PageResult` (module-local common; README documents) |
| Snowflake | `contract.common.id.SnowflakeIdGenerator` + `@Bean`; ApplicationService `idGenerator.nextId()`; PO `IdType.ASSIGN_ID` |
| Soft delete | Flyway `BOOLEAN deleted DEFAULT FALSE` + `@TableLogic Boolean` + MP global-config true/false |
| application.yml / Flyway | COMMENT ON columns; H2 profile docs; TIMESTAMPTZ→TIMESTAMP mapping; default vs `h2` profile in README |
| Testability | `*ApplicationServiceTest` with fake repo; ArchUnit ban on `com.fasterxml.jackson` + domain↛infra |

## P2 delivered

| Item | Implementation |
|------|----------------|
| Swagger/validation | `page(@Validated Query…)`; QueryRequest `@Min`/`@Max` |
| StatusEnum | Chinese `description` + `getDescription()` |
| Import/export | EasyExcel skeleton (ExcelRow + read/write stream) when import\|export on; `easyexcel` in parent BOM + application pom + ddd-enhanced kit |
| GlobalExceptionHandler | `BusinessException` mapping + validation ProblemDetail |
| Jackson3 | README + ArchUnit ban `com.fasterxml.jackson` (no JSONB handler unless needed) |
| Feign | Methods mirror Controller (page/detail/create/update/delete) |

## Verify

```bash
./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddEnterpriseSmokeIT,DddSixModuleProjectContributorTests test
(cd start-client && yarn test --testPathPattern='ApiUtils' --watchAll=false)
# Generated User enhanced: /tmp/p10-smoke-demo → mvn -DskipTests package SUCCESS
# Zip: docs/p10-smoke-demo-service.zip
```

## Files (main)

- Generator: `DddCrudSliceGenerator.java`, `DddSixModuleProjectContributor.java`
- Templates: `application-pom`, `parent-pom`, `application-yml`, `readme`, `archunit-test`, `feign-client-pom`
- Client: `ApiUtils.js`, `Application.js`, `Explore.js`, `Form.js`, `Template.js` (+ tests)
- Docs: this file, `gen-code-review-zh.md`, `STATUS.md`

## Superseded smoke zip

P11 refreshed `docs/p10-smoke-demo-service.zip` (concrete QueryService + Explore-friendly root). See `docs/phase-p11-notes.md` / `docs/p11-smoke-demo-service.zip`.
