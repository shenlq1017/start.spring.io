# Phase P7 — Java one-line, template kit deps, dark mode pass

Date: 2026-09-11 (Asia/Shanghai)

## Scope (done)

1. **Java selection one line** — Config | Java still share a metadata row; Java side gets more flex space; numeric radios use compact `min-width` + `flex-wrap: nowrap` so 26/25/21/17 stay on one horizontal line (light + dark). Global P6 radio `min-width` reduced from `4.5rem` → `3.25rem`; Architecture/Template/Boot keep wider mins.
2. **Template → auto-select dependencies** — `TEMPLATE_KIT_DEPS` in `Template.js`:
   - `ddd-standard` / `ddd-enhanced`: `web`, `validation`, `mybatis-plus`, `postgresql`, `flyway`, `knife4j`
   - `platform-standard`: `web`, `validation`, `mybatis-plus`, `postgresql`, `knife4j`
   - `platform-enhanced`: same + `flyway`
   - `applyTemplateKitDependencies` strips prior kit ids, keeps user extras (e.g. R2DBC), re-applies active kit (no duplicates).
   - Wired in Initializr `COMPLETE` / `UPDATE` (architecture|template) / `LOAD`. Architecture markers (`ddd-six-module` / `platform-monorepo`) still applied at share/generate via `applyArchitectureDependencies` and stay hidden from the picker.
3. **Dark mode thorough pass** — Unselected radio text + caret borders light on OLED; metadata input underlines visible (`$dark-border` vs invisible `#0f172a`); header logo: hexagon green, leaf light, **spring** wordmark (`st-word`) light; entity primary/secondary contrast; deps description contrast; dividers/overlays/buttons.

### Non-regressions
- P5/P6: divider ~55/45, packaging=jar, entity editor, Swagger labels, radio weight jitter fix, entity select-on-add, deps empty state, button borders.

## Files (high level)

- Client: `Template.js`, `Initializr.js`, `FieldRadio.js`, `Fields.js`, `Logo.js`, `_modern.scss`, `_dark.scss`, `_main.scss`
- Tests: `utils/__tests__/Template.js`, Initializr kit cases
- Docs: this file + `STATUS.md`

## Verify

```bash
export JAVA_HOME=/workspace/tools/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
(cd start-client && yarn test)
./mvnw -pl start-client,start-site -am -DskipTests package
```

## Build / verify result (this run)

- `yarn test`: **6 suites / 60 tests passed**
- `./mvnw -pl start-client,start-site -am -DskipTests package`: **BUILD SUCCESS**
