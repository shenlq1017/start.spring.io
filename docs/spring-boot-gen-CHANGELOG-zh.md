# spring-boot-gen 变更说明（相对原 zip / multi-module-ref）

日期：2026-09-11（Asia/Shanghai）

## 背景

`start.spring.io` Initializr 企业增强 CRUD（Phase P11）已改为**默认具体 Service 类**（无 `QueryService` 接口 + `service/impl/QueryServiceImpl`）。本技能原 zip / `docs/multi-module-ref` 仍强制 Interface+Impl，与生成器习惯冲突，导致对照文档/脚手架与 Explore 产物不一致。

## 变更摘要

| 项 | 旧（原 spring-boot-gen.zip） | 新（与 Initializr P11 对齐） |
|----|------------------------------|------------------------------|
| `QueryService` | `interface` + `service/impl/*QueryServiceImpl` | **具体 `@Service` 类**，CQRS 体在类内 |
| `ApplicationService` | 已是具体类 | 保持具体类，文档明确「默认无 Interface+impl/」 |
| `scaffold.py crud` | 写出 Interface + Impl 两文件 | **只写出** `*QueryService.java`（具体类） |
| 模板 `QueryServiceImpl.java.j2` | 存在 | **已删除**（默认路径不再需要） |
| `SKILL.md` / `templates/application.md` | `service(+impl)`、接口示例 | 改为具体类默认；注明多实现时可选拆接口 |
| 保留能力 | — | ApiPath `/{prefix}/v1/{resource}`、MP Page→PageResult、ReadMapper、Swagger、Snowflake、软删 BOOLEAN 等不变 |

## 为何改

1. 与 `start.spring.io` 当前生成物一致，避免「技能文档要求 Interface+Impl / 生成器产出具体类」双标准。
2. 应用层 Query/Application 服务通常单实现；强制接口增加噪音，Explore 树还易出现多余 `impl/`。
3. Domain `Repository` 端口仍保留接口（基础设施可替换）；仅应用层 Service 默认具体化。

## 未改

- 六模块依赖方向、contract/Feign、ReadMapper XML 投影、Problem Detail、Flyway/PostgreSQL 约定等红线未放松。
- 未把 zip 再提交进 git；以 `docs/multi-module-ref/` 提取树为准。

## 验证建议

```bash
# 模板应为具体类、无 Impl 模板
rg -n "public class .*QueryService" docs/multi-module-ref/scaffold/templates/application/QueryService.java.j2
test ! -f docs/multi-module-ref/scaffold/templates/application/QueryServiceImpl.java.j2
rg -n "QueryServiceImpl|service/impl" docs/multi-module-ref/scaffold/scaffold.py   # 应无命中
```
