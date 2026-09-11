# Phase P1 — Template kit + multi-entity CRUD slices

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **Template kit** (original Control / Radio style) under Architecture:
   - `arch-ddd-service` → `ddd-standard` / `ddd-enhanced` (default enhanced when entities present)
   - `arch-platform` → `platform-standard` / `platform-enhanced`
   - `arch-single` → template control hidden
2. **Multi-entity panel** (collapsible `<details>`, not a new card skin) when `arch-ddd-service` (and `platform-enhanced`):
   - Default entity: User / `sys_user` / postgresql / mybatis-plus
   - Editable fields (name, type, required, unique) + API flags including **import** / **export**
   - Button 添加实体
3. **API contract** on generate / share / Explore:
   - `template=<id>`
   - `entities=<url-encoded JSON>`
4. **Backend**: `GenerationRequestAttributesFilter` → ThreadLocal → DDD / platform contributors
5. **CRUD generation** (`ddd-enhanced` or non-empty entities without `ddd-standard`): Flyway SQL, contract DTOs, domain, infrastructure PO/Mapper/Repo, application Controller/Service; import/export stubs when flagged
6. **Explore** debounce also watches `template` + `entities`
7. Chinese strings for new panels (P2 polish included)

## API contract

### `template`

| Value | Meaning |
|-------|---------|
| `ddd-standard` | Six-module skeleton only |
| `ddd-enhanced` | Skeleton + per-entity vertical CRUD slices |
| `platform-standard` | Platform monorepo skeleton |
| `platform-enhanced` | Platform + `services/sample-service` placeholder README |

### `entities` (JSON array, URL-encoded)

```json
[{
  "name": "User",
  "table": "sys_user",
  "db": "postgresql",
  "orm": "mybatis-plus",
  "description": "用户",
  "fields": [
    {"name": "username", "type": "String", "required": true, "unique": true},
    {"name": "email", "type": "String", "required": false, "unique": false}
  ],
  "apis": {
    "create": true, "detail": true, "page": true, "update": true,
    "delete": true, "import": false, "export": false
  }
}]
```

Wired via `GenerationRequestAttributesFilter` on `/starter.zip` and `/starter.tgz`.

## Sample generated paths (artifact `demo-service`, package `com.example.demo`, entity User)

- `demo-service-bootstrap/src/main/resources/db/migration/V1__01_create_sys_user.sql`
- `demo-service-contract/.../contract/dto/request/CreateUserRequest.java`
- `demo-service-domain/.../domain/model/User.java`
- `demo-service-infrastructure/.../persistence/entity/UserPO.java`
- `demo-service-application/.../controller/UserController.java`
- `demo-service-application/.../service/UserApplicationService.java`
- (+ ImportExportService when import/export checked)

## UI fields

| Control | `values.*` | Notes |
|---------|------------|-------|
| Architecture | `architecture` | P0 |
| Template | `template` | Hidden for single-app |
| Entities panel | `entities[]` | Shown for DDD service |

## Files touched (high level)

- Client: `Template.js`, `Entities.js`, `EntitiesPanel.js`, `Fields.js`, `Initializr.js`, `ApiUtils.js`, `Application.js`, `zh.js`, `_modern.scss`
- Site: `GenerationRequestAttributes(+Filter)`, `DddCrudSliceGenerator`, DDD/Platform contributors, `StartApplication` filter bean
- Docs: this file + `STATUS.md`

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
cd /workspace/repos/start.spring.io
# client
(cd start-client && yarn test && yarn webpack --mode production --config webpack.prod.js)
# backend
./mvnw -pl start-client,start-site -am -DskipTests package
./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddSixModuleProjectContributorTests -Dsurefire.failIfNoSpecifiedTests=false test
```

## Build / verify result (this run)

- `yarn test`: **5 suites / 53 tests passed**
- `yarn webpack --mode production`: **success** (pre-existing size-limit warnings)
- `./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddSixModuleProjectContributorTests,PlatformMonorepoProjectContributorTests … test`: **5 tests passed**
- `./mvnw -pl start-client,start-site -am -DskipTests package`: **BUILD SUCCESS**
- Smoke (`start-site-exec.jar` @ 18083, offline):
  - `template=ddd-enhanced` + `entities=[User…]` → zip contains User CRUD paths (Flyway, Controller, domain, PO, ImportExport) — **PASS**
  - `template=ddd-standard` → **0** User files — **PASS**
- Server stopped after smoke.
