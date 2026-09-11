# Phase P3 — Dependencies i18n, layout, Entities upgrade, generation gaps

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **Dependencies Chinese** — `start-client/src/i18n/deps-zh.js` maps common / China-ecosystem / major Spring dependency **names & descriptions** (and group titles). Applied in `ApiUtils.getLists` so List / Dialog / search all show Chinese with English fallback.
2. **Narrower right panel** — `_modern.scss` `.colset-main` ≈ **60/40** (58/42 @ ≥1280px); stacks on narrow screens. Architecture radios stay **13px**.
3. **Fields reorder** — Boot → Architecture → Template → **Project Metadata** (Group / Artifact / **Name** / **Description** / Package / Packaging / Configuration / Java) → **Entities last**.
4. **EntitiesPanel upgrade** — denser Initializr-like controls; collapsible `<details>`; per-entity name/table/description/db/orm + entity swagger toggle; per-field name/type/required/unique/**description**/**swagger**; API flags unchanged. Serialization includes new fields.
5. **Generation gaps (critical)** — hardened `GenerationRequestAttributesFilter` (broader URL match + request attribute backup + double-decode), `isDddEnhanced()` resilient default, CRUD `@Schema` when swagger on, platform sample README lists configured entities. Always emit bootstrap `*Application.java` (already) + per-entity controllers when enhanced.

## Entity JSON schema (P3)

```json
[{
  "name": "User",
  "table": "sys_user",
  "db": "postgresql",
  "orm": "mybatis-plus",
  "description": "用户",
  "swagger": true,
  "fields": [
    {"name": "username", "type": "String", "required": true, "unique": true, "description": "用户名", "swagger": true}
  ],
  "apis": {"create": true, "detail": true, "page": true, "update": true, "delete": true, "import": false, "export": false}
}]
```

## Sample paths (artifact `demo-service`, package `com.example.demo`)

- `demo-service-bootstrap/.../DemoServiceApplication.java`
- `demo-service-bootstrap/.../db/migration/V1__01_create_sys_user.sql`
- `demo-service-application/.../controller/UserController.java`
- `demo-service-contract/.../dto/request/CreateUserRequest.java` (with `@Schema` when swagger)

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
(cd start-client && yarn test)
./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddSixModuleProjectContributorTests,PlatformMonorepoProjectContributorTests -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl start-client,start-site -am -DskipTests package
# smoke offline on a free port — assert Application + UserController in zip
```

## Build / verify result (this run)

- `yarn test`: **5 suites / 53 tests passed**
- `./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddSixModuleProjectContributorTests,PlatformMonorepoProjectContributorTests … test`: **5 tests passed**
- `./mvnw -pl start-client,start-site -am -DskipTests package`: **BUILD SUCCESS**
- Smoke (`start-site-exec.jar` @ 18085, offline):
  - `template=ddd-enhanced` + User entities → `DemoServiceApplication.java` + `UserController.java` + Flyway + `@Schema` on CreateUserRequest — **PASS**
  - `template=platform-enhanced` → sample-service README lists configured entities — **PASS**
- Server stopped after smoke.
