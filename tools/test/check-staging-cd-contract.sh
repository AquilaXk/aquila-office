#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CI_WORKFLOW="$ROOT_DIR/.github/workflows/ci.yml"
CD_WORKFLOW="$ROOT_DIR/.github/workflows/backend-cd.yml"
ENV_SCRIPT="$ROOT_DIR/scripts/deploy/materialize-backend-env.sh"
DOCTOR_SCRIPT="$ROOT_DIR/scripts/deploy/oci-staging-doctor.sh"
HOOK_SCRIPT="$ROOT_DIR/scripts/deploy/oci-backend-hook.sh"

relative_path() {
    local path="$1"
    printf '%s\n' "${path#"$ROOT_DIR"/}"
}

fail() {
    printf '[staging-cd-contract] %s\n' "$*" >&2
    exit 1
}

require_file() {
    local path="$1"
    [[ -f "$path" ]] || fail "missing required file: $(relative_path "$path")"
}

require_pattern() {
    local pattern="$1"
    local path="$2"
    local description="$3"
    grep -Eq "$pattern" "$path" || fail "missing $description in $(relative_path "$path")"
}

require_file "$CI_WORKFLOW"
require_file "$CD_WORKFLOW"
require_file "$ENV_SCRIPT"
require_file "$DOCTOR_SCRIPT"
require_file "$HOOK_SCRIPT"

require_pattern '^  workflow_run:' "$CD_WORKFLOW" "CI completion trigger"
require_pattern 'workflows:[[:space:]]*\["CI"\]|- CI' "$CD_WORKFLOW" "CI workflow_run source"
require_pattern 'workflow_dispatch:' "$CD_WORKFLOW" "manual dispatch trigger"
require_pattern 'dry_run:' "$CD_WORKFLOW" "manual dry-run input"
require_pattern 'target_slot:' "$CD_WORKFLOW" "manual target slot input"
require_pattern 'github.event.workflow_run.head_sha' "$CD_WORKFLOW" "workflow_run head SHA resolution"
require_pattern 'origin/main' "$CD_WORKFLOW" "main branch freshness check"
require_pattern 'deployments' "$CD_WORKFLOW" "GitHub deployment status API"
require_pattern 'deployment_id' "$CD_WORKFLOW" "deployment id propagation"
require_pattern 'OCI_SSH_PRIVATE_KEY' "$CD_WORKFLOW" "OCI SSH secret"
require_pattern 'OCI_BACKEND_HOST' "$CD_WORKFLOW" "OCI host secret"
require_pattern 'OCI_BACKEND_USER' "$CD_WORKFLOW" "OCI user secret"
require_pattern 'OCI_A1_BACKEND_ENV_B64|BACKEND_ENV_B64' "$CD_WORKFLOW" "backend env secret"
require_pattern 'materialize-backend-env.sh' "$CD_WORKFLOW" "backend env materialization"
require_pattern 'oci-staging-doctor.sh' "$CD_WORKFLOW" "remote doctor"
require_pattern 'GITHUB_STEP_SUMMARY' "$CD_WORKFLOW" "step summary diagnostics"
require_pattern 'deploy-debug' "$CD_WORKFLOW" "deploy debug artifact"
require_pattern 'upload-artifact@v4' "$CD_WORKFLOW" "artifact upload"

require_pattern 'check-staging-cd-contract.sh' "$CI_WORKFLOW" "workflow contract check in CI"
require_pattern 'workflow-contract' "$CI_WORKFLOW" "workflow contract job"

require_pattern 'SPRING_DATASOURCE_URL' "$ENV_SCRIPT" "datasource URL validation"
require_pattern 'SPRING_DATASOURCE_USERNAME' "$ENV_SCRIPT" "datasource username validation"
require_pattern 'SPRING_DATASOURCE_PASSWORD' "$ENV_SCRIPT" "datasource password validation"
require_pattern 'AQUILA_AUTH_TOKEN_SECRET' "$ENV_SCRIPT" "auth token secret validation"
require_pattern 'AQUILA_EXTERNAL_TOKEN_KEY' "$ENV_SCRIPT" "external token key validation"
require_pattern 'chmod 600' "$ENV_SCRIPT" "env file permission hardening"

require_pattern '/var/lib/aquila-data' "$DOCTOR_SCRIPT" "OCI data volume check"
require_pattern 'docker info' "$DOCTOR_SCRIPT" "Docker daemon check"
require_pattern 'docker ps' "$DOCTOR_SCRIPT" "container inventory"
require_pattern 'active-slot' "$DOCTOR_SCRIPT" "blue/green active slot check"
require_pattern 'docker logs' "$DOCTOR_SCRIPT" "container log capture hint"

require_pattern 'oci-staging-doctor.sh' "$HOOK_SCRIPT" "hook doctor integration"

printf '[staging-cd-contract] contract checks passed\n'
