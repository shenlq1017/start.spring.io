# Phase 3 — DDD six-module microservice generation (MVP)

## Approach

Initializr always emits a **single-module** tree. For multi-module DDD we:

1. Expose a **marker dependency** `ddd-six-module` (group **Project Structure**) — same pattern as `native` (coordinates point at `spring-boot` but are removed from the POM).
2. `DddSixModuleBuildCustomizer` removes that marker from the build model.
3. `DddSixModuleProjectContributor` (`Ordered.LOWEST_PRECEDENCE`) **deletes** root `src/`, **overwrites** root `pom.xml` with a Maven aggregator, and writes six child modules from classpath templates under `templates/ddd-six/` (simple `{{placeholder}}` substitution — no Jinja/Python at runtime).

Maven-only (`@ConditionalOnBuildSystem(MavenBuildSystem.ID)`). Gradle is not supported in this MVP.

## Dependency ID

| ID | UI group | Effect |
|----|----------|--------|
| `ddd-six-module` | Project Structure | Triggers six-module layout; marker removed from POM |

## Modules produced

For artifactId `{svc}` (example `demo-service`):

```
{svc}/
├── pom.xml                          # aggregator (spring-boot-starter-parent)
├── README-DDD.md
├── {svc}-contract/
├── {svc}-feign-client/
├── {svc}-domain/
├── {svc}-infrastructure/
├── {svc}-application/
└── {svc}-bootstrap/                 # Application main + application.yml + ArchUnit stub
```

Standalone skeleton (not nested under platform monorepo). Platform `common-*` / starters are **out of scope** (Phase B); domain depends on `jspecify` only; infrastructure uses MyBatis-Plus Boot4 starter directly.

## Files changed

| Path | Role |
|------|------|
| `start-site/.../application.yml` | Metadata entry |
| `start-site/.../META-INF/spring.factories` | Register `DddSixModuleProjectGenerationConfiguration` |
| `start-site/.../dependency/dddsix/*` | Config + customizer + contributor |
| `start-site/.../templates/ddd-six/*` | POM / Java / yml templates |
| `start-site/.../dddsix/DddSixModuleProjectContributorTests.java` | Offline unit test |
| `scripts/smoke-ddd-six.sh` | Compile + focused test |

## How to verify later (with running server)

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
cd /workspace/repos/start.spring.io

# Compile
./mvnw -pl start-site -am -DskipTests compile

# Focused unit test (no network required for contributor itself)
./mvnw -pl start-site -Dtest=DddSixModuleProjectContributorTests test
# or: ./scripts/smoke-ddd-six.sh

