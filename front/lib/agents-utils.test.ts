import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  MAX_AGENT_SKILL_CONTENT_CHARS,
  MAX_AGENT_SKILL_FILES,
  normalizeWorkspaceAgents,
  validateAgentSkillFiles,
  workspaceAgentPath,
  workspaceAgentsPath,
  type WorkspaceAgent,
} from "./agents-utils.ts";

const agent: WorkspaceAgent = {
  agentId: 501,
  workspaceId: 101,
  name: "Backend Agent",
  category: "BACKEND",
  openClawAgentId: "gateway-agent-1",
  workspacePath: ".aquila/workspaces/101",
  status: "READY",
  syncError: null,
  createdByGoogleSub: "google-1",
  createdAt: "2026-05-16T10:00:00",
  updatedAt: "2026-05-16T10:00:00",
  skillFiles: [
    {
      skillFileId: 601,
      fileName: "AGENTS.md",
      syncStatus: "SYNCED",
      syncError: null,
    },
  ],
};

test("workspace agent paths target a single workspace", () => {
  assert.equal(workspaceAgentsPath(101), "/api/v1/workspaces/101/agents");
  assert.equal(workspaceAgentPath(101, 501), "/api/v1/workspaces/101/agents/501");
});

test("normalizeWorkspaceAgents accepts direct arrays", () => {
  assert.deepEqual(normalizeWorkspaceAgents([agent]), [agent]);
});

test("normalizeWorkspaceAgents accepts content and agents wrappers", () => {
  assert.deepEqual(normalizeWorkspaceAgents({ content: [agent] }), [agent]);
  assert.deepEqual(normalizeWorkspaceAgents({ agents: [agent] }), [agent]);
});

test("normalizeWorkspaceAgents rejects non-list payloads", () => {
  assert.deepEqual(normalizeWorkspaceAgents(null), []);
  assert.deepEqual(normalizeWorkspaceAgents({ data: agent }), []);
  assert.deepEqual(normalizeWorkspaceAgents({ content: "not-list" }), []);
});

test("validateAgentSkillFiles enforces fail-closed bounds and rejects path traversal", () => {
  const valid = validateAgentSkillFiles([
    { fileName: " AGENTS.md ", content: " # Role\n- Check code " },
    { fileName: "   ", content: "   " },
  ]);
  assert.deepEqual(valid, [{ fileName: "AGENTS.md", content: "# Role\n- Check code" }]);

  assert.throws(
    () => validateAgentSkillFiles([{ fileName: "../secret.md", content: "bad" }]),
    RangeError,
  );
  assert.throws(
    () => validateAgentSkillFiles([{ fileName: "/etc/passwd", content: "bad" }]),
    RangeError,
  );
  assert.throws(
    () =>
      validateAgentSkillFiles([
        { fileName: "big.md", content: "x".repeat(MAX_AGENT_SKILL_CONTENT_CHARS + 1) },
      ]),
    RangeError,
  );
  assert.throws(
    () =>
      validateAgentSkillFiles(
        Array.from({ length: MAX_AGENT_SKILL_FILES + 1 }, (_, i) => ({
          fileName: `skill-${i}.md`,
          content: "ok",
        })),
      ),
    RangeError,
  );
});

test("agents client exposes updateWorkspaceAgent PATCH contract", () => {
  const source = readFileSync(new URL("./agents.ts", import.meta.url), "utf8");
  assert.match(source, /export async function updateWorkspaceAgent/);
  assert.match(source, /method:\s*"PATCH"/);
  assert.match(source, /validateAgentSkillFiles/);
});
