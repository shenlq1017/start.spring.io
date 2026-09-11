---
name: "spring-boot-gen"
description: "企业级 Spring Boot 4 微服务代码生成：脚手架/工程骨架、六模块 DDD 分层、Flyway 建表 SQL、CRUD 垂直切片（DTO/契约→领域/聚合→PO/Mapper→Controller/Service）、Feign 契约。搭建微服务工程、生成实体/Mapper/Service/Controller/建表脚本或接口代码时使用。"
---

# 企业级 Spring Boot 代码生成

> 面向企业级 Spring Boot 微服务（Java 21 + Spring Boot 4.1 + Spring Cloud 2025.1 + Spring Cloud Alibaba + MyBatis-Plus + PostgreSQL 17）的代码生成，采用六模块 DDD 分层。
> **所有生成物必须符合本文与模板中的架构约束**；规则冲突时以本文为准，禁止自行发明替代方案。

## 使用方式

```
/spring-boot-gen scaffold  - 搭建骨架（平台 Monorepo / 单个六模块微服务，二选一）
/spring-boot-gen service   - 在已有平台下新增业务微服务（六模块聚合）
/spring-boot-gen sql       - 生成 Flyway 迁移 SQL（PostgreSQL）
/spring-boot-gen domain    - 生成领域层对象（聚合/值对象/命令/事件/状态机/仓储接口）
/spring-boot-gen contract  - 生成契约 DTO/常量/枚举 + Feign Client + Fallback
/spring-boot-gen crud      - 生成聚合垂直全切片（SQL → 契约 → 领域 → 基础设施 → 应用层）
/spring-boot-gen api       - 生成单个接口
/spring-boot-gen           - 自动模式（读取技术方案文档，按切片顺序生成）
```

### 脚本一键生成（推荐：更快、更省 token）

本技能内置 Python 脚手架脚本 `scaffold/scaffold.py`（Jinja2 模板渲染），可直接落地工程骨架与聚合切片，比逐文件手写更快、更省 token。生成后按文末「自检清单」核对即可。

```bash
# 1. 初始化平台 Monorepo（common/starters/gateway/deploy + 平台 POM）
python scaffold/scaffold.py init --base-package com.enterprise --target .

# 2. 新增业务微服务（六模块骨架）
python scaffold/scaffold.py service --name user-service --prefix user --description 用户 --base-package com.enterprise --target .

# 3. 生成某聚合垂直全切片（SQL → 契约 → 领域 → 基础设施 → 应用层）
python scaffold/scaffold.py crud --service user-service --prefix user --entity User --base-package com.enterprise --target .
```

| 命令 | 作用 | 参数（`--target` 默认 `.`） |
|------|------|------|
| `init` | 平台 Monorepo 骨架 | `--base-package`（默认 `com.enterprise`） |
| `service` | 业务微服务六模块骨架 | `--name`、`--prefix`、`--base-package`（必填）；`--description`（默认「业务」） |
| `crud` | 聚合垂直全切片 | `--service`、`--prefix`、`--base-package`（必填）；`--entity`（默认 `User`） |

**扩展新聚合**：在 `scaffold.py` 的 `ENTITIES` 字典中增加条目（字段 `name/java/sql/comment/required/max/unique`，状态 `statuses/status_desc/transitions`），再执行 `crud` 即可，无需改模板。

**脚本边界**：脚本覆盖「标准 CRUD 切片 + 六模块骨架 + 平台静态文件」的确定性部分；业务规则（领域行为、Saga/Outbox 编排）、跨聚合复杂设计、非标准接口仍用下文的 `/spring-boot-gen domain|contract|api` 手动生成。

---

## 技术基线（强制）

| 组件 | 版本 | 关键约束 |
|------|------|----------|
| JDK | **Java 21 (LTS)** | 虚拟线程默认开启；禁止 `synchronized`（Pinning 风险），用 `ReentrantLock` |
| Spring Boot | **4.1.1** | BOM：`spring-boot-dependencies` |
| Spring Cloud | **2025.1.0 (Oakwood)** | BOM：`spring-cloud-dependencies` |
| Spring Cloud Alibaba | **2025.1.0.0** | Nacos 3.1.1 / Sentinel / RocketMQ 5.3.1 |
| MyBatis-Plus | **3.5.15+** | artifactId 必须是 **`mybatis-plus-spring-boot4-starter`**，且分页插件需单独引入 `mybatis-plus-jsqlparser` |
| PostgreSQL | **17.x** | 每服务独立 Database，禁止跨服务 JOIN |
| Flyway | 由 SB BOM 管理（12.x） | 脚本放 `{svc}-bootstrap/src/main/resources/db/migration` |
| SpringDoc OpenAPI | **3.1.0** | 接口注解 `@Tag`/`@Operation`/`@Schema` |
| Jackson | **3（`tools.jackson`）** | 禁止 import `com.fasterxml.jackson.*` |
| 可观测性 | Micrometer Tracing + OTel | `spring-boot-micrometer-tracing-opentelemetry` |

