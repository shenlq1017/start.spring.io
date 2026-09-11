#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/workspace/tools/jdk-21}"
export PATH="$JAVA_HOME/bin:${PATH}"
cd "$ROOT"
./mvnw -pl start-site -am -DskipTests compile
./mvnw -pl start-site -Dtest=PlatformMonorepoProjectContributorTests -Dsurefire.failIfNoSpecifiedTests=false test
echo "OK: PlatformMonorepoProjectContributorTests passed"
