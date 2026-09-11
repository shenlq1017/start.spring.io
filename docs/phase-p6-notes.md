# Phase P6 — Interaction / UI fixes

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **Add entity → select new** — Sidebar / home 「添加实体」 selects the new entity tab. `EntitiesEditor` no longer resets `activeIndex` to stale `initialIndex` when `list.length` grows; add path still calls `setActiveIndex(next.length - 1)`.
2. **中文描述 forward** — Entity form order: 对象名 → 中文描述 → 表名 → 数据库 → ORM → 是否开启 Swagger → 字段 → 接口.
3. **Swagger label clarity** — zh: `entities.swagger` = 「是否开启 Swagger」; `entities.field.swagger` = 「Swagger 文档」.
4. **Right column Dependencies** — Header alignment/density, empty-state card (dashed muted panel), tighter list typography; Chinese deps + add-deps modal unchanged.
5. **Radio width jitter (global)** — Checked radios no longer bump `font-weight` (was 300→600). `.group-radio` uses flex + stable `font-weight: 500` + per-option `min-width` (Project/Language/Boot/Architecture/Template/Config/Java + entity DB/ORM).
6. **Entity list actions right-aligned** — Home rows `space-between`: name left, 「编辑 / 移除」 in `.entity-chip-actions` on the right (`width: 100%`).
7. **Button border unify** — 「添加实体」「编辑」 use **2px solid** frames matching Explore / 添加依赖 (`$light-color`), not dashed gray.
8. **Dark mode** — Fixed label specificity (`#333` / `$light-color` beating dark rules on meta/entity labels). Restored readable labels, green checkbox accent, borders, inputs, radios, entity buttons, deps empty state under `body.dark`.

### Non-regressions
- P5: back/ESC, green checkboxes, hide 添加依赖 in editor, divider ~55/45, packaging=jar, Explore debounce / entities JSON.

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
(cd start-client && yarn test)
./mvnw -pl start-client,start-site -am -DskipTests package
```
