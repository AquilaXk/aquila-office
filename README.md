# Aquila Office

Aquila Office is a Java 21 + Spring Boot project.

## Backend

```bash
cd back
./gradlew test
./gradlew bootRun
```

Default local API:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/actuator/health`

## Workflow

1. Create an issue for the work item.
2. Create a focused branch.
3. Implement and verify locally.
4. Open a pull request that links the issue.

Local agent documents such as `AGENTS.md`, `docs/`, and `.codex/` are intentionally ignored.
