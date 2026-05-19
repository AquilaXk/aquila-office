#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-}"
DOCKER_NETWORK="${DOCKER_NETWORK:-aquila-office}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-aquila-postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:17-alpine}"
POSTGRES_VOLUME="${POSTGRES_VOLUME:-aquila-office-postgres-data}"
MANAGED_POSTGRES_ENABLED="${MANAGED_POSTGRES_ENABLED:-auto}"
POSTGRES_READY_RETRIES="${POSTGRES_READY_RETRIES:-30}"
POSTGRES_READY_INTERVAL_SECONDS="${POSTGRES_READY_INTERVAL_SECONDS:-2}"
DRY_RUN="${DRY_RUN:-false}"

usage() {
    cat <<USAGE
Usage: ensure-postgres-service.sh [--dry-run] --env-file PATH [--network NAME]

Ensures the staging Postgres container exists when the backend datasource uses
the managed Docker hostname. Secret values are never printed.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --env-file)
            ENV_FILE="${2:?--env-file requires a path}"
            shift 2
            ;;
        --network)
            DOCKER_NETWORK="${2:?--network requires a name}"
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

log() {
    printf '[ensure-postgres-service] %s\n' "$*"
}

env_value() {
    local key="$1"
    local line
    line="$(grep -E "^[[:space:]]*(export[[:space:]]+)?${key}=" "$ENV_FILE" | tail -1 || true)"
    if [[ -z "$line" ]]; then
        return 1
    fi
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line#export }"
    printf '%s' "${line#*=}"
}

required_env_value() {
    local key="$1"
    local value
    if ! value="$(env_value "$key")"; then
        echo "missing required backend env key: $key" >&2
        exit 1
    fi
    printf '%s' "$value"
}

strip_optional_quotes() {
    local value="$1"
    if [[ "${value:0:1}" == '"' && "${value: -1}" == '"' ]]; then
        value="${value#\"}"
        value="${value%\"}"
    elif [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
        value="${value#\'}"
        value="${value%\'}"
    fi
    printf '%s' "$value"
}

parse_jdbc_url() {
    local url="$1"
    if [[ "$url" != jdbc:postgresql://* ]]; then
        echo "backend datasource is not a PostgreSQL JDBC URL; skipping managed Postgres" >&2
        return 1
    fi

    local remainder authority path
    remainder="${url#jdbc:postgresql://}"
    authority="${remainder%%/*}"
    if [[ "$authority" == "$remainder" ]]; then
        echo "backend datasource URL does not include a database name" >&2
        return 1
    fi

    POSTGRES_HOST="${authority%%:*}"
    path="${remainder#*/}"
    POSTGRES_DB="${path%%\?*}"
    if [[ -z "$POSTGRES_HOST" || -z "$POSTGRES_DB" ]]; then
        echo "backend datasource URL is missing host or database name" >&2
        return 1
    fi
}

should_manage_postgres() {
    case "$MANAGED_POSTGRES_ENABLED" in
        true)
            return 0
            ;;
        false)
            return 1
            ;;
        auto)
            [[ "$POSTGRES_HOST" == "$POSTGRES_CONTAINER" || "$POSTGRES_HOST" == "aquila-postgres" || "$POSTGRES_HOST" == "postgres" ]]
            ;;
        *)
            echo "MANAGED_POSTGRES_ENABLED must be true, false, or auto" >&2
            exit 2
            ;;
    esac
}

ensure_network() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would ensure Docker network exists"
        return
    fi
    if ! docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1; then
        docker network create "$DOCKER_NETWORK" >/dev/null
    fi
}

container_exists() {
    docker inspect "$POSTGRES_CONTAINER" >/dev/null 2>&1
}

container_running() {
    [[ "$(docker inspect "$POSTGRES_CONTAINER" --format '{{.State.Running}}' 2>/dev/null || true)" == "true" ]]
}

ensure_network_alias() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would connect Postgres container to deploy network"
        return
    fi
    if docker inspect "$POSTGRES_CONTAINER" --format '{{json .NetworkSettings.Networks}}' | grep -q "\"$DOCKER_NETWORK\""; then
        return
    fi
    docker network connect --alias "$POSTGRES_HOST" "$DOCKER_NETWORK" "$POSTGRES_CONTAINER"
}

create_or_start_container() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would ensure managed Postgres container name=$POSTGRES_CONTAINER image=$POSTGRES_IMAGE volume=$POSTGRES_VOLUME"
        return
    fi

    if container_exists; then
        ensure_network_alias
        if ! container_running; then
            docker start "$POSTGRES_CONTAINER" >/dev/null
        fi
        log "managed Postgres container already exists"
        return
    fi

    docker volume create "$POSTGRES_VOLUME" >/dev/null
    docker run -d \
        --name "$POSTGRES_CONTAINER" \
        --network "$DOCKER_NETWORK" \
        --network-alias "$POSTGRES_HOST" \
        -e "POSTGRES_DB=$POSTGRES_DB" \
        -e "POSTGRES_USER=$POSTGRES_USER" \
        -e "POSTGRES_PASSWORD=$POSTGRES_PASSWORD" \
        -v "$POSTGRES_VOLUME:/var/lib/postgresql/data" \
        --restart unless-stopped \
        "$POSTGRES_IMAGE" >/dev/null
    log "managed Postgres container created"
}

wait_for_postgres() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log "would wait for managed Postgres readiness"
        return
    fi

    for attempt in $(seq 1 "$POSTGRES_READY_RETRIES"); do
        if docker exec "$POSTGRES_CONTAINER" pg_isready -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" &&
            [[ "$(docker exec -e "PGPASSWORD=$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" psql -qAt -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select 1" 2>/dev/null)" == "1" ]]; then
            log "managed Postgres is ready"
            return
        fi
        sleep "$POSTGRES_READY_INTERVAL_SECONDS"
    done

    echo "managed Postgres did not become ready" >&2
    exit 1
}

if [[ -z "$ENV_FILE" ]]; then
    echo "--env-file is required" >&2
    usage >&2
    exit 2
fi
if [[ ! -f "$ENV_FILE" ]]; then
    echo "env file not found: $ENV_FILE" >&2
    exit 1
fi

SPRING_DATASOURCE_URL="$(strip_optional_quotes "$(required_env_value SPRING_DATASOURCE_URL)")"
POSTGRES_USER="$(strip_optional_quotes "$(required_env_value SPRING_DATASOURCE_USERNAME)")"
POSTGRES_PASSWORD="$(strip_optional_quotes "$(required_env_value SPRING_DATASOURCE_PASSWORD)")"

parse_jdbc_url "$SPRING_DATASOURCE_URL"

if ! should_manage_postgres; then
    log "backend datasource does not use managed Postgres host; skipping"
    exit 0
fi

ensure_network
create_or_start_container
wait_for_postgres
