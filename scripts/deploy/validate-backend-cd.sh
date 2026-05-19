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
VERTEX_KEY_SCRIPT="$ROOT_DIR/scripts/deploy/materialize-vertex-key.sh"
DOCTOR_SCRIPT="$ROOT_DIR/scripts/deploy/oci-staging-doctor.sh"
POSTGRES_SCRIPT="$ROOT_DIR/scripts/deploy/ensure-postgres-service.sh"
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
require_file "$VERTEX_KEY_SCRIPT"
require_file "$DOCTOR_SCRIPT"
require_file "$POSTGRES_SCRIPT"
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
    "$VERTEX_KEY_SCRIPT" \
    "$DOCTOR_SCRIPT" \
    "$POSTGRES_SCRIPT" \
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
require_pattern 'scripts/deploy/materialize-vertex-key.sh' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_BACKEND_ENV_B64|BACKEND_ENV_B64' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_FRONTEND_ENV_B64|FRONTEND_ENV_B64' "$WORKFLOW_FILE"
require_pattern 'OCI_A1_VERTEX_KEY_JSON_B64|VERTEX_KEY_JSON_B64' "$WORKFLOW_FILE"
require_pattern 'deploy-debug' "$WORKFLOW_FILE"
require_pattern 'GITHUB_STEP_SUMMARY' "$WORKFLOW_FILE"
reject_pattern 'OCI_SSH_PRIVATE_KEY|OCI_BACKEND_HOST|OCI_BACKEND_USER|scp ' "$WORKFLOW_FILE"

log "checking workflow contract gate"
"$CONTRACT_SCRIPT"

backend_env_fixture="$(mktemp)"
frontend_env_fixture="$(mktemp)"
vertex_key_fixture="$(mktemp)"
materialized_env="$(mktemp)"
materialized_frontend_env="$(mktemp)"
materialized_vertex_key="$(mktemp)"
trap 'rm -f "$backend_env_fixture" "$frontend_env_fixture" "$vertex_key_fixture" "$materialized_env" "$materialized_frontend_env" "$materialized_vertex_key"' EXIT
cat > "$backend_env_fixture" <<ENV
SPRING_DATASOURCE_URL=jdbc:postgresql://aquila-postgres:5432/aquila
SPRING_DATASOURCE_USERNAME=aquila
SPRING_DATASOURCE_PASSWORD=secret
AQUILA_AUTH_TOKEN_SECRET=secret-secret-secret-secret-secret-1
AQUILA_EXTERNAL_TOKEN_KEY=external-secret-secret-secret-1
ENV
cat > "$frontend_env_fixture" <<ENV
NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:8080
NEXT_PUBLIC_GOOGLE_CLIENT_ID=local-google-client.apps.googleusercontent.com
NEXT_PUBLIC_SITE_URL=http://127.0.0.1:3000
ENV
cat > "$vertex_key_fixture" <<'JSON'
{
  "type": "service_account",
  "project_id": "local-project",
  "private_key_id": "local-private-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\nLOCAL\n-----END PRIVATE KEY-----\n",
  "client_email": "vertex-local@local-project.iam.gserviceaccount.com"
}
JSON

log "checking backend env materialization"
BACKEND_ENV_B64="$(base64 < "$backend_env_fixture" | tr -d '\n')" "$ENV_SCRIPT" --output "$materialized_env"

log "running managed Postgres dry-run"
"$POSTGRES_SCRIPT" --dry-run --env-file "$materialized_env" --network aquila-office

log "checking frontend env materialization"
FRONTEND_ENV_B64="$(base64 < "$frontend_env_fixture" | tr -d '\n')" "$FRONTEND_ENV_SCRIPT" --output "$materialized_frontend_env"

log "checking Vertex key materialization"
VERTEX_KEY_JSON_B64="$(base64 < "$vertex_key_fixture" | tr -d '\n')" "$VERTEX_KEY_SCRIPT" --output "$materialized_vertex_key"

log "running blue/green entrypoint dry-run"
"$BLUE_GREEN_SCRIPT" --dry-run --image aquila-office-back:dry-run --vertex-key-file "$materialized_vertex_key"

log "running OCI hook dry-run"
"$OCI_HOOK_SCRIPT" \
    --dry-run \
    --image aquila-office-back:dry-run \
    --image-tar /tmp/aquila-office-back-dry-run.tar \
    --env-file "$materialized_env" \
    --vertex-key-file "$materialized_vertex_key" \
    --doctor-script "$DOCTOR_SCRIPT"

log "running frontend OCI hook dry-run"
"$OCI_FRONTEND_HOOK_SCRIPT" \
    --dry-run \
    --image aquila-office-front:dry-run \
    --image-tar /tmp/aquila-office-front-dry-run.tar \
    --env-file "$materialized_frontend_env" \
    --doctor-script "$DOCTOR_SCRIPT"

log "staging CD dry-run validation passed"
