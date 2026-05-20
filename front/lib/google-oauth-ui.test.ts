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
  assert.match(source, /credential/);
  assert.match(source, /safeReturnTo/);
  assert.match(source, /nextStep/);
  assert.match(source, /SIGN IN TO CONTINUE/);
  assert.match(source, /WORKSPACE MEMBER VERIFICATION/);
  assert.match(source, /PRESS TO CONTINUE/);
  assert.match(source, /\[ ESC BACK \] © aquilaXk 2026/);
  assert.match(source, /containerProps/);
  assert.match(source, /background:\s*"transparent"/);
  assert.match(source, /border:\s*"1px solid rgba\(194, 163, 91, 0\.55\)"/);
  assert.match(source, /Google 토큰을 받지 못했습니다\./);
  assert.match(source, /LOADING SAVE FILE\.\.\./);
  assert.match(source, /로그인에 실패했습니다\./);
  assert.match(source, /router\.replace\(nextStep\(returnTo\)\)/);
  assert.doesNotMatch(source, /saveAuthTokens/);
  assert.doesNotMatch(source, /window\.location\.replace\(returnTo\)/);
  assert.doesNotMatch(source, /Google id token/);
  assert.doesNotMatch(source, /인증 세션을 저장하고 있습니다/);
  assert.doesNotMatch(source, /Google 인증 토큰을 받지 못했습니다/);
  assert.doesNotMatch(source, /TEAM IoT 2026/);
});

test("title screen keeps unauthenticated continue on title screen", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /isAuthenticated/);
  assert.match(source, /readAuthUser/);
  assert.match(source, /LOGIN/);
  assert.match(source, /CONTINUE/);
  assert.doesNotMatch(source, /SETTINGS/);
  assert.doesNotMatch(source, /\/auth\/character/);
  assert.match(source, /useState<AuthUser \| null>\(null\)/);
  assert.match(source, /HERO \{user\?\.name \?\? "PLAYER"\}/);
  assert.match(source, /router\.push\(item\.href\)/);
  assert.match(source, /router\.push\("\/"\)/);
  assert.doesNotMatch(source, /router\.push\("\/auth\?returnTo=\/workspaces"\)/);
  assert.doesNotMatch(source, /인증 화면으로 이동합니다/);
  assert.doesNotMatch(source, /로그인이 필요합니다/);
  assert.doesNotMatch(source, /title-screen-notice/);
});

test("auth helper exposes authenticated session predicate", () => {
  const source = readSource("lib/auth.ts");

  assert.match(source, /isAuthenticated/);
  assert.match(source, /AUTH_ACCESS_TOKEN_KEY/);
  assert.match(source, /AUTH_REFRESH_TOKEN_KEY/);
  assert.match(source, /AUTH_USER_KEY/);
  assert.match(source, /readAuthUser/);
  assert.doesNotMatch(source, /AUTH_STORAGE_KEY = "aquila-office\.auth"/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