# Full server (optional)
./mvnw -pl start-site spring-boot:run
# Then:
curl -s 'http://localhost:8080/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=demo-service&groupId=com.example&artifactId=demo-service&name=demo-service&packageName=com.example.demo&javaVersion=21&dependencies=ddd-six-module' -o /tmp/ddd.zip
unzip -l /tmp/ddd.zip | head -80
```

Confirm zip contains six `*-*/pom.xml` modules and bootstrap `*Application.java`.

## Gaps / follow-ups

- **Full CRUD vertical slice** (entity → SQL → contract → domain → infra → controller): not generated; packages are `package-info` placeholders only.
- **Platform monorepo** (common/starters/gateway/deploy): Phase B — parent here is Boot parent, not `platform-parent`.
- **Nacos / Spring Cloud Alibaba**: not wired in bootstrap POM (comment / Phase B).
- **Gradle**: unsupported for this option.
- **UI**: metadata-only; optional `Extend.json` weight not required.
- Selecting other Initializr deps with `ddd-six-module` still customizes the *discarded* single-module build model; multi-module POMs are fixed templates (user-selected deps are not merged into child modules yet).

## No git push

Local sandbox only; do not push or consume CloudAgent quota for this phase.

## Example tree (contributor output for artifactId=`demo-service`)

```
demo-service/README-DDD.md
demo-service/demo-service-application
demo-service/demo-service-application/pom.xml
demo-service/demo-service-application/src
demo-service/demo-service-application/src/main
demo-service/demo-service-application/src/main/java
demo-service/demo-service-application/src/main/java/com
demo-service/demo-service-application/src/main/java/com/example
demo-service/demo-service-application/src/main/java/com/example/demo
demo-service/demo-service-application/src/main/java/com/example/demo/application
demo-service/demo-service-application/src/main/java/com/example/demo/application/advice
demo-service/demo-service-application/src/main/java/com/example/demo/application/advice/package-info.java
demo-service/demo-service-application/src/main/java/com/example/demo/application/assembler
demo-service/demo-service-application/src/main/java/com/example/demo/application/assembler/package-info.java
demo-service/demo-service-application/src/main/java/com/example/demo/application/config
demo-service/demo-service-application/src/main/java/com/example/demo/application/config/package-info.java
demo-service/demo-service-application/src/main/java/com/example/demo/application/controller
demo-service/demo-service-application/src/main/java/com/example/demo/application/controller/package-info.java
demo-service/demo-service-application/src/main/java/com/example/demo/application/service
demo-service/demo-service-application/src/main/java/com/example/demo/application/service/package-info.java
demo-service/demo-service-bootstrap
demo-service/demo-service-bootstrap/pom.xml
demo-service/demo-service-bootstrap/src
demo-service/demo-service-bootstrap/src/main
demo-service/demo-service-bootstrap/src/main/java
demo-service/demo-service-bootstrap/src/main/java/com
demo-service/demo-service-bootstrap/src/main/java/com/example
demo-service/demo-service-bootstrap/src/main/java/com/example/demo
demo-service/demo-service-bootstrap/src/main/java/com/example/demo/DemoServiceApplication.java
demo-service/demo-service-bootstrap/src/main/resources
demo-service/demo-service-bootstrap/src/main/resources/application.yml
demo-service/demo-service-bootstrap/src/main/resources/db
demo-service/demo-service-bootstrap/src/main/resources/db/migration
demo-service/demo-service-bootstrap/src/main/resources/db/migration/.gitkeep
demo-service/demo-service-bootstrap/src/test
demo-service/demo-service-bootstrap/src/test/java
demo-service/demo-service-bootstrap/src/test/java/com
demo-service/demo-service-bootstrap/src/test/java/com/example
demo-service/demo-service-bootstrap/src/test/java/com/example/demo
demo-service/demo-service-bootstrap/src/test/java/com/example/demo/ModuleDependencyTest.java
demo-service/demo-service-contract
demo-service/demo-service-contract/pom.xml
demo-service/demo-service-contract/src
demo-service/demo-service-contract/src/main
demo-service/demo-service-contract/src/main/java
demo-service/demo-service-contract/src/main/java/com
demo-service/demo-service-contract/src/main/java/com/example
demo-service/demo-service-contract/src/main/java/com/example/demo
demo-service/demo-service-contract/src/main/java/com/example/demo/contract
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/constant
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/constant/package-info.java
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/dto
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/dto/request
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/dto/request/package-info.java
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/dto/response
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/dto/response/package-info.java
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/enums
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/enums/package-info.java
demo-service/demo-service-contract/src/main/java/com/example/demo/contract/package-info.java
demo-service/demo-service-domain
demo-service/demo-service-domain/pom.xml
demo-service/demo-service-domain/src
demo-service/demo-service-domain/src/main
demo-service/demo-service-domain/src/main/java
demo-service/demo-service-domain/src/main/java/com
demo-service/demo-service-domain/src/main/java/com/example
demo-service/demo-service-domain/src/main/java/com/example/demo
demo-service/demo-service-domain/src/main/java/com/example/demo/domain
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/command
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/command/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/event
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/event/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/exception
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/exception/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/model
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/model/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/query
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/query/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/repository
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/repository/package-info.java
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/service
demo-service/demo-service-domain/src/main/java/com/example/demo/domain/service/package-info.java
demo-service/demo-service-feign-client
demo-service/demo-service-feign-client/pom.xml
demo-service/demo-service-feign-client/src
demo-service/demo-service-feign-client/src/main
demo-service/demo-service-feign-client/src/main/java
demo-service/demo-service-feign-client/src/main/java/com
demo-service/demo-service-feign-client/src/main/java/com/example
demo-service/demo-service-feign-client/src/main/java/com/example/demo
demo-service/demo-service-feign-client/src/main/java/com/example/demo/feign
demo-service/demo-service-feign-client/src/main/java/com/example/demo/feign/package-info.java
demo-service/demo-service-infrastructure
demo-service/demo-service-infrastructure/pom.xml
demo-service/demo-service-infrastructure/src
demo-service/demo-service-infrastructure/src/main
demo-service/demo-service-infrastructure/src/main/java
demo-service/demo-service-infrastructure/src/main/java/com
demo-service/demo-service-infrastructure/src/main/java/com/example
demo-service/demo-service-infrastructure/src/main/java/com/example/demo
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/config
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/config/package-info.java
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/converter
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/converter/package-info.java
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/entity
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/entity/package-info.java
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/mapper
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/mapper/package-info.java
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/repository
demo-service/demo-service-infrastructure/src/main/java/com/example/demo/infrastructure/persistence/repository/package-info.java
demo-service/demo-service-infrastructure/src/main/resources
demo-service/demo-service-infrastructure/src/main/resources/mapper
demo-service/demo-service-infrastructure/src/main/resources/mapper/.gitkeep
demo-service/pom.xml
```
