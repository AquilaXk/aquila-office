import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");
const readme = await readFile(resolve(root, "README.md"), "utf8");

const requiredHeadings = [
  "# Aquila Office",
  "## About the Project",
  "# AI-assisted workspace operations for teams",
  "## Recognition",
  "### Built With",
  "## Contact us",
  "## Stay Up-to-Date",
  "## Getting Started",
  "### Prerequisites",
  "## Development",
  "### Setup",
  "#### Quick start with Docker Compose",
  "#### Development tip",
  "#### Manual setup",
  "#### Setting up your first user",
  "### E2E-Testing",
  "#### Resolving issues",
  "### Upgrading from earlier versions",
  "## Deployment",
  "### Docker",
  "### OCI blue-green",
  "## Roadmap",
  "## License",
  "## Repo Activity",
  "## Contributing",
  "### Good First Issues",
  "### Contributors",
  "## Enabling Content Security Policy",
  "## Single Workspace Mode",
  "## Integrations",
  "### Google OAuth",
  "### Vertex AI",
  "### Gateway",
  "### Slack",
  "### GitHub",
  "## Workflows",
  "### Local smoke checks",
  "### Release preflight",
  "## Acknowledgements",
  "## Troubleshooting",
];

for (const heading of requiredHeadings) {
  assert.match(readme, new RegExp(`^${escapeRegExp(heading)}$`, "m"), `missing heading: ${heading}`);
}

const requiredSnippets = [
  "docker compose --env-file .env.example -f compose.dev.yml up --build",
  "./back/gradlew -p back check",
  "./back/gradlew -p back bootRun",
  "npm --prefix front install",
  "npm --prefix front run test:node",
  "npm --prefix front run lint",
  "npm --prefix front run build",
  "npx --prefix front playwright install chromium",
  "npm --prefix front run test:browser",
  "tools/test/run-k6-smoke.sh",
  "gh issue list --repo AquilaXk/aquila-office --state open --limit 50",
  "gh run list --repo AquilaXk/aquila-office --branch main --limit 10",
  "GET /api/health",
  "GET /actuator/prometheus",
  "POST /api/v1/auth/google/login",
  "GET /api/v1/workspaces/{workspaceId}/agents",
  "POST /api/v1/workspaces/{workspaceId}/tasks/run",
  "GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports",
  "GET /api/v1/workspaces/{workspaceId}/gateway",
  "PATCH /api/v1/workspaces/{workspaceId}/presence/me/position",
  "Java 21",
  "Spring Boot",
  "Next.js",
  "Docker Compose",
  "PostgreSQL",
  "GOOGLE_CLOUD_PROJECT_ID",
  "VERTEX_AI_LOCATION",
  "VERTEX_AI_MODEL",
  "GOOGLE_APPLICATION_CREDENTIALS",
  "AQUILA_AUTH_TOKEN_SECRET",
  "AQUILA_EXTERNAL_TOKEN_KEY",
  "AQUILA_SLACK_CLIENT_ID",
  "Workspace",
  "WorkspaceAgent",
  "Task",
  "TaskExecution",
  "SlackChannelMapping",
  "GithubCredential",
  "Completion criteria are tracked in [COMPLETION_CHECKLIST.md](COMPLETION_CHECKLIST.md).",
];

for (const snippet of requiredSnippets) {
  assert(readme.includes(snippet), `missing README snippet: ${snippet}`);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
