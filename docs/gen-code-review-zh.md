# 生成 CRUD 代码评审（相对 spring-boot-gen）

> **P10（2026-09-11 Asia/Shanghai）**：下列 **P1 + P2 均已落地**（见 `docs/phase-p10-notes.md`）。下表「现象」保留历史描述；状态列以「本轮已修」为准。


评审范围：`DddCrudSliceGenerator.java` + `DddSixModuleProjectContributor` 产出，对照 `/workspace/multi-module-ref`（spring-boot-gen）`SKILL.md` 与 `scaffold/templates/**`。

评审基准日：2026-09-11（Asia/Shanghai）。本轮已修复部分 **P0**（分页手写 + 测试锁定 `@RequestMapping(XxxApiPath.BASE)`），文档中仍标注「已修 / 未修」。

---

## 总览

| 维度 | 结论 |
|------|------|
| 六模块骨架 | 基本对齐（contract / feign-client / domain / infrastructure / application / bootstrap） |
| 垂直切片完整度 | 中等：有 Assembler、ApplicationService、QueryService、Repository 端口、Feign、Problem Details，但 CQRS 读侧与 common 复用偏弱 |
| 最大差距 | 分页曾用手写 `skip/limit`（P0，本轮已改为 MP `Page.of` + `selectPage`）；ID 用 UUID 而非 Snowflake；无 platform `common-core`；Query 无 `@Validated`；导入导出仅为 stub |

---

## 问题清单

### 1. 分页：手写页码数学 vs MyBatis-Plus `Page` / `PageResult`

| 项 | 内容 |
|----|------|
| **严重度** | **P0**（已修） |
| **现象** | 原 `QueryServiceImpl.page`：`countByQuery` + `findByQuery` 全量拉取后 `.skip((current-1)*size).limit(size)`，大表 OOM / 慢查询风险。 |
| **skill** | `QueryServiceImpl` 使用 `Page.of(current, size)` + `ReadMapper.selectSummaryPage`，对外只返回 `common.core.page.PageResult`，禁止把 `IPage` 暴露到 Controller。 |
| **来源** | **我们的简化**：为保持 application→domain 端口、避免 application 直接依赖 infrastructure ReadMapper，在仓储上做了全量查询 + 内存切片。 |
| **修复** | 本轮：domain 增加 `PageSlice<T>`；`Repository.findPage`；`RepositoryImpl` 使用 `Page.of` + `BaseMapper.selectPage`；`QueryServiceImpl` 映射为 contract `PageResult`。仍无 skill 的 ReadMapper 投影（见 P1）。 |

### 2. Controller `@RequestMapping` / ApiPath

| 项 | 内容 |
|----|------|
| **严重度** | **P0**（生成侧已具备；测试已锁定） |
| **现象** | 生成物已是 `@RequestMapping(UserApiPath.BASE)` + `UserApiPath.BASE = "/{prefix}/v1/{resource}"`，Feign `path = ApiPath.BASE` 一致。历史 smoke zip 亦正确。 |
| **skill** | 同约定：`@RequestMapping({{ entity }}ApiPath.BASE)`。 |
| **来源** | 非缺陷；曾担心格式化串 `%s` 错位导致丢失，已用单测断言 `@RequestMapping(UserApiPath.BASE)`。 |
| **建议** | 保持；后续勿再硬编码路径字符串。 |

### 3. 分层差距（Assembler / QueryService / Repository / Feign / 异常 / Result）

| 项 | 内容 |
|----|------|
| **严重度** | **P1**（本轮已修：ReadMapper + PageResult 迁 common） |
| **现象** | ✅ Assembler、ApplicationService、QueryService 拆分、Repository 接口、Feign+Fallback、`GlobalExceptionHandler`（ProblemDetail）均有。✅ **ReadMapper + XML 投影**；✅ `PageResult` → `contract.common.page`；✅ `BusinessException` + 更丰富 GlobalExceptionHandler；✅ 有意不使用 `R`/`ApiResponse` 包装（与 skill「标准 REST 直接返回 DTO/PageResult」一致）。 |
| **来源** | **混合**：无包装是对齐 skill；ReadMapper/CQRS 与 common-core 是 **我们的简化**（Initializr 单服务无平台 common 模块）。 |
| **建议** | 中期：enhanced 模板增加 `XxxReadMapper` + `selectSummaryPage`；长期：platform-monorepo 抽出 `common-core` 再让微服务依赖。 |

### 4. Swagger 完整度

| 项 | 内容 |
|----|------|
| **严重度** | **P2**（本轮已修） |
| **现象** | Controller 有 `@Tag`/`@Operation`/`@Parameter`；DTO 有 `@Schema`。~~缺：分页 `@Validated`~~ → 已加；部分响应字段 schema 深度一般；Feign 方法 swagger 视开关而定。 |
| **skill** | 同样要求 Tag/Operation/Schema；Controller `page(@Validated Query…)`。 |
| **来源** | **我们的简化**（page 方法漏 `@Validated`）。 |
| **建议** | Controller `page` 增加 `@Validated`；QueryRequest 补 `@Min`/`@Max`（current/size）。 |

### 5. 校验、ID 类型、软删、状态枚举

