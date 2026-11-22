import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");

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
  assert.match(source, /Google 계정으로 로그인/);
  assert.match(source, /워크스페이스 계정 인증/);
  assert.match(source, /Google 계정으로 계속하기/);
  assert.doesNotMatch(source, /PRESS TO CONTINUE/);
  assert.match(source, /className="auth-google-frame"/);
  assert.match(readSource("app/globals.css"), /\.auth-google-visible-label/);
  assert.match(source, /containerProps/);
  assert.match(source, /width:\s*284/);
  assert.match(source, /height:\s*48/);
  assert.match(source, /overflow:\s*"hidden"/);
  assert.match(source, /background:\s*"#202124"/);
  assert.match(source, /border:\s*"1px solid rgba\(230, 234, 240, 0\.14\)"/);
  assert.match(source, /Google 인증 토큰을 받지 못했습니다\./);
  assert.match(source, /계정 정보를 확인하고 있습니다\./);
  assert.match(source, /로그인에 실패했습니다\./);
  assert.match(source, /router\.replace\(nextStep\(returnTo\)\)/);
  assert.doesNotMatch(source, /saveAuthTokens/);
  assert.doesNotMatch(source, /window\.location\.replace\(returnTo\)/);
  assert.doesNotMatch(source, /TEAM IoT 2026/);
  assert.doesNotMatch(source, /ESC BACK/);
});

test("title screen keeps unauthenticated continue on title screen", () => {
  const source = readSource("app/page.tsx");

  assert.match(source, /isAuthenticated/);
  assert.match(source, /readAuthUser/);
  assert.match(source, /로그인/);
  assert.match(source, /워크스페이스로 이동/);
  assert.doesNotMatch(source, /SETTINGS/);
  assert.doesNotMatch(source, /\/auth\/character/);
  assert.match(source, /useState<AuthUser \| null>\(null\)/);
  assert.match(source, /게스트/);
  assert.match(source, /router\.push\(item\.href\)/);
  assert.match(source, /router\.push\("\/"\)/);
  assert.doesNotMatch(source, /router\.push\("\/auth\?returnTo=\/workspaces"\)/);
});

test("auth helper exposes authenticated session predicate", () => {
  const source = readSource("lib/auth.ts");

  assert.match(source, /isAuthenticated/);
  assert.match(source, /AUTH_ACCESS_TOKEN_KEY/);
  assert.match(source, /AUTH_USER_KEY/);
  assert.match(source, /readAuthUser/);
  assert.doesNotMatch(source, /AUTH_REFRESH_TOKEN_KEY/);
  assert.doesNotMatch(source, /AUTH_STORAGE_KEY = "aquila-office\.auth"/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
