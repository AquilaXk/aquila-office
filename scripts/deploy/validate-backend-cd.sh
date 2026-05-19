#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW_FILE="$ROOT_DIR/.github/workflows/backend-cd.yml"
BLUE_GREEN_ENTRYPOINT="$ROOT_DIR/scripts/deploy-bluegreen.sh"
BLUE_GREEN_SCRIPT="$ROOT_DIR/scripts/deploy/backend-blue-green.sh"
OCI_HOOK_SCRIPT="$ROOT_DIR/scripts/deploy/oci-backend-hook.sh"
ENV_SCRIPT="$ROOT_DIR/scripts/deploy/materialize-backend-env.sh"
DOCTOR_SCRIPT="$ROOT_DIR/scripts/deploy/oci-staging-doctor.sh"
CONTRACT_SCRIPT="$ROOT_DIR/tools/test/check-staging-cd-contract.sh"

log() {
    printf '[validate-backend-cd] %s\n' "$*"
}

require_file() {
    if [[ ! -f "$1" ]]; then
        echo "missing required file: $1" >&2
        exit 1
    fi
}

require_pattern() {
    local pattern="$1"
    local file="$2"
    if ! grep -Eq "$pattern" "$file"; then
        echo "missing pattern '$pattern' in $file" >&2
        exit 1
    fi
}

require_file "$WORKFLOW_FILE"
require_file "$BLUE_GREEN_ENTRYPOINT"
require_file "$BLUE_GREEN_SCRIPT"
require_file "$OCI_HOOK_SCRIPT"
require_file "$ENV_SCRIPT"
require_file "$DOCTOR_SCRIPT"
require_file "$CONTRACT_SCRIPT"

log "checking shell syntax"
bash -n "$BLUE_GREEN_ENTRYPOINT" "$BLUE_GREEN_SCRIPT" "$OCI_HOOK_SCRIPT" "$ENV_SCRIPT" "$DOCTOR_SCRIPT" "$CONTRACT_SCRIPT" "$0"

log "checking workflow contract"
require_pattern '^  workflow_run:' "$WORKFLOW_FILE"
require_pattern 'workflows:[[:space:]]*\["CI"\]|- CI' "$WORKFLOW_FILE"
require_pattern '^  workflow_dispatch:' "$WORKFLOW_FILE"
require_pattern 'dry_run:' "$WORKFLOW_FILE"
require_pattern 'target_slot:' "$WORKFLOW_FILE"
require_pattern 'github.event.workflow_run.head_sha' "$WORKFLOW_FILE"
require_pattern 'origin/main' "$WORKFLOW_FILE"
require_pattern 'deployments' "$WORKFLOW_FILE"
require_pattern 'docker build -f docker/Dockerfile' "$WORKFLOW_FILE"
require_pattern 'docker save "\$IMAGE_REF" -o backend-image.tar' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy-bluegreen.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-backend-hook.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-staging-doctor.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/materialize-backend-env.sh' "$WORKFLOW_FILE"
require_pattern 'OCI_SSH_PRIVATE_KEY' "$WORKFLOW_FILE"
require_pattern 'OCI_BACKEND_HOST' "$WORKFLOW_FILE"
require_pattern 'OCI_BACKEND_USER' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_BACKEND_ENV_B64|BACKEND_ENV_B64' "$WORKFLOW_FILE"
require_pattern 'deploy-debug' "$WORKFLOW_FILE"
require_pattern 'GITHUB_STEP_SUMMARY' "$WORKFLOW_FILE"

log "checking workflow contract gate"
"$CONTRACT_SCRIPT"

env_fixture="$(mktemp)"
materialized_env="$(mktemp)"
trap 'rm -f "$env_fixture" "$materialized_env"' EXIT
cat > "$env_fixture" <<ENV
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/aquila
SPRING_DATASOURCE_USERNAME=aquila
SPRING_DATASOURCE_PASSWORD=secret
AQUILA_AUTH_TOKEN_SECRET=secret-secret-secret-secret-secret-1
AQUILA_EXTERNAL_TOKEN_KEY=external-secret-secret-secret-1
ENV

log "checking backend env materialization"
BACKEND_ENV_B64="$(base64 < "$env_fixture" | tr -d '\n')" "$ENV_SCRIPT" --output "$materialized_env"

log "running blue/green entrypoint dry-run"
"$BLUE_GREEN_ENTRYPOINT" --dry-run --image aquila-office-back:dry-run

log "running OCI hook dry-run"
"$OCI_HOOK_SCRIPT" \
    --dry-run \
    --image aquila-office-back:dry-run \
    --image-tar /tmp/aquila-office-back-dry-run.tar \
    --env-file "$materialized_env" \
    --doctor-script "$DOCTOR_SCRIPT"

log "backend CD dry-run validation passed"
