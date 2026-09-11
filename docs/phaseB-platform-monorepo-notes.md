# Phase B — Platform Monorepo generation (MVP)

Date: 2026-09-11 (Asia/Shanghai). Local sandbox only; no CloudAgent / git push.

## Approach

Same pattern as Phase 3 `ddd-six-module`:

1. Marker dependency `platform-monorepo` in UI group **Project Structure**.
2. `PlatformMonorepoBuildCustomizer` removes the marker from the build model.
3. `PlatformMonorepoProjectContributor` (`Ordered.LOWEST_PRECEDENCE`) deletes root `src/`, overwrites root `pom.xml` with a BOM aggregator, and writes `common/` / `starters/` / `services/` / `gateway/` / `deploy/` from classpath templates under `templates/platform-monorepo/`.
4. Mutual exclusion: `PlatformMonorepoProjectDescriptionCustomizer` removes `ddd-six-module` when `platform-monorepo` is present (platform wins).

Maven-only (`@ConditionalOnBuildSystem(MavenBuildSystem.ID)`).

## Dependency ID

| ID | UI group | Effect |
|----|----------|--------|
| `platform-monorepo` | Project Structure | Triggers platform Monorepo layout; marker removed from POM |
| (interaction) | | If both selected, `ddd-six-module` is stripped from the description |

## Tree produced

For artifactId `{artifactId}` (example `platform-parent`):

```
{artifactId}/
├── pom.xml                 # platform parent BOM aggregator
├── README-PLATFORM.md
├── common/
│   ├── pom.xml
│   ├── common-core/
│   ├── common-web/
│   ├── common-mybatis/
│   ├── common-redis/
│   ├── common-cloud/
│   └── common-test/
├── starters/
│   ├── pom.xml
│   ├── common-web-starter/     # empty @AutoConfiguration + AutoConfiguration.imports
│   ├── common-mybatis-starter/
│   └── common-cloud-starter/
├── services/               # empty aggregator + README placeholder
├── gateway/                # Gateway WebFlux starter POM + package-info
└── deploy/
    └── compose.yaml        # postgres / redis / nacos stubs
```

## Files changed

| Path | Role |
|------|------|
| `start-site/.../application.yml` | Metadata entry |
| `start-site/.../META-INF/spring.factories` | Register configuration |
| `start-site/.../dependency/platformmono/*` | Config + customizers + contributor |
| `start-site/.../project/ProjectDescriptionCustomizerConfiguration.java` | Register mutual-exclusion customizer |
| `start-site/.../templates/platform-monorepo/*` | POM / Java / compose templates |
| `start-site/.../platformmono/PlatformMonorepoProjectContributorTests.java` | Offline unit tests |
| `scripts/smoke-platform-monorepo.sh` | Compile + focused test |
| `docs/phaseB-platform-monorepo-notes.md` | This file |
| `docs/STATUS.md` | Phase 0–3 + B summary |

## How to verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
cd /workspace/repos/start.spring.io

./mvnw -pl start-site -am -DskipTests compile
./mvnw -pl start-site -Dtest=PlatformMonorepoProjectContributorTests -Dsurefire.failIfNoSpecifiedTests=false test
# or: ./scripts/smoke-platform-monorepo.sh
```

Optional zip smoke (server on 18080):

```bash
curl -sS 'http://localhost:18080/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=platform-parent&groupId=com.example&artifactId=platform-parent&name=platform-parent&packageName=com.example.platform&javaVersion=21&dependencies=platform-monorepo' -o /tmp/platform.zip
unzip -l /tmp/platform.zip | head -80
```

## Gaps / follow-ups

- No full PageResult / BusinessException / JsonbTypeHandler Java sources (package-info stubs only); starters are empty `@AutoConfiguration`.
- `services/` has no sample six-module service (use `ddd-six-module` separately or scaffold skill).
- Gateway has no Application main / routes — POM + package-info only.
- SCA / Sentinel / RocketMQ not wired beyond BOM import property.
- Gradle unsupported.
- User-selected Initializr deps are not merged into platform child POMs (fixed templates).
- Full e2e zip smoke against running server: optional / not required for this phase.

## No git push

Local sandbox only.

## Live zip E2E (2026-09-11)

Rebuilt `start-site` (jar had been older than Phase B sources), ran on **port 18081**, downloaded `dependencies=platform-monorepo`, unpacked to `/workspace/verify/platform-zip`.

**Result: PASS** — reactor layout matches MVP tree (`common/*`, `starters/*`, `gateway`, `deploy/compose.yaml`, `services`); metadata contains `platform-monorepo`. Details in `docs/phase4-e2e-verify.md` (section “Live E2E — platform-monorepo”).
