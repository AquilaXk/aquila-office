#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW_FILE="$ROOT_DIR/.github/workflows/backend-cd.yml"
BLUE_GREEN_ENTRYPOINT="$ROOT_DIR/scripts/deploy-bluegreen.sh"
BLUE_GREEN_SCRIPT="$ROOT_DIR/scripts/deploy/backend-blue-green.sh"
OCI_HOOK_SCRIPT="$ROOT_DIR/scripts/deploy/oci-backend-hook.sh"

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

log "checking shell syntax"
bash -n "$BLUE_GREEN_ENTRYPOINT" "$BLUE_GREEN_SCRIPT" "$OCI_HOOK_SCRIPT" "$0"

log "checking workflow contract"
require_pattern '^  workflow_dispatch:' "$WORKFLOW_FILE"
require_pattern 'dry_run:' "$WORKFLOW_FILE"
require_pattern 'docker build -f docker/Dockerfile' "$WORKFLOW_FILE"
require_pattern 'docker save "\$IMAGE_REF" -o backend-image.tar' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy-bluegreen.sh' "$WORKFLOW_FILE"
require_pattern 'scripts/deploy/oci-backend-hook.sh' "$WORKFLOW_FILE"
require_pattern 'OCI_SSH_PRIVATE_KEY' "$WORKFLOW_FILE"
require_pattern 'OCI_BACKEND_HOST' "$WORKFLOW_FILE"

log "running blue/green entrypoint dry-run"
"$BLUE_GREEN_ENTRYPOINT" --dry-run --image aquila-office-back:dry-run

log "running OCI hook dry-run"
"$OCI_HOOK_SCRIPT" --dry-run --image aquila-office-back:dry-run --image-tar /tmp/aquila-office-back-dry-run.tar

log "backend CD dry-run validation passed"