| 项 | 内容 |
|----|------|
| **严重度** | **P1**（ID/雪花，本轮已修）；**P2**（其余，本轮已修） |
| **现象** | Create 用 `@NotBlank`/`@Size`；ID 为 **Snowflake**（`SnowflakeIdGenerator` + `IdType.ASSIGN_ID`）；PO `@TableLogic` + `deleted BOOLEAN`；契约有 `StatusEnum`，领域另有 `XxxStatus`——双轨可接受但描述字段弱于 skill（skill 枚举带 description）。软删类型 skill 用 `BOOLEAN`，我们用 `INT 0/1`。 |
| **来源** | ID：**我们的简化**；软删类型差异：**简化**；双状态枚举：两边都有 domain/contract 分离思想。 |
| **建议** | 引入轻量 `SnowflakeIdGenerator`（可生成到 infrastructure/common）；Flyway `deleted BOOLEAN`；StatusEnum 补中文 description。 |

### 6. Jackson 3 / Boot 4 约定

| 项 | 内容 |
|----|------|
| **严重度** | **P2**（当前无直接违规） |
| **现象** | 生成切片几乎不手写 Jackson；无 `JsonbTypeHandler`。若后续加 JSONB，必须用 `tools.jackson`，禁止 `com.fasterxml.jackson`。 |
| **skill** | 强制 Jackson 3；ArchUnit 可禁旧包名。 |
| **来源** | 未实现 JSONB：**简化**；骨架 mustache 侧有 ArchUnit 模板可部分兜底。 |
| **建议** | 需要 JSONB 时复制 skill 的 `JsonbTypeHandler`；保持 ArchUnit 规则。 |

### 7. 导入/导出 stub 质量

| 项 | 内容 |
|----|------|
| **严重度** | **P2**（本轮已修） |
| **现象** | `ImportExportService` 使用 **EasyExcel** 读写骨架（ExcelRow + stream），不再是纯 Map stub、无流式响应、无文件校验。 |
| **skill** | 完整 CRUD 切片不强制 Excel；企业实践通常 EasyExcel + 异步任务。 |
| **来源** | **有意简化**（可编译占位）。 |
| **建议** | 选中 easyexcel 依赖时生成真实读写骨架；否则在 README 标明 stub。 |

### 8. application.yml / Flyway 一致性

| 项 | 内容 |
|----|------|
| **严重度** | **P1**（本轮已修：COMMENT ON + profile 文档 + BOOLEAN） |
| **现象** | CRUD 生成 `V1__NN_create_*.sql`（TIMESTAMPTZ）+ `application-h2.yml`（关 Flyway、用 `schema-h2.sql`）。主 `application.yml`（mustache）需与 datasource/Flyway 路径一致；H2 与 PG 类型替换较粗（TIMESTAMPTZ→TIMESTAMP）。多实体时 H2 schema **追加**写入，顺序依赖。 |
| **skill** | 单库 Flyway 于 bootstrap；注释 `COMMENT ON`；deleted/布尔与时间类型规范。 |
| **来源** | H2 smoke：**我们的增强**；COMMENT/类型细节：**简化**。 |
| **建议** | SQL 补 `COMMENT ON`；H2 与 PG 脚本生成双轨或更完整类型映射；文档写明默认 profile。 |

### 9. 可测试性

| 项 | 内容 |
|----|------|
| **严重度** | **P1**（本轮已修：ApplicationServiceTest fake repo + ArchUnit fasxml ban） |
| **现象** | 有 Initializr 侧 `DddCrudSliceGeneratorTests` / smoke IT；生成工程内缺：切片级单元测试、Testcontainers、`@SpringBootTest` 切片、ArchUnit 模块依赖测试（模板有 archunit mustache，enhanced CRUD 未强制断言）。构造器注入利于测试，但 RepositoryImpl/QueryService 紧耦合 MP，无接口级 fake 示例。 |
| **skill** | 强调模块依赖测试、禁止错误依赖方向。 |
| **来源** | **简化**（生成体积优先）。 |
| **建议** | 为 Repository 提供内存实现示例测试；bootstrap 保留 ArchUnit；可选生成 `XxxApplicationServiceTest`。 |

---

## 本轮已落地的 P0

1. **分页**：`PageSlice` + `Repository.findPage` + MP `Page.of`/`selectPage`；去掉 `skip/limit` 手写分页。
2. **ApiPath**：测试断言 Controller `@RequestMapping(UserApiPath.BASE)`；QueryService 使用 `PageSlice`/`findPage` 模式。

**P10 已纳入**：Snowflake、ReadMapper CQRS、Query `@Validated`、EasyExcel skeleton、Flyway COMMENT、Explore/Generate 对齐。

---

## 对照速查

| skill 构件 | 我们生成 | 差距 |
|------------|----------|------|
| `XxxApiPath` | ✅ | — |
| `PageResult` | ✅（在 contract） | 应迁 common |
| `Page.of` + DB 分页 | ✅（本轮） | 仍无 ReadMapper 投影 |
| Assembler | ✅ | — |
| QueryService 拆分 | ✅ | — |
| Repository 端口 | ✅ | — |
| Feign + Fallback | ✅ | — |
| GlobalExceptionHandler | ✅（偏瘦） | 异常类型少 |
| Snowflake | ✅ | 已修 |
| ReadMapper | ✅ | 已修 |
| StatusEnum 描述 | ✅ 中文 description | 已修 |
| Import/Export | ✅ EasyExcel skeleton | 已修 |