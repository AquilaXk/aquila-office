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

## Workflow

1. Create an issue for the work item.
2. Create a focused branch.
3. Implement and verify locally.
4. Open a pull request that links the issue.

Local agent documents such as `AGENTS.md`, `docs/`, and `.codex/` are intentionally ignored.
