#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SCRIPT_PATH="$REPO_ROOT/tools/perf/k6-smoke.js"

export API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
export SMOKE_SERVICE_JOURNEY="${SMOKE_SERVICE_JOURNEY:-true}"
export SMOKE_GOOGLE_ID_TOKEN="${SMOKE_GOOGLE_ID_TOKEN:-smoke-google-id-token}"

k6 run "$SCRIPT_PATH"