**Spring Boot 4 迁移红线**：
- JSON 序列化走 Jackson 3，包名 `tools.jackson`；自定义 TypeHandler/序列化器必须基于 Jackson 3
- Nacos 配置用 `spring.config.import`，**禁止** `bootstrap.yml`
- 可空参数/返回值显式标注 `@Nullable`（JSpecify）
- Starter 自动配置类注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

## 架构红线（生成任何代码前必读）

### 服务六模块与依赖方向（编译期强制）

```
{svc}-bootstrap      → {svc}-application, {svc}-infrastructure   （唯一可执行 JAR，唯一含启动类）
{svc}-application    → {svc}-contract, {svc}-domain
{svc}-infrastructure → {svc}-domain, common-mybatis-starter, common-redis-starter
{svc}-domain         → common-core                                （纯 Java，禁止 Spring/MyBatis 注解）
{svc}-feign-client   → {svc}-contract, spring-cloud-openfeign(optional)
{svc}-contract       → 零框架依赖（仅 JDK + 校验/注解）
```

### 绝对禁止项

| 红线 | 说明 |
|------|------|
| 禁止 `ApiResponse`/`R` 全局包装 | 错误统一 Problem Details（RFC 7807），业务错误码放 `type` URI；不使用 `ResponseBodyAdvice` 自动包装 |
| 禁止业务启动类扫描 common 包 | common 能力一律通过 starter `@AutoConfiguration` 装配 |
| 禁止跨服务 JOIN / 共享库表 | 跨服务取数只能走对方 contract + feign-client，经 ACL 防腐层适配 |
| 禁止调用方自定义 Feign 接口 | 契约唯一来源是被调用方的 `{svc}-contract` + `{svc}-feign-client` |
| 禁止在 domain 层出现框架注解 | domain 纯 Java：无 `@Service`/`@Component`/MyBatis 注解 |
| 禁止 `synchronized` | 虚拟线程 Pinning；改 `ReentrantLock`（CI ArchUnit 强制） |
| 契约只增不改 | 废弃字段标 `@Deprecated`，保留至少一个大版本 |
| 禁止 `JacksonTypeHandler` | JSONB 列用 `common-mybatis` 的 `JsonbTypeHandler`（Jackson 3 适配） |

### 命名与主键约定

- 持久化对象：`{Entity}PO`（infrastructure.persistence.entity），与领域模型 `{Entity}`（domain.model）严格分离，经 MapStruct `{Entity}Converter` 互转
- 主键：`String`，Snowflake（`IdType.ASSIGN_ID`），Worker ID 由 Nacos 实例感知分配
- PO 必须带 `@Version` 乐观锁字段
- 表名小写下划线单数；时间列 `TIMESTAMPTZ`；金额 `NUMERIC`

---

## 0. 搭建骨架

### 0a. 平台级 Monorepo（`/spring-boot-gen scaffold`）

**触发时机**：平台首次初始化。团队 ≥15 人或多服务并存时使用。

**目录结构**：

```
platform-parent/
├── pom.xml                  # 平台父 POM（BOM 统一管理）
├── common/                  # common-core / common-web / common-mybatis / common-redis / common-cloud / common-test
├── starters/                # common-web-starter / common-mybatis-starter / common-cloud-starter
├── services/                # 业务微服务组（每个六模块）
├── gateway/                 # Spring Cloud Gateway + Sentinel
└── deploy/                  # docker / k8s / compose.yaml
```

**生成步骤**：
1. 创建 `platform-parent/pom.xml` —— 模板见 [pom-platform.md](templates/pom-platform.md)，BOM 导入顺序：spring-boot-dependencies → spring-cloud-dependencies → spring-cloud-alibaba-dependencies → mybatis-plus-bom
2. 创建 common 六个模块及 POM（依赖严格单向：`*-starter → common-* → common-core`，common 模块间禁止互依赖）
3. 创建三个 starter 及 `@AutoConfiguration` 类 + `AutoConfiguration.imports` 注册文件
4. 创建 gateway 模块（路由 + Sentinel 网关适配 + JWT Resource Server）
5. 创建 `deploy/compose.yaml`（PostgreSQL / Redis / Nacos / RocketMQ / OTel Collector）
6. 每个空 Java 目录放 `package-info.java` 占位，避免聚合构建报依赖解析错误

### 0b. 单个业务微服务（`/spring-boot-gen service`）

**触发时机**：平台已存在，新增业务服务；或独立服务初始化。

**输入参数**：

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| 服务名 | 是 | 小写中划线 | `order-service` |
| 业务前缀 | 是 | 小写，包名用 | `order` |
| 中文描述 | 是 | | `订单服务` |
| 基础包名 | 是 | | `com.enterprise` |

