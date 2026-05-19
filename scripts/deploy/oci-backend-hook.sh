#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/aquila-office}"
APP_NAME="${APP_NAME:-aquila-office-back}"
IMAGE_REF="${IMAGE_REF:-aquila-office-back:local}"
IMAGE_TAR="${IMAGE_TAR:-}"
ENV_SOURCE="${ENV_SOURCE:-}"
VERTEX_KEY_SOURCE="${VERTEX_KEY_SOURCE:-}"
DOCKER_NETWORK="${DOCKER_NETWORK:-aquila-office}"
BLUE_GREEN_SCRIPT="${BLUE_GREEN_SCRIPT:-$SCRIPT_DIR/backend-blue-green.sh}"
DOCTOR_SCRIPT="${DOCTOR_SCRIPT:-$SCRIPT_DIR/oci-staging-doctor.sh}"
POSTGRES_SCRIPT="${POSTGRES_SCRIPT:-$SCRIPT_DIR/ensure-postgres-service.sh}"
DRY_RUN="${DRY_RUN:-false}"
TARGET_SLOT=""

usage() {
    cat <<USAGE
Usage: oci-backend-hook.sh [--dry-run] [--slot blue|green] [--image IMAGE] [--image-tar PATH] [--env-file PATH] [--vertex-key-file PATH] [--doctor-script PATH]

This hook is intended to run on the OCI compute host after workflow upload.
It prepares shared directories, installs the backend env file, optionally loads
an image tar, runs non-secret diagnostics, and delegates slot switching to
backend-blue-green.sh.
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
        --vertex-key-file)
            VERTEX_KEY_SOURCE="${2:?--vertex-key-file requires a path}"
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

run_doctor() {
    local phase="$1"
    if [[ -z "$DOCTOR_SCRIPT" ]]; then
        return
    fi
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would run staging doctor phase=$phase script=$DOCTOR_SCRIPT"
        return
    fi
    if [[ ! -f "$DOCTOR_SCRIPT" ]]; then
        echo "doctor script not found: $DOCTOR_SCRIPT" >&2
        exit 1
    fi
    APP_NAME="$APP_NAME" \
    DEPLOY_ROOT="$DEPLOY_ROOT" \
    bash "$DOCTOR_SCRIPT" --phase "$phase" --image "$IMAGE_REF"
}

install_env_file() {
    if [[ -z "$ENV_SOURCE" ]]; then
        log "no env file provided; keeping existing $DEPLOY_ROOT/backend/.env if present"
        return
    fi
    if [[ "$DRY_RUN" != "true" && ! -f "$ENV_SOURCE" ]]; then
        echo "env file not found: $ENV_SOURCE" >&2
        exit 1
    fi
    run install -d "$DEPLOY_ROOT/backend"
    run install -m 600 "$ENV_SOURCE" "$DEPLOY_ROOT/backend/.env"
}

install_vertex_key_file() {
    if [[ -z "$VERTEX_KEY_SOURCE" ]]; then
        log "no Vertex key file provided; keeping existing $DEPLOY_ROOT/backend/vertex-key.json if present"
        return
    fi
    if [[ "$DRY_RUN" != "true" && ! -f "$VERTEX_KEY_SOURCE" ]]; then
        echo "Vertex key file not found: $VERTEX_KEY_SOURCE" >&2
        exit 1
    fi
    run install -d "$DEPLOY_ROOT/backend"
    run install -m 600 "$VERTEX_KEY_SOURCE" "$DEPLOY_ROOT/backend/vertex-key.json"
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

ensure_postgres_service() {
    local postgres_env_file="$DEPLOY_ROOT/backend/.env"
    if [[ ! -f "$POSTGRES_SCRIPT" ]]; then
        echo "Postgres service script not found: $POSTGRES_SCRIPT" >&2
        exit 1
    fi
    if [[ "$DRY_RUN" == "true" && -n "$ENV_SOURCE" ]]; then
        postgres_env_file="$ENV_SOURCE"
    fi
    ENV_FILE="$postgres_env_file" \
    DOCKER_NETWORK="$DOCKER_NETWORK" \
    DRY_RUN="$DRY_RUN" \
    bash "$POSTGRES_SCRIPT" --env-file "$postgres_env_file" --network "$DOCKER_NETWORK"
}

if [[ "$DRY_RUN" != "true" && ! -x "$BLUE_GREEN_SCRIPT" ]]; then
    echo "blue/green script is not executable: $BLUE_GREEN_SCRIPT" >&2
    exit 1
fi

log "preparing deploy root=$DEPLOY_ROOT image=$IMAGE_REF dry_run=$DRY_RUN"

run install -d "$DEPLOY_ROOT/backend" "$DEPLOY_ROOT/releases"
install_env_file
install_vertex_key_file
run_doctor before

if [[ -n "$IMAGE_TAR" ]]; then
    if [[ "$DRY_RUN" != "true" && ! -f "$IMAGE_TAR" ]]; then
        echo "image tar not found: $IMAGE_TAR" >&2
        exit 1
    fi
    run docker load -i "$IMAGE_TAR"
fi

ensure_network
ensure_postgres_service

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

if [[ -n "$VERTEX_KEY_SOURCE" ]]; then
    blue_green_args+=(--vertex-key-file "$DEPLOY_ROOT/backend/vertex-key.json")
fi

blue_green_env=(
    "APP_NAME=$APP_NAME"
    "IMAGE_REF=$IMAGE_REF"
    "DOCKER_NETWORK=$DOCKER_NETWORK"
    "ACTIVE_SLOT_FILE=$DEPLOY_ROOT/backend/active-slot"
    "ENV_FILE=$DEPLOY_ROOT/backend/.env"
    "SKIP_IMAGE_PULL=$([[ -n "$IMAGE_TAR" ]] && echo true || echo false)"
)

if [[ -n "$VERTEX_KEY_SOURCE" ]]; then
    blue_green_env+=("VERTEX_KEY_FILE=$DEPLOY_ROOT/backend/vertex-key.json")
fi

env "${blue_green_env[@]}" "${blue_green_args[@]}"

run_doctor after
log "backend deployment hook finished"
