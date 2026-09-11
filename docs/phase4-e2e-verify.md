# Phase 4 — End-to-end verify (local sandbox)

Date: 2026-09-11 (Asia/Shanghai). Local sandbox only; no CloudAgent / git push.

## Environment

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$(npm prefix -g)/bin:$PATH"
cd /workspace/repos/start.spring.io
```

- JDK: Temurin 21.0.12.1+1
- Port: **18080**
- Client assets: `start-client` already packaged (`public/` → jar → `start-site` Maven dependency); no separate webpack rebuild required for this run.

## Build

```bash
./mvnw -pl start-site -am -DskipTests package
```

**Result:** PASS (incremental; `start-site/target/start-site-exec.jar` present).

## Start server

```bash
java -jar start-site/target/start-site-exec.jar --server.port=18080
```

Waited until Tomcat started; then:

| URL | HTTP |
|-----|------|
| `http://localhost:18080/` | 200 |
| `http://localhost:18080/metadata/client` | 200 |

**Result:** PASS — server started and responded.

## Metadata spot-check

From `/metadata/client` (211 deps total):

| id | group | name | found |
|----|-------|------|-------|
| `ddd-six-module` | Project Structure | DDD Six-Module Microservice | YES |
| `mybatis-plus` | SQL | MyBatis-Plus | YES |
| `hutool` | China Ecosystem | Hutool | YES |

**Result:** PASS

## Zip 1 — normal single-module (no ddd-six)

```bash
curl -sS 'http://localhost:18080/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=demo-service&groupId=com.example&artifactId=demo-service&name=demo-service&packageName=com.example.demo&javaVersion=21&dependencies=web,mybatis-plus' \
  -o /workspace/verify/single.zip
unzip -q /workspace/verify/single.zip -d /workspace/verify/single
```

- Zip size: ~16 KB (15942 bytes); zip listing 27 entries; 10 files on disk
- Layout: classic single-module with root `src/` and `DemoServiceApplication.java`
- POM includes `spring-boot-starter-webmvc` + MyBatis-Plus BOM / starters

Tree summary:
```

demo-service/
├── pom.xml                 # jar (single-module)
├── src/main/java/.../DemoServiceApplication.java
├── src/main/resources/application.properties
├── src/test/java/.../DemoServiceApplicationTests.java
├── mvnw, mvnw.cmd, .mvn/, HELP.md, .gitignore

```

**Result:** PASS

## Zip 2 — ddd-six-module multi-module

```bash
curl -sS 'http://localhost:18080/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=demo-service&groupId=com.example&artifactId=demo-service&name=demo-service&packageName=com.example.demo&javaVersion=21&dependencies=ddd-six-module,mybatis-plus' \
  -o /workspace/verify/ddd-six.zip
unzip -q /workspace/verify/ddd-six.zip -d /workspace/verify/ddd-six
```

- Zip size: ~56 KB (55906 bytes); zip listing 125 entries; 42 files on disk
- Root `src/` **absent** (contributor deleted it)
- Aggregator `pom.xml`: `packaging=pom`, six `<module>` entries
- Marker `ddd-six-module` **not** present in any generated POM (only mentioned in `README-DDD.md`)
- Bootstrap entry: `demo-service-bootstrap/.../DemoServiceApplication.java`

Tree summary:
```

demo-service/
├── pom.xml                 # packaging=pom aggregator (6 modules)
├── README-DDD.md
├── demo-service-contract/
├── demo-service-feign-client/
├── demo-service-domain/
├── demo-service-infrastructure/
├── demo-service-application/
├── demo-service-bootstrap/  # DemoServiceApplication.java + application.yml
├── mvnw, mvnw.cmd, .mvn/, HELP.md
(no root src/)

```