**生成清单**（六模块聚合，POM 模板见 [pom-service.md](templates/pom-service.md)）：

```
{svc}/
├── pom.xml                      # 服务聚合 POM
├── {svc}-contract/              # dto/request, dto/response, constant, enums（零框架依赖）
├── {svc}-feign-client/          # {X}FeignClient + {X}FeignFallbackFactory
├── {svc}-domain/                # model, command, query, event, service, repository, exception
├── {svc}-infrastructure/        # persistence(entity/mapper/repository/converter/handler), cache, mq, outbox, saga, client/adapter, config
├── {svc}-application/           # controller, service(+impl), assembler, advice, config
└── {svc}-bootstrap/             # {X}Application + application.yml + db/migration
```

**生成步骤**：
1. 创建聚合 POM + 六个子模块 POM（按 [pom-service.md](templates/pom-service.md) 依赖矩阵，禁止反向依赖）
2. 各模块按上表建包结构，空包放 `package-info.java`
3. bootstrap 生成启动类与 `application.yml` —— 模板见 [config.md](templates/config.md)
4. 生成 ArchUnit 依赖方向校验测试（放 bootstrap 的 test 目录）

---

## 1. 生成 SQL（`/spring-boot-gen sql`）

**输入参数**：表名（小写下划线）、表描述（中文）、业务字段列表、所属服务。

**强制规范**：
- 文件命名 `V{x.y.z}__{snake_description}.sql`，放 `{svc}-bootstrap/src/main/resources/db/migration/`
- 紧急回滚脚本 `U{x.y.z}__rollback_{desc}.sql` 放 `deploy/scripts/`（前向修复优先）
- 时间列一律 `TIMESTAMPTZ`；主键 `VARCHAR(64)`；乐观锁列 `version INT NOT NULL DEFAULT 0`
- 每个表/列必须 `COMMENT ON`

**平台通用表**（`saga_executions`、`{aggregate}_outbox`、`idempotent_record`、`{aggregate}_audit_log`）直接复用 [sql-reference.md](templates/sql-reference.md) 既有定义。

字段类型、金额/长度建议、索引规范、JSONB GIN 索引：见 [sql-reference.md](templates/sql-reference.md)。

---

## 2. 生成领域层（`/spring-boot-gen domain`）

**输入参数**：聚合名（大驼峰）、业务前缀、中文描述、状态枚举及流转表（如有）、核心行为方法列表。

**生成清单**（模板见 [domain.md](templates/domain.md)）：

```
domain/
├── model/{Entity}.java              # 聚合：私有构造 + 工厂方法 + 行为方法（状态机校验内聚）
├── model/{Xxx}Enum / {Entity}Status # 状态机枚举（TRANSITIONS 表 + validateTransition）
├── model/{ValueObject}.java         # 值对象（record，如 Money）
├── command/Create{Entity}Command.java   # record
├── query/{Entity}Query.java             # record
├── event/{Entity}CreatedEvent.java      # record + occurredAt
├── repository/{Entity}Repository.java   # 仅接口
├── service/{Entity}DomainService.java   # 跨聚合/复杂领域规则
└── exception/{Entity}NotFoundException.java / {Entity}StatusException.java
```

**铁律**：domain 层只依赖 `common-core`，禁止任何 Spring/MyBatis/Jackson 注解；业务规则内聚在聚合行为方法中，禁止把规则写到 application 层。

---

## 3. 生成契约（`/spring-boot-gen contract`）

**输入参数**：服务名、业务前缀、接口清单（方法/路径/请求响应字段）、是否生成 Feign Client。

**生成清单**（模板见 [contract.md](templates/contract.md)）：

```
contract/
└── dto/request/  Create{X}Request, Update{X}Request, Query{X}Request
└── dto/response/ {X}DetailResponse, {X}SummaryResponse
└── constant/     {X}ApiPath, {X}ServiceName
└── enums/        {X}StatusEnum
feign-client/
└── {X}FeignClient.java               # @FeignClient(name = {X}ServiceName.NAME, path = {X}ApiPath.BASE, fallbackFactory = ...)
└── {X}FeignFallbackFactory.java      # 不抛异常，返回 available=false 的降级 Response
```

**铁律**：contract 零框架依赖（只允许 jakarta.validation / springdoc 注解）；Response 含 `available` 布尔字段支撑降级语义；契约只增不改。

---

## 4. 生成 CRUD 垂直切片（`/spring-boot-gen crud`）

**输入参数**：聚合名（大驼峰）、业务前缀（小写）、表名、中文描述、字段列表。

**生成顺序**（自上而下，逐层落模板）：

