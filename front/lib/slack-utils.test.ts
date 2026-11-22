import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

test("slack client exposes install and integration API contracts", () => {
  const source = readFileSync(path.join(appRoot, "lib/slack.ts"), "utf8");

  assert.match(source, /type SlackIntegration/);
  assert.match(source, /type CreateSlackIntegrationInput/);
  assert.match(source, /type UpdateSlackIntegrationInput/);
  assert.match(source, /fetchSlackInstallUrl/);
  assert.match(source, /fetchSlackIntegrations/);
  assert.match(source, /createSlackIntegration/);
  assert.match(source, /updateSlackIntegration/);
  assert.match(source, /deleteSlackIntegration/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/slack\/install/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/slack\/integrations/);
});

test("slack client exposes channel mapping API contracts", () => {
  const source = readFileSync(path.join(appRoot, "lib/slack.ts"), "utf8");

  assert.match(source, /type SlackChannelMapping/);
  assert.match(source, /type CreateSlackChannelMappingInput/);
  assert.match(source, /slackChannelMappingsPath/);
  assert.match(source, /fetchSlackChannelMappings/);
  assert.match(source, /createSlackChannelMapping/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/slack\/channel-mappings/);
});
