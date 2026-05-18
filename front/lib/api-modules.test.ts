import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

const apiModules = [
  "auth",
  "workspaces",
  "invites",
  "agents",
  "tasks",
  "artifacts",
  "chat",
  "gateway",
] as const;

test("domain api modules are available under lib/api", () => {
  for (const moduleName of apiModules) {
    assert.equal(existsSync(path.join(appRoot, "lib/api", `${moduleName}.ts`)), true, moduleName);
  }
});

test("invite and artifact clients use compatible endpoint aliases", () => {
  assert.match(readSource("lib/api/invites.ts"), /\/api\/v1\/invites\/\$\{encodeURIComponent\(token\)\}/);
  assert.match(readSource("lib/api/artifacts.ts"), /steps\/\$\{stepId\}/);
});

test("legacy domain clients delegate to api domain modules", () => {
  assert.match(readSource("lib/auth.ts"), /@\/lib\/api\/auth/);
  assert.match(readSource("lib/invites.ts"), /@\/lib\/api\/invites/);
  assert.match(readSource("lib/artifacts.ts"), /@\/lib\/api\/artifacts/);
  assert.match(readSource("lib/artifacts.ts"), /ArtifactNode/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
