# start.spring.io MCP server

stdio MCP server so Cursor / Claude Desktop can scaffold projects against a **local** start-site.

## Prerequisites

```bash
# terminal 1 — start-site (offline OK)
export JAVA_HOME=/workspace/tools/jdk-21   # or your JDK 21
./mvnw -pl start-client,start-site -am -DskipTests package
java -jar start-site/target/start-site-exec.jar --application.offline=true --server.port=8080
```

```bash
# terminal 2 — MCP deps
cd mcp && npm install
```

## Cursor MCP config

```json
{
  "mcpServers": {
    "start-spring-io": {
      "command": "node",
      "args": ["/ABS/PATH/to/start.spring.io/mcp/src/index.js"],
      "env": {
        "START_SITE_URL": "http://localhost:8080"
      }
    }
  }
}
```

## Tools

| Tool | Purpose |
|------|---------|
| `get_capabilities` / `describe_generator` | Architectures, templates, kits, URL convention |
| `get_metadata` | `/metadata/client` |
| `list_dependencies` | Dependency catalog |
| `validate_request` | `POST /ai/v1/validate` |
| `create_project` | Download `/starter.zip` to a path (supports `entities`) |

## Companion Skill

See `docs/skills/spring-initializr-china/SKILL.md` — after MCP creates the zip, use the Skill to iterate modules/conventions.

## Example (Order)

`create_project` with `artifactId=order-service`, `template=ddd-enhanced`, `architecture=ddd-six-module`, entities=`[{name:Order,table:biz_order,...}]` → API base `/order/v1/orders`.
