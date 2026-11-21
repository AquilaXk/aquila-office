#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/aquila-office}"
APP_NAME="${APP_NAME:-aquila-office-front}"
IMAGE_REF="${IMAGE_REF:-aquila-office-front:local}"
IMAGE_TAR="${IMAGE_TAR:-}"
ENV_SOURCE="${ENV_SOURCE:-}"
DOCKER_NETWORK="${DOCKER_NETWORK:-aquila-office}"
BLUE_GREEN_SCRIPT="${BLUE_GREEN_SCRIPT:-$SCRIPT_DIR/frontend-blue-green.sh}"
DOCTOR_SCRIPT="${DOCTOR_SCRIPT:-$SCRIPT_DIR/oci-staging-doctor.sh}"
DRY_RUN="${DRY_RUN:-false}"
TARGET_SLOT=""

usage() {
    cat <<USAGE
Usage: oci-frontend-hook.sh [--dry-run] [--slot blue|green] [--image IMAGE] [--image-tar PATH] [--env-file PATH] [--doctor-script PATH]

Prepares frontend staging directories, installs the frontend env file, loads the
image tar, runs non-secret diagnostics, and delegates slot switching.
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
        --env-file)
            ENV_SOURCE="${2:?--env-file requires a path}"
            shift 2
            ;;
        --doctor-script)
            DOCTOR_SCRIPT="${2:?--doctor-script requires a path}"
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
    printf '[oci-frontend-hook] %s\n' "$*"
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

run_doctor() {
    local phase="$1"
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would run staging doctor phase=$phase script=$DOCTOR_SCRIPT"
        return
    fi
    APP_NAME="$APP_NAME" DEPLOY_ROOT="$DEPLOY_ROOT" bash "$DOCTOR_SCRIPT" --phase "$phase" --image "$IMAGE_REF"
}

install_env_file() {
    if [[ -z "$ENV_SOURCE" ]]; then
        log "no env file provided; keeping existing $DEPLOY_ROOT/frontend/.env if present"
        return
    fi
    if [[ "$DRY_RUN" != "true" && ! -f "$ENV_SOURCE" ]]; then
        echo "env file not found: $ENV_SOURCE" >&2
        exit 1
    fi
    run install -d "$DEPLOY_ROOT/frontend"
    run install -m 600 "$ENV_SOURCE" "$DEPLOY_ROOT/frontend/.env"
}

ensure_network() {
    if [[ "$DRY_RUN" == "true" ]]; then
        run docker network create "$DOCKER_NETWORK"
        return
    fi
    if docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1; then
        log "docker network already exists: $DOCKER_NETWORK"
    else
        docker network create "$DOCKER_NETWORK"
    fi
}

if [[ "$DRY_RUN" != "true" && ! -x "$BLUE_GREEN_SCRIPT" ]]; then
    echo "blue/green script is not executable: $BLUE_GREEN_SCRIPT" >&2
    exit 1
fi

log "preparing deploy root=$DEPLOY_ROOT image=$IMAGE_REF dry_run=$DRY_RUN"

run install -d "$DEPLOY_ROOT/frontend" "$DEPLOY_ROOT/releases"
install_env_file
run_doctor before-frontend

if [[ -n "$IMAGE_TAR" ]]; then
    if [[ "$DRY_RUN" != "true" && ! -f "$IMAGE_TAR" ]]; then
        echo "image tar not found: $IMAGE_TAR" >&2
        exit 1
    fi
    run docker load -i "$IMAGE_TAR"
fi

ensure_network

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
ACTIVE_SLOT_FILE="$DEPLOY_ROOT/frontend/active-slot" \
ENV_FILE="$DEPLOY_ROOT/frontend/.env" \
SKIP_IMAGE_PULL="$([[ -n "$IMAGE_TAR" ]] && echo true || echo false)" \
"${blue_green_args[@]}"

run_doctor after-frontend
log "frontend deployment hook finished"
