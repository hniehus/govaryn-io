#!/usr/bin/env bash
set -euo pipefail
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Install Maven first."
  exit 1
fi
mvn -N -q wrapper:wrapper
echo "Maven wrapper created. Use: ./mvnw -v"
