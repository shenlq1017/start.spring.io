# MCP / AI verify — Order + Activity

Date: 2026-09-11 (Asia/Shanghai)

## Setup

- start-site: `java -jar start-site/target/start-site-exec.jar --application.offline=true --server.port=8080`
- AI: `GET /ai/v1/capabilities` → **200**
- Generation: `GET /starter.zip` with `template=ddd-enhanced`, `dependencies=ddd-six-module,...`, `entities=[...]`

## Order (订单)

| Check | Result |
|-------|--------|
| Zip size | ~80KB |
| Bootstrap module | Pass |
| `OrderController` + `@Tag`/`@Operation` | Pass |
| Real `OrderApplicationService` (no TODO) | Pass |
| `OrderQueryServiceImpl` | Pass |
| ApiPath | **`/order/v1/orders`** |
| Flyway `biz_order` | Pass |
| `mvn -DskipTests package` | **Pass** |

## Activity (活动)

| Check | Result |
|-------|--------|
| Zip size | ~83KB |
| Bootstrap module | Pass |
| `ActivityController` + swagger | Pass |
| Real `ActivityApplicationService` | Pass |
| ApiPath | **`/activity/v1/activities`** |
| Flyway `biz_activity` | Pass |
| `mvn -DskipTests package` | **Pass** |

## Structure assertions

`14 / 14` automated checks passed (bootstrap, controller, swagger, services, URLs, flyway, query impl).

## MCP

`mcp/` Node stdio server; `npm install` OK. Tools call the same start-site URLs. Config snippet in `mcp/README.md`.

## Skill

`docs/skills/spring-initializr-china/SKILL.md` — post-scaffold iteration companion.
