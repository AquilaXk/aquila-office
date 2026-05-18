#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/aquila-office}"
APP_NAME="${APP_NAME:-aquila-office-back}"
IMAGE_REF="${IMAGE_REF:-aquila-office-back:local}"
IMAGE_TAR="${IMAGE_TAR:-}"
DOCKER_NETWORK="${DOCKER_NETWORK:-aquila-office}"
BLUE_GREEN_SCRIPT="${BLUE_GREEN_SCRIPT:-$SCRIPT_DIR/backend-blue-green.sh}"
DRY_RUN="${DRY_RUN:-false}"
TARGET_SLOT=""

usage() {
    cat <<USAGE
Usage: oci-backend-hook.sh [--dry-run] [--slot blue|green] [--image IMAGE] [--image-tar PATH]

This hook is intended to run on the OCI compute host after workflow upload.
It prepares shared directories, optionally loads an image tar, ensures the Docker
network exists, and delegates slot switching to backend-blue-green.sh.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --slot)
            TARGET_SLOT="${2:?--slot requires blue or green}"
            shift 2
            ;;
        --image)
            IMAGE_REF="${2:?--image requires an image reference}"
            shift 2
            ;;
        --image-tar)
            IMAGE_TAR="${2:?--image-tar requires a path}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ "$TARGET_SLOT" != "" && "$TARGET_SLOT" != "blue" && "$TARGET_SLOT" != "green" ]]; then
    echo "--slot must be blue or green" >&2
    exit 2
fi

log() {
    printf '[oci-backend-hook] %s\n' "$*"
}

run() {
    if [[ "$DRY_RUN" == "true" ]]; then
        printf '[dry-run]'
        printf ' %q' "$@"
        printf '\n'
        return 0
    fi
    "$@"
}

if [[ "$DRY_RUN" != "true" && ! -x "$BLUE_GREEN_SCRIPT" ]]; then
    echo "blue/green script is not executable: $BLUE_GREEN_SCRIPT" >&2
    exit 1
fi

log "preparing deploy root=$DEPLOY_ROOT image=$IMAGE_REF dry_run=$DRY_RUN"

run install -d "$DEPLOY_ROOT/backend" "$DEPLOY_ROOT/releases"

if [[ -n "$IMAGE_TAR" ]]; then
    if [[ "$DRY_RUN" != "true" && ! -f "$IMAGE_TAR" ]]; then
        echo "image tar not found: $IMAGE_TAR" >&2
        exit 1
    fi
    run docker load -i "$IMAGE_TAR"
fi

run docker network create "$DOCKER_NETWORK"

blue_green_args=(
    "$BLUE_GREEN_SCRIPT"
    --image "$IMAGE_REF"
)

if [[ "$DRY_RUN" == "true" ]]; then
    blue_green_args+=(--dry-run)
fi

if [[ -n "$TARGET_SLOT" ]]; then
    blue_green_args+=(--slot "$TARGET_SLOT")
fi

APP_NAME="$APP_NAME" \
IMAGE_REF="$IMAGE_REF" \
DOCKER_NETWORK="$DOCKER_NETWORK" \
ACTIVE_SLOT_FILE="$DEPLOY_ROOT/backend/active-slot" \
ENV_FILE="$DEPLOY_ROOT/backend/.env" \
"${blue_green_args[@]}"

log "backend deployment hook finished"
