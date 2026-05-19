#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW_FILE="$ROOT_DIR/.github/workflows/backend-cd.yml"
BLUE_GREEN_ENTRYPOINT="$ROOT_DIR/scripts/deploy-bluegreen.sh"
BLUE_GREEN_SCRIPT="$ROOT_DIR/scripts/deploy/backend-blue-green.sh"
FRONTEND_BLUE_GREEN_SCRIPT="$ROOT_DIR/scripts/deploy/frontend-blue-green.sh"
OCI_HOOK_SCRIPT="$ROOT_DIR/scripts/deploy/oci-backend-hook.sh"
OCI_FRONTEND_HOOK_SCRIPT="$ROOT_DIR/scripts/deploy/oci-frontend-hook.sh"
ENV_SCRIPT="$ROOT_DIR/scripts/deploy/materialize-backend-env.sh"
FRONTEND_ENV_SCRIPT="$ROOT_DIR/scripts/deploy/materialize-frontend-env.sh"
DOCTOR_SCRIPT="$ROOT_DIR/scripts/deploy/oci-staging-doctor.sh"
CONTRACT_SCRIPT="$ROOT_DIR/tools/test/check-staging-cd-contract.sh"
BOOTSTRAP_SCRIPT="$ROOT_DIR/scripts/deploy/bootstrap-oci-staging-host.sh"

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

reject_pattern() {
    local pattern="$1"
    local file="$2"
    if grep -Eq "$pattern" "$file"; then
        echo "unexpected pattern '$pattern' in $file" >&2
        exit 1
    fi
}

require_file "$WORKFLOW_FILE"
require_file "$BLUE_GREEN_ENTRYPOINT"
require_file "$BLUE_GREEN_SCRIPT"
require_file "$FRONTEND_BLUE_GREEN_SCRIPT"
require_file "$OCI_HOOK_SCRIPT"
require_file "$OCI_FRONTEND_HOOK_SCRIPT"
require_file "$ENV_SCRIPT"
require_file "$FRONTEND_ENV_SCRIPT"
require_file "$DOCTOR_SCRIPT"
require_file "$CONTRACT_SCRIPT"
require_file "$BOOTSTRAP_SCRIPT"

log "checking shell syntax"
bash -n \
    "$BLUE_GREEN_ENTRYPOINT" \
    "$BLUE_GREEN_SCRIPT" \
    "$FRONTEND_BLUE_GREEN_SCRIPT" \
    "$OCI_HOOK_SCRIPT" \
    "$OCI_FRONTEND_HOOK_SCRIPT" \
    "$ENV_SCRIPT" \
    "$FRONTEND_ENV_SCRIPT" \
    "$DOCTOR_SCRIPT" \
    "$CONTRACT_SCRIPT" \
    "$BOOTSTRAP_SCRIPT" \
    "$0"

log "checking workflow contract"
require_pattern '^  workflow_run:' "$WORKFLOW_FILE"
require_pattern 'workflows:[[:space:]]*\["CI"\]|- CI' "$WORKFLOW_FILE"
require_pattern '^  workflow_dispatch:' "$WORKFLOW_FILE"
require_pattern 'dry_run:' "$WORKFLOW_FILE"
require_pattern 'target_slot:' "$WORKFLOW_FILE"
require_pattern 'github.event.workflow_run.head_sha' "$WORKFLOW_FILE"
require_pattern 'origin/main' "$WORKFLOW_FILE"
require_pattern 'deployments' "$WORKFLOW_FILE"
require_pattern 'self-hosted' "$WORKFLOW_FILE"
require_pattern 'oci-a1-staging' "$WORKFLOW_FILE"
require_pattern 'linux/arm64' "$WORKFLOW_FILE"
require_pattern 'docker buildx build' "$WORKFLOW_FILE"
require_pattern 'backend-image.tar' "$WORKFLOW_FILE"
require_pattern 'frontend-image.tar' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-backend-hook.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-frontend-hook.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-staging-doctor.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/materialize-backend-env.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/materialize-frontend-env.sh' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_BACKEND_ENV_B64|BACKEND_ENV_B64' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_FRONTEND_ENV_B64|FRONTEND_ENV_B64' "$WORKFLOW_FILE"
require_pattern 'deploy-debug' "$WORKFLOW_FILE"
require_pattern 'GITHUB_STEP_SUMMARY' "$WORKFLOW_FILE"
reject_pattern 'OCI_SSH_PRIVATE_KEY|OCI_BACKEND_HOST|OCI_BACKEND_USER|scp ' "$WORKFLOW_FILE"

log "checking workflow contract gate"
"$CONTRACT_SCRIPT"

backend_env_fixture="$(mktemp)"
frontend_env_fixture="$(mktemp)"
materialized_env="$(mktemp)"
materialized_frontend_env="$(mktemp)"
trap 'rm -f "$backend_env_fixture" "$frontend_env_fixture" "$materialized_env" "$materialized_frontend_env"' EXIT
cat > "$backend_env_fixture" <<ENV
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/aquila
SPRING_DATASOURCE_USERNAME=aquila
SPRING_DATASOURCE_PASSWORD=secret
AQUILA_AUTH_TOKEN_SECRET=secret-secret-secret-secret-secret-1
AQUILA_EXTERNAL_TOKEN_KEY=external-secret-secret-secret-1
ENV
cat > "$frontend_env_fixture" <<ENV
NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:8080
ENV

log "checking backend env materialization"
BACKEND_ENV_B64="$(base64 < "$backend_env_fixture" | tr -d '\n')" "$ENV_SCRIPT" --output "$materialized_env"

log "checking frontend env materialization"
FRONTEND_ENV_B64="$(base64 < "$frontend_env_fixture" | tr -d '\n')" "$FRONTEND_ENV_SCRIPT" --output "$materialized_frontend_env"

log "running blue/green entrypoint dry-run"
"$BLUE_GREEN_ENTRYPOINT" --dry-run --image aquila-office-back:dry-run

log "running OCI hook dry-run"
"$OCI_HOOK_SCRIPT" \
    --dry-run \
    --image aquila-office-back:dry-run \
    --image-tar /tmp/aquila-office-back-dry-run.tar \
    --env-file "$materialized_env" \
    --doctor-script "$DOCTOR_SCRIPT"

log "running frontend OCI hook dry-run"
"$OCI_FRONTEND_HOOK_SCRIPT" \
    --dry-run \
    --image aquila-office-front:dry-run \
    --image-tar /tmp/aquila-office-front-dry-run.tar \
    --env-file "$materialized_frontend_env" \
    --doctor-script "$DOCTOR_SCRIPT"

log "staging CD dry-run validation passed"
