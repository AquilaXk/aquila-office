import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("member profile client exposes profile API contracts", () => {
  const source = readFileSync(path.join(appRoot, "lib/members.ts"), "utf8");

  assert.match(source, /type MemberProfile/);
  assert.match(source, /type UpdateMemberProfileInput/);
  assert.match(source, /fetchMemberProfile/);
  assert.match(source, /updateMemberProfile/);
  assert.match(source, /\/api\/v1\/members\/me\/profile/);
  assert.match(source, /\baccessToken\b/);
});
