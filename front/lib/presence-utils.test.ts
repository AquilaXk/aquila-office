import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();

test("workspace presence client exposes stream and position contracts", () => {
  const source = readFileSync(path.join(appRoot, "lib/presence.ts"), "utf8");

  assert.match(source, /type WorkspacePresenceMessage/);
  assert.match(source, /type UpdateWorkspacePresencePositionInput/);
  assert.match(source, /workspacePresenceStreamPath/);
  assert.match(source, /workspacePresencePositionPath/);
  assert.match(source, /createWorkspacePresenceStream/);
  assert.match(source, /updateWorkspacePresencePosition/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/presence\/stream/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/presence\/me\/position/);
});
