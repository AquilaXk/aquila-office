# Aquila Office

Aquila Office is a Java 21 + Spring Boot project.

Completion criteria are tracked in [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md).

## Backend

```bash
cd back
./gradlew test
./gradlew bootRun
```

Default local API:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/actuator/health`

## Frontend

```bash
cd front
npm install
npm run dev
```

Default local app:

- `http://localhost:3000`

Environment:

- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`

## Vertex AI

Backend chat intent analysis uses Vertex AI Gemini.

Required runtime values:

- `GOOGLE_CLOUD_PROJECT_ID=grepp-ai-project`
- `VERTEX_AI_LOCATION=us-central1`
- `VERTEX_AI_MODEL=gemini-2.5-flash`
- `GOOGLE_APPLICATION_CREDENTIALS`

For Docker Compose, keep the ignored service account key at `vertex-key.json` or set `VERTEX_AI_CREDENTIALS_HOST_PATH` to another local path. Compose mounts that file read-only at `/run/secrets/vertex-key.json`, which is the default `GOOGLE_APPLICATION_CREDENTIALS` value in `.env.example`.

For direct backend runs outside Docker, set `GOOGLE_APPLICATION_CREDENTIALS` to the absolute local path of `vertex-key.json`. The key file is intentionally ignored by git.

## Workflow

1. Create an issue for the work item.
2. Create a focused branch.
3. Implement and verify locally.
4. Open a pull request that links the issue.

Local agent documents such as `AGENTS.md`, `docs/`, and `.codex/` are intentionally ignored.
