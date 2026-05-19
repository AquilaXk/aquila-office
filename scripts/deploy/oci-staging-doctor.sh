#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-aquila-office-back}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/aquila-office}"
DOCKER_DATA_ROOT="${DOCKER_DATA_ROOT:-/var/lib/aquila-data}"
IMAGE_REF="${IMAGE_REF:-}"
INCLUDE_LOGS=false
LOG_TAIL="${LOG_TAIL:-120}"
PHASE="manual"

usage() {
    cat <<USAGE
Usage: oci-staging-doctor.sh [--phase NAME] [--image IMAGE] [--include-logs]

Prints non-secret OCI staging diagnostics for GitHub Actions artifacts.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --phase)
            PHASE="${2:?--phase requires a name}"
            shift 2
            ;;
        --image)
            IMAGE_REF="${2:?--image requires a value}"
            shift 2
            ;;
        --include-logs)
            INCLUDE_LOGS=true
            shift
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

section() {
    printf '\n## %s\n' "$*"
}

run_optional() {
    printf '$'
    printf ' %q' "$@"
    printf '\n'
    "$@" || true
}

echo "# OCI staging doctor"
echo "- phase: $PHASE"
echo "- app: $APP_NAME"
if [[ -n "$IMAGE_REF" ]]; then
    echo "- image: $IMAGE_REF"
fi

section "Docker daemon"
command -v docker
docker info --format 'ServerVersion={{.ServerVersion}} Driver={{.Driver}} DockerRootDir={{.DockerRootDir}}'

section "Disk and data root"
run_optional df -h "$DEPLOY_ROOT"
run_optional df -h "$DOCKER_DATA_ROOT"
if [[ -f /etc/docker/daemon.json ]]; then
    echo "/etc/docker/daemon.json exists"
    grep -E '"data-root"|"log-driver"|"log-opts"' /etc/docker/daemon.json || true
else
    echo "/etc/docker/daemon.json is missing"
fi

section "Blue/green state"
active_slot_file="$DEPLOY_ROOT/backend/active-slot"
if [[ -f "$active_slot_file" ]]; then
    printf 'active-slot='
    tr -d '[:space:]' < "$active_slot_file"
    printf '\n'
else
    echo "active-slot file is missing"
fi

section "Container inventory"
docker ps -a \
    --filter "name=${APP_NAME}" \
    --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'

for slot in blue green; do
    container="${APP_NAME}-${slot}"
    if docker inspect "$container" >/dev/null 2>&1; then
        echo ""
        echo "container=$container"
        docker inspect "$container" \
            --format 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} started={{.State.StartedAt}} finished={{.State.FinishedAt}}'
        if [[ "$INCLUDE_LOGS" == "true" ]]; then
            echo "docker logs --tail $LOG_TAIL $container"
            docker logs --tail "$LOG_TAIL" "$container" 2>&1 || true
        fi
    fi
done

section "Image inventory"
if [[ -n "$IMAGE_REF" ]]; then
    docker image inspect "$IMAGE_REF" --format 'image={{.RepoTags}} id={{.Id}} created={{.Created}}' || true
fi
docker images "$APP_NAME" --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}' || true
