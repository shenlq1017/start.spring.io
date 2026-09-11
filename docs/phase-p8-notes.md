# Phase P8 — Config|Java gap, enterprise CRUD generation quality

Date: 2026-09-11 (Asia/Shanghai)

## Scope

1. **UI** — Tighten whitespace **between** 配置格式 and Java in `.colset-meta-row`: left column `flex: 0 0 auto` (no 40% gap), smaller gap/padding; Java radios stay `nowrap` for 4 versions.
2. **Swagger** — When entity `swagger` on: Controllers get `@Tag` / `@Operation` / `@Parameter`; DTOs keep `@Schema` (+ validation). springdoc already on application/contract POMs; UI kit still selects knife4j.
3. **Real services** — `*ApplicationService` + `*QueryService`/`*QueryServiceImpl` with real create/detail/page/update/delete via repository + assembler/converter. Import/export stubs return JSON maps (compile, no TODO / UnsupportedOperationException).
4. **URL convention** — `/{prefix}/v1/{resource}` (spring-boot-gen guided). `prefix` = artifact without `-service`/`-svc` (else last package segment). Example: artifact `demo-service` + entity User → **`/demo/v1/users`**.
5. **Enterprise smoke** — Generated zip under `docs/p8-smoke-demo-service.zip`. `mvn -DskipTests package` **SUCCESS**. Runtime with `--spring.profiles.active=h2`: create/detail/page/update/delete all succeeded against `/demo/v1/users`.

### spring-boot-gen reference (guidance only, not 1:1)

Aligned pragmatically: six-module layering, PageResult (no R/ApiResponse), Problem Details advice, Assembler, Converter, Feign client, Flyway SQL + `application-h2.yml`/`schema-h2.sql` for local smoke. Not a verbatim j2 copy.

## URL / sample paths

| Artifact | Entity | ApiPath.BASE |
|----------|--------|--------------|
| demo-service | User | `/demo/v1/users` |
| order-service | Order | `/order/v1/orders` |
| activity-service | Activity | `/activity/v1/activities` |

## Enterprise checklist

| Check | Result |
|-------|--------|
| `*Application` present | Pass |
| Controller → Service → Mapper/PO wired | Pass |
| DTOs + `@NotBlank`/`@Size` / `@Schema` | Pass |
| PageResult (no global R wrapper) | Pass |
| application.yml PG placeholders + Flyway | Pass |
| Flyway migration matches entity | Pass |
| No TODO / empty services | Pass |
| Feign client generated | Pass |
| `mvn -DskipTests package` on generated | **Pass** |
| H2 profile CRUD HTTP | **Pass** (create/detail/page/update/delete) |

## Files

- Client: `start-client/src/styles/_modern.scss`
- Generator: `DddCrudSliceGenerator.java`, contributor appendix, `bootstrap-pom` (jdbc, starter-flyway, h2), `application-pom` (spring-tx)
- Tests: `DddCrudSliceGeneratorTests`, `DddEnterpriseSmokeIT`
- Docs: this file, `STATUS.md`, `docs/p8-smoke-demo-service.zip`

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
(cd start-client && yarn test)
./mvnw -pl start-site -Dtest=DddCrudSliceGeneratorTests,DddEnterpriseSmokeIT -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl start-client,start-site -am -DskipTests package
```
