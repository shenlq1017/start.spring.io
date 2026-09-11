# 原风格改动对照（不另做新 UI）

以现网/本地原版页面为准，只改文案与在原有 `Control` / `Radio` 槽位插入内容。

## 主表单（`Fields.js` 原结构）

| 原英文 | 中文 |
|--------|------|
| Project | 项目 |
| Language | 语言 |
| Spring Boot | Spring Boot（可保留） |
| Project Metadata | 项目元数据 |
| Group / Artifact / Package name | Group / Artifact / 包名 |
| Packaging / Configuration / Java | 打包 / 配置格式 / Java |
| Dependencies / ADD DEPENDENCIES | 依赖 / 添加依赖 |
| Generate / Explore | 生成 / 探索 |

## 新增（插在 Spring Boot 与 Project Metadata 之间，同一套 Radio）

**工程架构**（与 Project/Language 同款组件，注意窄屏换行与原版一致）：
- 单应用
- 业务微服务
- 平台工程

选「业务微服务 / 平台工程」时，在 Dependencies 上方或右侧依赖区下方增加可折叠「业务实体」块（仍用原边框/按钮样式，不新造卡片皮肤）。

## 业务实体（可收起，支持多实体）

每个实体：对象名*、表名、数据库*、ORM*；接口勾选：创建/详情/分页/更新/删除/**导入**/**导出**。

## 视觉

仅质感：原 SCSS 上微提选中态/主按钮/边框，不改布局栅格。
