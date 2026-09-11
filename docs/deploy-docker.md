# Docker deploy — start.spring.io China fork

## Build

```bash
./scripts/docker-build.sh
# or
docker build -t start-spring-io-china:local .
```

## Run

```bash
./scripts/docker-run.sh
# or compose
docker compose up --build
```

Defaults: port **8080**, `APPLICATION_OFFLINE=true`, sensible `JAVA_OPTS`.

```bash
SERVER_PORT=8080 APPLICATION_OFFLINE=true ./scripts/docker-run.sh
```

## Push (example)

```bash
docker tag start-spring-io-china:local registry.example.com/start-spring-io-china:1.0
docker push registry.example.com/start-spring-io-china:1.0
```

## Without Docker

```bash
./scripts/pack.sh
java -jar start-site/target/start-site-exec.jar --application.offline=true
```

UI: http://localhost:8080 — AI: http://localhost:8080/ai/v1/capabilities — MCP: see `mcp/README.md`.