Module POMs found (7):
- `demo-service/pom.xml` (aggregator)
- `demo-service-contract/pom.xml`
- `demo-service-feign-client/pom.xml`
- `demo-service-domain/pom.xml`
- `demo-service-infrastructure/pom.xml`
- `demo-service-application/pom.xml`
- `demo-service-bootstrap/pom.xml`

**Result:** PASS

## Optional Maven on generated DDD project

```bash
cd /workspace/verify/ddd-six/demo-service
./mvnw -N validate          # exit 0
./mvnw -pl demo-service-bootstrap -am -DskipTests compile   # exit 0
```

**Result:** PASS (deps resolved; bootstrap module compiled successfully in this environment).

## Stop server

Killed `java -jar start-site-exec.jar` process; port 18080 free afterward.

**Result:** PASS

## Screenshots

None captured (CLI verify only). Unpacked trees live under:

- `/workspace/verify/single/`
- `/workspace/verify/ddd-six/`
- zips: `/workspace/verify/single.zip`, `/workspace/verify/ddd-six.zip`

## Overall

| Check | Status |
|-------|--------|
| Package start-site | PASS |
| Server start / respond | PASS |
| Metadata ids (mybatis-plus, ddd-six-module, hutool) | PASS |
| Single-module zip layout | PASS |
| DDD six-module zip + aggregator | PASS |
| Marker removed from POMs | PASS |
| mvn validate / bootstrap compile | PASS |
| Server stop | PASS |

**Verdict: PASS** — Phase 4 e2e success criteria met. No failures.


## STATUS addendum (Phase B)

Phase B adds marker `platform-monorepo` (Project Structure). See `docs/phaseB-platform-monorepo-notes.md` and `docs/STATUS.md`. Mutual exclusion: platform removes `ddd-six-module` when both are requested. Phase 4 e2e above did **not** re-run zip smoke for platform (contributor unit test covers layout).

## Live E2E — platform-monorepo (port 18081)

Date: 2026-09-11 ~12:07 Asia/Shanghai (UTC+8). Local sandbox only.

### Rebuild

`start-site-exec.jar` was **stale** vs Phase B sources (jar 03:58 UTC; platform sources ~04:02–04:05 UTC). Rebuilt:

```bash
./mvnw -pl start-site -am -DskipTests package
```

Jar then contained `BOOT-INF/classes/.../platformmono/*` and `templates/platform-monorepo/*`.

### Server

```bash
java -jar start-site/target/start-site-exec.jar --server.port=18081
```

| URL | HTTP |
|-----|------|
| `http://localhost:18081/` | 200 |
| `http://localhost:18081/metadata/client` | 200 |

### Metadata spot-check

`platform-monorepo` present under group **Project Structure**, name **Platform Monorepo**. (`ddd-six-module` still present.)

### Zip — platform-monorepo

```bash
curl -sS 'http://localhost:18081/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=platform-parent&groupId=com.example&artifactId=platform-parent&name=platform-parent&packageName=com.example.platform&javaVersion=21&dependencies=platform-monorepo' \
  -o /workspace/verify/platform-monorepo.zip
unzip -q /workspace/verify/platform-monorepo.zip -d /workspace/verify/platform-zip
```

- HTTP 200; zip ~62 KB (62271 bytes); ~147 zip entries; 39 files on disk
- Unpacked: `/workspace/verify/platform-zip/platform-parent/`

| Check | Status |
|-------|--------|
| `common/*` (core, web, mybatis, redis, cloud, test) | PASS |
| `starters/*` (web, mybatis, cloud starters) | PASS |
| `gateway/` | PASS |
| `deploy/compose.yaml` (542 bytes) | PASS |
| `services/` | PASS |
| Root `pom.xml` packaging=pom; modules common/starters/services/gateway | PASS |
| No root `src/` | PASS |
| Marker `platform-monorepo` absent from root POM | PASS |
| `README-PLATFORM.md` | PASS |

### Stop server

Killed Java process; port **18081** free afterward.

**Verdict: PASS** — live platform-monorepo zip E2E succeeded.
