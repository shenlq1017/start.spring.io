#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export JAVA_HOME="${JAVA_HOME:-/workspace/tools/jdk-21}"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -pl start-client,start-site -am -DskipTests package
echo "JAR: $ROOT/start-site/target/start-site-exec.jar"
