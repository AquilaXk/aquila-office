# Aquila Office

Aquila Office is a Java 21 + Spring Boot backend and Next.js frontend for running AI-assisted workspace, agent, task, artifact, and integration workflows.

Completion criteria are tracked in [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md).

## Quick Start

Prerequisites:

- Java 21
- Node.js 20 or later
- Docker Desktop
- k6 for local smoke checks

Create a local environment file from the tracked sample, then keep private values local:

```bash
cp .env.example .env
```

Run the full local stack with PostgreSQL, backend, frontend, and the nginx entrypoint:

```bash
docker compose --env-file .env.example -f compose.dev.yml up --build
```

Run backend checks and the local backend directly:

```bash
./back/gradlew -p back check
./back/gradlew -p back bootRun
```

Run frontend checks and the local frontend directly:

```bash
npm --prefix front install
npm --prefix front run build
npm --prefix front run dev
```

Default local endpoints:

- Backend API: `http://localhost:8080`
- Frontend app: `http://localhost:3000`
- Health: `GET /api/health`
- Actuator liveness: `GET /actuator/health/liveness`
- Actuator readiness: `GET /actuator/health/readiness`
- Prometheus metrics: `GET /actuator/prometheus`

## Environment Variables

Keep sensitive values out of git. `vertex-key.json` is intentionally ignored and can be mounted by Docker Compose through `VERTEX_AI_CREDENTIALS_HOST_PATH`.

| Name | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_DB` | `aquila_office` | Local PostgreSQL database name |
| `POSTGRES_USER` | `aquila` | Local PostgreSQL user |
| `POSTGRES_PASSWORD` | `aquila_dev_password` | Local PostgreSQL password |
| `BACKEND_PORT` | `8080` | Host port for the backend container |
| `FRONTEND_PORT` | `3000` | Host port for the frontend container |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Frontend API base URL |
| `CUSTOM_CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:3000,http://localhost` | Browser origins allowed by the backend |
| `AQUILA_AUTH_TOKEN_ISSUER` | `aquila-office` | JWT issuer |
| `AQUILA_AUTH_TOKEN_SECRET` | local development secret | JWT signing secret; replace outside local runs |
| `AQUILA_AUTH_ACCESS_TOKEN_TTL` | `15m` | Access token lifetime |
| `AQUILA_AUTH_REFRESH_TOKEN_TTL` | `14d` | Refresh token lifetime |
| `AQUILA_EXTERNAL_TOKEN_KEY` | local development key | AES-GCM key for external tokens; replace outside local runs |
| `AQUILA_EXTERNAL_TOKEN_KEY_ID` | `local` | External token key identifier |
| `AQUILA_GOOGLE_OAUTH_CLIENT_IDS` | development client ID | Accepted Google OAuth client IDs |
| `AQUILA_GOOGLE_ALLOWED_HOSTED_DOMAINS` | empty | Optional Google hosted-domain allow list |
| `AQUILA_GOOGLE_REQUIRE_EMAIL_VERIFIED` | `true` | Require verified Google email claims |
| `GOOGLE_CLOUD_PROJECT_ID` | `grepp-ai-project` | Vertex AI project ID |
| `VERTEX_AI_LOCATION` | `us-central1` | Vertex AI region |
| `VERTEX_AI_MODEL` | `gemini-2.5-flash` | Vertex AI model |
| `GOOGLE_APPLICATION_CREDENTIALS` | `/run/secrets/vertex-key.json` | Runtime credential file path |
| `VERTEX_AI_CREDENTIALS_HOST_PATH` | `./vertex-key.json` | Local service account key path mounted into Compose |
| `AQUILA_SLACK_CLIENT_ID` | empty | Slack OAuth client ID |
| `AQUILA_SLACK_CLIENT_SECRET` | empty | Slack OAuth client secret |
| `AQUILA_SLACK_SIGNING_SECRET` | empty | Slack event signature secret |
| `AQUILA_INVITE_EMAIL_ENABLED` | `false` | Enable SMTP invite delivery |
