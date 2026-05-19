#!/usr/bin/env bash
set -euo pipefail

GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-AquilaXk/aquila-office}"
GITHUB_RUNNER_LABELS="${GITHUB_RUNNER_LABELS:-oci-a1-staging}"
GITHUB_RUNNER_NAME="${GITHUB_RUNNER_NAME:-oci-a1-staging}"
GITHUB_RUNNER_TOKEN="${GITHUB_RUNNER_TOKEN:-}"
ACTIONS_RUNNER_VERSION="${ACTIONS_RUNNER_VERSION:-2.334.0}"
RUNNER_USER="${RUNNER_USER:-actions-runner}"
RUNNER_HOME="${RUNNER_HOME:-/opt/actions-runner/aquila-office}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/aquila-office}"
DOCKER_DATA_ROOT="${DOCKER_DATA_ROOT:-/var/lib/aquila-data/docker}"

if [[ -z "$GITHUB_RUNNER_TOKEN" ]]; then
    echo "GITHUB_RUNNER_TOKEN is required" >&2
    exit 2
fi

if [[ "$(uname -m)" != "aarch64" && "$(uname -m)" != "arm64" ]]; then
    echo "this bootstrap expects an ARM64 OCI A1 host" >&2
    exit 1
fi

log() {
    printf '[bootstrap-oci-staging-host] %s\n' "$*"
}

require_sudo() {
    if [[ "$(id -u)" == "0" ]]; then
        return
    fi
    sudo -n true
}

require_sudo

log "installing runtime packages"
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    ca-certificates \
    curl \
    docker.io \
    git \
    jq \
    nginx \
    sudo \
    tar

log "configuring docker data root"
sudo install -d "$DOCKER_DATA_ROOT"
sudo install -d /etc/docker
sudo tee /etc/docker/daemon.json >/dev/null <<JSON
{
  "data-root": "$DOCKER_DATA_ROOT"
}
JSON
sudo systemctl enable docker
sudo systemctl restart docker

log "configuring nginx staging proxy"
sudo install -d /etc/nginx/conf.d
sudo tee /etc/nginx/conf.d/aquila-office-backend-upstream.conf >/dev/null <<'NGINX'
upstream aquila_office_backend {
    server 127.0.0.1:18081;
    keepalive 16;
}
NGINX
sudo tee /etc/nginx/conf.d/aquila-office-frontend-upstream.conf >/dev/null <<'NGINX'
upstream aquila_office_frontend {
    server 127.0.0.1:13001;
    keepalive 16;
}
NGINX
sudo tee /etc/nginx/conf.d/aquila-office-staging.conf >/dev/null <<'NGINX'
server {
    listen 80;
    server_tokens off;

    location = /healthz {
        add_header Content-Type text/plain;
        return 200 "ok\n";
    }

    location /api/ {
        proxy_pass http://aquila_office_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    location /actuator/health {
        proxy_pass http://aquila_office_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /v3/api-docs {
        proxy_pass http://aquila_office_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /swagger-ui {
        proxy_pass http://aquila_office_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://aquila_office_frontend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
NGINX
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl restart nginx

log "creating runtime directories"
sudo install -d "$DEPLOY_ROOT/backend" "$DEPLOY_ROOT/frontend" "$DEPLOY_ROOT/releases"

log "creating runner user"
if ! id "$RUNNER_USER" >/dev/null 2>&1; then
    sudo useradd --system --create-home --shell /bin/bash "$RUNNER_USER"
fi
sudo usermod -aG docker "$RUNNER_USER"

log "granting runner deploy permissions"
sudo chown -R "$RUNNER_USER:$RUNNER_USER" "$DEPLOY_ROOT"
sudo chown "$RUNNER_USER:$RUNNER_USER" \
    /etc/nginx/conf.d/aquila-office-backend-upstream.conf \
    /etc/nginx/conf.d/aquila-office-frontend-upstream.conf
sudo tee /etc/sudoers.d/aquila-office-runner-nginx >/dev/null <<SUDOERS
$RUNNER_USER ALL=(root) NOPASSWD: /usr/sbin/nginx -t, /usr/sbin/nginx -s reload
SUDOERS
sudo chmod 440 /etc/sudoers.d/aquila-office-runner-nginx
sudo visudo -cf /etc/sudoers.d/aquila-office-runner-nginx

log "installing GitHub Actions runner"
sudo install -d "$RUNNER_HOME"
sudo chown "$RUNNER_USER:$RUNNER_USER" "$RUNNER_HOME"

runner_tar="actions-runner-linux-arm64-${ACTIONS_RUNNER_VERSION}.tar.gz"
runner_url="https://github.com/actions/runner/releases/download/v${ACTIONS_RUNNER_VERSION}/${runner_tar}"
if [[ ! -x "$RUNNER_HOME/config.sh" ]]; then
    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "$tmp_dir"' EXIT
    curl -fsSL "$runner_url" -o "$tmp_dir/$runner_tar"
    sudo -u "$RUNNER_USER" tar -xzf "$tmp_dir/$runner_tar" -C "$RUNNER_HOME"
fi

if [[ -x "$RUNNER_HOME/bin/installdependencies.sh" ]]; then
    sudo "$RUNNER_HOME/bin/installdependencies.sh"
fi

if [[ -f "$RUNNER_HOME/.runner" ]]; then
    log "runner is already configured; replacing registration"
    sudo -u "$RUNNER_USER" "$RUNNER_HOME/config.sh" remove --unattended --token "$GITHUB_RUNNER_TOKEN" || true
fi

sudo -u "$RUNNER_USER" "$RUNNER_HOME/config.sh" \
    --url "https://github.com/${GITHUB_REPOSITORY}" \
    --token "$GITHUB_RUNNER_TOKEN" \
    --name "$GITHUB_RUNNER_NAME" \
    --labels "$GITHUB_RUNNER_LABELS" \
    --work _work \
    --unattended \
    --replace

sudo "$RUNNER_HOME/svc.sh" install "$RUNNER_USER" || true
sudo "$RUNNER_HOME/svc.sh" start

log "bootstrap finished"
