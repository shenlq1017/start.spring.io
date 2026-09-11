# Phase 2 — China ecosystem dependencies

Added to `start-site/src/main/resources/application.yml` and minimal BuildCustomizers.

## Dependency IDs

| ID | Coordinates | Version / BOM | Group in UI | Notes |
|----|-------------|----------------|-------------|-------|
| `mybatis` | `org.mybatis.spring.boot:mybatis-spring-boot-starter` | 4.0.1 (existing) | SQL | **Unchanged.** Do not remove. |
| `mybatis-plus` | `com.baomidou:mybatis-plus-spring-boot4-starter` | BOM `mybatis-plus` → **3.5.17** | SQL | Boot 4 starter. Customizer removes classic `mybatis` / `mybatis-test` when both selected; also adds `mybatis-plus-jsqlparser` for pagination. |
| `mapstruct` | `org.mapstruct:mapstruct` | **1.6.3** (last Final before 1.7 betas) | Developer Tools | Customizer adds `mapstruct-processor` as `annotationProcessor`. |
| `hutool` | `cn.hutool:hutool-all` | **5.8.47** | China Ecosystem | No customizer. |
| `easyexcel` | `com.alibaba:easyexcel` | **4.0.3** | China Ecosystem | No customizer. |
| `knife4j` | `com.baizhukui:knife4j-openapi3-boot4-spring-boot-starter` | **5.6.0** | Web (after springdoc) | Boot 4 OpenAPI3 starter (`com.baizhukui` community fork). Customizer removes `springdoc-openapi` if both selected. |
| `sa-token` | `cn.dev33:sa-token-spring-boot4-starter` | **1.46.0** | Security | Dedicated Boot 4 starter exists; pin carefully across Boot minor bumps. No customizer. |

## BOM

```yaml
initializr.env.boms.mybatis-plus:
  groupId: com.baomidou
  artifactId: mybatis-plus-bom
  mappings: "[4.0.0,4.3.0-M1)" → 3.5.17
```

## Customizers / wiring

| Package | Config class | Registered in |
|---------|--------------|---------------|
| `...dependency.mybatisplus` | `MyBatisPlusProjectGenerationConfiguration` | `META-INF/spring.factories` |
| `...dependency.mapstruct` | `MapStructProjectGenerationConfiguration` | `META-INF/spring.factories` |
| `...dependency.knife4j` | `Knife4jProjectGenerationConfiguration` | `META-INF/spring.factories` |

## Compatibility ranges

New entries use `[4.0.0,4.3.0-M1)` to cover Boot 4.0–4.2 line (parent is Boot 4.1.x). Classic `mybatis` remains `[4.0.0,4.1.0-M1)` as previously configured.

## How to verify later

1. **Compile site module** (online Maven):
   ```bash
   export JAVA_HOME=/workspace/tools/jdk-21
   cd /workspace/repos/start.spring.io
   ./mvnw -pl start-site -am -DskipTests compile
   ```
2. **Metadata smoke** — start the app and hit `/metadata/client` (or UI) and confirm IDs appear:
   `mybatis-plus`, `mapstruct`, `hutool`, `easyexcel`, `knife4j`, `sa-token`.
3. **Generation checks**:
   - Request deps=`mybatis-plus` → POM has `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`, BOM import, **no** classic `mybatis`.
   - Request deps=`mybatis,mybatis-plus` → classic `mybatis` removed by customizer.
   - Request deps=`mapstruct` → `mapstruct` + `mapstruct-processor` (annotation processor path / Gradle `annotationProcessor`).
   - Request deps=`knife4j,springdoc-openapi` → only Knife4j starter remains.
4. **Runtime** (optional): generate a sample with `web,mybatis-plus,postgresql` and confirm app starts against Boot 4.1.x.

## Version pins rationale

- **MyBatis-Plus 3.5.17**: latest Maven Central release with `mybatis-plus-spring-boot4-starter` (Boot4 since 3.5.13).
- **MapStruct 1.6.3**: prefer Final over `1.7.0.Beta2`.
- **Knife4j 5.6.0**: `knife4j-openapi3-boot4-spring-boot-starter` on Maven Central under `com.baizhukui`.
- **Sa-Token 1.46.0**: first line with explicit `sa-token-spring-boot4-starter`.
- **Hutool / EasyExcel**: latest stable at time of wiring; not Boot-coupled.

## Out of scope / follow-ups

- No `git push`.
- Sa-Token reactor / WebFlux starter not added (WebMVC Boot4 only).
- MyBatis-Plus test starter not auto-added (can mirror `MyBatisTestBuildCustomizer` later).

## Compile result (2026-09-11)

```bash
JAVA_HOME=/workspace/tools/jdk-21 ./mvnw -pl start-site -am -DskipTests compile
```

**BUILD SUCCESS** after applying `spring-javaformat:apply` and adding `@author` tags required by Checkstyle.
