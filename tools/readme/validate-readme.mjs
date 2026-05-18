import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const readme = await readFile(resolve(root, "README.md"), "utf8");

const requiredHeadings = [
  "# Aquila Office",
  "## Quick Start",
  "## Environment Variables",
  "## API Map",
  "## ERD Summary",
  "## Operations",
  "## Troubleshooting",
];

for (const heading of requiredHeadings) {
  assert.match(readme, new RegExp(`^${escapeRegExp(heading)}$`, "m"), `missing heading: ${heading}`);
}

const requiredSnippets = [
  "docker compose --env-file .env.example -f compose.dev.yml up --build",
  "./back/gradlew -p back check",
  "npm --prefix front run build",
  "tools/test/run-k6-smoke.sh",
  "GET /api/health",
  "GET /actuator/prometheus",
  "POST /api/v1/auth/google/login",
  "GET /api/v1/workspaces/{workspaceId}/agents",
  "POST /api/v1/workspaces/{workspaceId}/tasks/run",
  "GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports",
  "GOOGLE_CLOUD_PROJECT_ID",
  "VERTEX_AI_LOCATION",
  "VERTEX_AI_MODEL",
  "GOOGLE_APPLICATION_CREDENTIALS",
  "AQUILA_AUTH_TOKEN_SECRET",
  "Workspace",
  "WorkspaceAgent",
  "Task",
  "TaskExecution",
  "SlackChannelMapping",
  "GithubCredential",
];

for (const snippet of requiredSnippets) {
  assert(readme.includes(snippet), `missing README snippet: ${snippet}`);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
