# Phase P5 — Entities editor polish + home divider/alignment

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

### Homepage
1. **Divider left** — `.colset-main` split **~55–58% left / 42–45% right** (was 65–68%). At ≥1600px (1920): **55/45**; ≥1280: **56/44**; default **58/42**. Project/Boot radio rows stay `nowrap`.
2. **Entity list actions** — chip rows are `width: fit-content` with actions beside the name (not pinned to the column’s right edge).
3. **Control alignment restored** — `.control-inline` again uses **110px right-aligned label column**, `align-items: center`, darker labels `#333` (not washed `#64748b`). Metadata denser but same baseline.
4. **Single-app blank gap** — removed Template/Entities **reserved min-height spacers**; slots render only when visible.

### Entities editor (Explore-like overlay)
5. **Portal to `document.body`** + `z-index: 450` so home sticky/actions/`添加依赖` cannot paint through; `body.entities-editor-open` hides home floaters.
6. **返回** in header (+ ESC / bottom 完成); Escape closes.
7. **Rounded frames** (`$spring-radius`), **font ~+1** (14px interiors), **darker labels**.
8. **Delete ×** unified `entity-icon-btn` (~22px), muted → soft red hover (sidebar + fields).
9. **DB/ORM** use home **Radio** pattern; field type keeps styled select.
10. **Checkbox `accent-color`** green (`$light-primary`).
11. **Flex baselines** on meta rows and field rows; less empty bottom padding in panes.
12. **zh** `entities.back` = `返回`.

### Non-regressions
- packaging forced `jar`; entities JSON + Explore debounce unchanged.

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
(cd start-client && yarn test)
./mvnw -pl start-client,start-site -am -DskipTests package
```