| 步骤 | 产物 | 模板 |
|------|------|------|
| 1 | Flyway SQL | [sql-reference.md](templates/sql-reference.md) |
| 2 | contract：Request/Response DTO + 枚举 | [contract.md](templates/contract.md) |
| 3 | domain：聚合 + 命令/查询 + 仓储接口 + 领域异常 | [domain.md](templates/domain.md) |
| 4 | infrastructure：`{Entity}PO` + Mapper(+XML 按需) + `{Entity}Converter`(MapStruct) + `{Entity}RepositoryImpl` | [infrastructure.md](templates/infrastructure.md) |
| 5 | application：`{Entity}Controller` + `{Entity}ApplicationService` + `{Entity}QueryService`(读侧 CQRS) + `{Entity}Assembler` | [application.md](templates/application.md) |

**数据访问约定**（与 [infrastructure.md](templates/infrastructure.md) 一致）：
- 单表条件/分页/COUNT：RepositoryImpl 内用 `Wrappers.lambdaQuery()`，禁止堆简单 XML
- 读侧投影（CQRS）：`QueryService` 走 Mapper XML 直接投影 DTO，不经过领域对象
- 仅联表、窗口函数、`INSERT ... ON CONFLICT` 等场景才自定义 XML

---

## 5. 生成单个 API（`/spring-boot-gen api`）

**输入参数**：服务/聚合、HTTP 方法、路径、功能描述、请求/响应字段、读写侧（命令走 ApplicationService / 查询走 QueryService）。

**返回值策略**（严格执行）：

| 接口类型 | 返回方式 |
|----------|----------|
| 标准 REST | 直接返回 DTO / `PageResult<T>`（common-core），无包装 |
| Feign 远程调用 | 直接返回 DTO，失败由 FeignException + 降级处理 |
| 业务错误 | 抛领域异常，`GlobalExceptionHandler` 转 Problem Detail（`type` URI 承载错误码） |
| 结果不固定 | `Map<String, Object>`，显式声明，不得蔓延 |
| 文件/SSE | `ResponseEntity` / `Flux` |

模板见 [application.md](templates/application.md)。

---

## 自动模式

当存在技术方案文档时执行 `/spring-boot-gen`：
1. 读取技术方案，提取服务划分、表结构、接口清单
2. 按 `/service` → `/sql` → `/crud`（逐聚合）→ `/contract`（对外契约）顺序生成
3. 生成完成输出产物清单，并提示：骨架已生成，业务规则（领域行为、Saga 编排）需参照 [infrastructure.md](templates/infrastructure.md) 的 Saga/Outbox 模板补充

---

## 自检清单

- [ ] 六模块依赖方向正确，无反向依赖；启动类只在 bootstrap
- [ ] 未引入 `ApiResponse`/`R` 包装；异常处理为 `ResponseEntityExceptionHandler` + ProblemDetail
- [ ] domain 层零框架注解；业务规则在聚合行为方法内
- [ ] PO 带 `@Version`；JSONB 列用 `JsonbTypeHandler`；无 `com.fasterxml.jackson` import
- [ ] MyBatis-Plus 依赖为 `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`；分页插件最后注册且指定 `DbType.POSTGRE_SQL`
- [ ] 单表查询在 RepositoryImpl 用 lambdaQuery，XML 仅承载复杂 SQL / 读侧投影
- [ ] Flyway 脚本 `V` 命名规范，含 COMMENT ON，时间列 TIMESTAMPTZ
- [ ] 契约只增不改；Response 含 `available` 字段；Feign 指定 `fallbackFactory`
- [ ] Nacos 用 `spring.config.import`；`spring.mvc.problemdetails.enabled: true`
- [ ] 代码无 `synchronized`；时间字段类型与 SQL 一致（`OffsetDateTime`）
- [ ] 每个字段有 `@Schema` 描述；必填有校验注解

---

## 详细模板文件

| 模板 | 文件 | 说明 |
|------|------|------|
| 平台 POM | [pom-platform.md](templates/pom-platform.md) | 父 POM BOM + common/starter POM 要点 |
| 服务 POM | [pom-service.md](templates/pom-service.md) | 六模块依赖矩阵 + 各模块 POM |
| 契约 | [contract.md](templates/contract.md) | DTO/常量/枚举/Feign/Fallback |
| 领域 | [domain.md](templates/domain.md) | 聚合/值对象/状态机/事件/仓储接口/异常 |
| 应用层 | [application.md](templates/application.md) | Controller/AppService/QueryService/Assembler/异常处理 |
| 基础设施 | [infrastructure.md](templates/infrastructure.md) | PO/Mapper/XML/RepositoryImpl/Converter/Outbox/Saga/ACL |
| SQL | [sql-reference.md](templates/sql-reference.md) | PostgreSQL 类型/Flyway/通用表 |
| 配置 | [config.md](templates/config.md) | 启动类/application.yml/logback |
