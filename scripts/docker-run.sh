#!/usr/bin/env bash
set -euo pipefail
TAG="${1:-start-spring-io-china:local}"
PORT="${SERVER_PORT:-8080}"
docker run --rm -p "${PORT}:8080" \
  -e SERVER_PORT=8080 \
  -e APPLICATION_OFFLINE="${APPLICATION_OFFLINE:-true}" \
  -e JAVA_OPTS="${JAVA_OPTS:--XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0}" \
  "$TAG"
