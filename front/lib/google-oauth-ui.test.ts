import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();

test("root layout wraps app with google oauth provider", () => {
  assert.match(readSource("app/layout.tsx"), /Providers/);
  assert.match(readSource("app/providers.tsx"), /GoogleOAuthProvider/);
  assert.match(readSource("app/providers.tsx"), /NEXT_PUBLIC_GOOGLE_CLIENT_ID/);
});

test("auth page uses google login button and handles success failure and return target", () => {
  const source = readSource("app/auth/page.tsx");

  assert.match(source, /GoogleLogin/);
  assert.match(source, /loginWithGoogle/);
  assert.match(source, /saveAuthTokens/);
  assert.match(source, /credential/);
  assert.match(source, /safeReturnTo/);
  assert.match(source, /nextStep/);
  assert.match(source, /router\.replace\(nextStep\(returnTo\)\)/);
  assert.doesNotMatch(source, /window\.location\.replace\(returnTo\)/);
  assert.doesNotMatch(source, /Google id token/);
});

test("title screen keeps unauthenticated continue on title screen", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /isAuthenticated/);
  assert.match(source, /router\.push\(item\.href\)/);
  assert.doesNotMatch(source, /router\.push\("\/auth\?returnTo=\/workspaces"\)/);
  assert.doesNotMatch(source, /인증 화면으로 이동합니다/);
});

test("auth helper exposes authenticated session predicate", () => {
  const source = readSource("lib/auth.ts");

  assert.match(source, /isAuthenticated/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
