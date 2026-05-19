#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-aquila-office-back}"
IMAGE_REF="${IMAGE_REF:-aquila-office-back:local}"
ACTIVE_SLOT_FILE="${ACTIVE_SLOT_FILE:-/opt/aquila-office/backend/active-slot}"
ENV_FILE="${ENV_FILE:-/opt/aquila-office/backend/.env}"
BLUE_PORT="${BLUE_PORT:-18081}"
GREEN_PORT="${GREEN_PORT:-18082}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-2}"
UPSTREAM_FILE="${UPSTREAM_FILE:-/etc/nginx/conf.d/aquila-office-backend-upstream.conf}"
RELOAD_COMMAND="${RELOAD_COMMAND:-nginx -t && nginx -s reload}"
DOCKER_NETWORK="${DOCKER_NETWORK:-bridge}"
SKIP_IMAGE_PULL="${SKIP_IMAGE_PULL:-false}"
DRY_RUN="${DRY_RUN:-false}"
TARGET_SLOT=""

usage() {
    cat <<USAGE
Usage: backend-blue-green.sh [--dry-run] [--slot blue|green] [--image IMAGE]

Environment:
  APP_NAME                     Default: aquila-office-back
  IMAGE_REF                    Default: aquila-office-back:local
  ACTIVE_SLOT_FILE             Default: /opt/aquila-office/backend/active-slot
  ENV_FILE                     Default: /opt/aquila-office/backend/.env
  BLUE_PORT                    Default: 18081
  GREEN_PORT                   Default: 18082
  CONTAINER_PORT               Default: 8080
  HEALTH_PATH                  Default: /actuator/health
  UPSTREAM_FILE                Default: /etc/nginx/conf.d/aquila-office-backend-upstream.conf
  RELOAD_COMMAND               Default: nginx -t && nginx -s reload
  DOCKER_NETWORK               Default: bridge
  SKIP_IMAGE_PULL              Default: false
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
    printf '[backend-blue-green] %s\n' "$*"
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

current_slot() {
    if [[ -f "$ACTIVE_SLOT_FILE" ]]; then
        tr -d '[:space:]' < "$ACTIVE_SLOT_FILE"
    else
        echo "green"
    fi
}

slot_port() {
    case "$1" in
        blue) echo "$BLUE_PORT" ;;
        green) echo "$GREEN_PORT" ;;
        *) echo "unknown slot: $1" >&2; exit 2 ;;
    esac
}

next_slot() {
    if [[ -n "$TARGET_SLOT" ]]; then
        echo "$TARGET_SLOT"
        return
    fi
    if [[ "$(current_slot)" == "blue" ]]; then
        echo "green"
    else
        echo "blue"
    fi
}

write_active_slot() {
    local slot="$1"
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would write active slot '$slot' to $ACTIVE_SLOT_FILE"
        return
    fi
    install -d "$(dirname "$ACTIVE_SLOT_FILE")"
    printf '%s\n' "$slot" > "$ACTIVE_SLOT_FILE"
}

write_upstream() {
    local port="$1"
    local upstream_dir
    upstream_dir="$(dirname "$UPSTREAM_FILE")"
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would write nginx upstream $UPSTREAM_FILE -> 127.0.0.1:$port"
        return
    fi
    if [[ ! -d "$upstream_dir" ]]; then
        install -d "$upstream_dir"
    fi
    cat > "$UPSTREAM_FILE" <<UPSTREAM
upstream aquila_office_backend {
    server 127.0.0.1:$port;
    keepalive 16;
}
UPSTREAM
}

pull_image() {
    if [[ "$SKIP_IMAGE_PULL" == "true" ]]; then
        log "skipping docker pull because image was loaded from deployment payload"
        return
    fi
    run docker pull "$IMAGE_REF"
}

remove_existing_container() {
    local container_name="$1"
    if [[ "$DRY_RUN" == "true" ]]; then
        run docker rm -f "$container_name"
        return
    fi
    if docker inspect "$container_name" >/dev/null 2>&1; then
        docker rm -f "$container_name"
    else
        log "no existing container named $container_name"
    fi
}

wait_for_health() {
    local port="$1"
    local url="http://127.0.0.1:${port}${HEALTH_PATH}"
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would poll $url"
        return
    fi
    for attempt in $(seq 1 "$HEALTH_RETRIES"); do
        if curl --fail --silent --show-error "$url" | grep -q '"status":"UP"'; then
            log "health check passed on attempt $attempt"
            return
        fi
        sleep "$HEALTH_INTERVAL_SECONDS"
    done
    echo "health check failed for $url" >&2
    exit 1
}

slot="$(next_slot)"
port="$(slot_port "$slot")"
container_name="${APP_NAME}-${slot}"

log "deploying image=$IMAGE_REF slot=$slot port=$port dry_run=$DRY_RUN"

pull_image
remove_existing_container "$container_name"

docker_run_args=(
    docker run -d
    --name "$container_name"
    --network "$DOCKER_NETWORK"
    -p "127.0.0.1:${port}:${CONTAINER_PORT}"
    -e "SPRING_PROFILES_ACTIVE=prod"
)

if [[ -f "$ENV_FILE" || "$DRY_RUN" == "true" ]]; then
    docker_run_args+=(--env-file "$ENV_FILE")
fi

docker_run_args+=("$IMAGE_REF")

run "${docker_run_args[@]}"
wait_for_health "$port"
write_upstream "$port"
run sh -c "$RELOAD_COMMAND"
write_active_slot "$slot"

log "deployment prepared for slot=$slot"
