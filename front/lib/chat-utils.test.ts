import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";

const appRoot = process.cwd();

test("workspace chat client exposes message API contracts", () => {
  const source = readFileSync(path.join(appRoot, "lib/chat.ts"), "utf8");

  assert.match(source, /type ChatMessage/);
  assert.match(source, /type SendChatMessageInput/);
  assert.match(source, /workspaceChatMessagesPath/);
  assert.match(source, /workspaceTaskChatMessagesPath/);
  assert.match(source, /workspaceChatSessionMessagesPath/);
  assert.match(source, /sendWorkspaceChatMessage/);
  assert.match(source, /fetchTaskChatMessages/);
  assert.match(source, /fetchChatSessionMessages/);
  assert.match(source, /\/api\/v1\/workspaces\/\$\{workspaceId\}\/chat\/messages/);
  assert.match(source, /\/chat\/tasks\/\$\{taskId\}\/messages/);
  assert.match(source, /\/chat\/sessions\/\$\{chatSessionId\}\/messages/);
});
