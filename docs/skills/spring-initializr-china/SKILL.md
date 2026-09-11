---
name: spring-initializr-china
description: Continuously advance projects scaffolded from the China start.spring.io fork (DDD six-module). First create via MCP /ai or /starter.zip, then iterate entities, modules, conventions, and checks locally.
---

# Spring Initializr China — post-scaffold Skill

## Workflow

1. **Create** via MCP `create_project` (or `POST /ai/v1/projects` → `/starter.zip`) with `template=ddd-enhanced`, dependency `ddd-six-module`, and entities JSON.
2. **Unzip** and open the six-module tree (`*-contract|domain|infrastructure|application|bootstrap|feign-client`).
3. **Iterate** — add entities/modules following conventions below; do not invent a parallel layering.
4. **Verify** — `mvn -DskipTests package`; optional `--spring.profiles.active=h2` for CRUD smoke.

## Conventions (guidance)

- URL: `/{prefix}/v1/{resource}` (`prefix` = artifact without `-service`)
- No global `R`/`ApiResponse` wrapper — return DTOs / `PageResult`; errors = Problem Details
- Real ApplicationService + QueryService (no TODO stubs)
- Swagger: `@Tag` / `@Operation` / `@Schema` when swagger enabled
- Flyway under `{svc}-bootstrap/.../db/migration`

## MCP vs Skill

| | MCP | This Skill |
|--|-----|------------|
| Role | Third-party bootstrap against running start-site | Continuous advancement after unzip |
| Creates zip | Yes | No (edits repo) |
| May call MCP | — | Yes, to generate additional slices |

Reference patterns (not mandatory 1:1): `docs/multi-module-ref/` / spring-boot-gen.
