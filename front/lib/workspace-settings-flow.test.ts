import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = path.resolve(import.meta.dirname, "..");
const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");
const cssSource = readSource("app/globals.css");
const inviteApiSource = readSource("lib/api/invites.ts");
const inviteAliasSource = readSource("lib/invites.ts");

test("workspace settings exposes the four primary management sections and dailyTokenLimit", () => {
  assert.match(
    settingsSource,
    /type SettingsSection = "workspace" \| "members" \| "invite" \| "invites";/,
  );
  assert.match(settingsSource, /AppTopChrome/);
  assert.match(settingsSource, /activeSection/);
  assert.match(settingsSource, /activeSection === "workspace"/);
  assert.match(settingsSource, /activeSection === "members"/);
  assert.match(settingsSource, /activeSection === "invite"/);
  assert.match(settingsSource, /activeSection === "invites"/);
  assert.match(settingsSource, /dailyTokenLimit/);
  assert.match(settingsSource, /일일 토큰 예산 한도/);
});

test("workspace settings redirects without stored auth and keeps connections out of settings", () => {
  assert.match(settingsSource, /getStoredUser/);
  assert.match(settingsSource, /router\.replace\("\/"\)/);
  assert.doesNotMatch(settingsSource, /Slack 설정으로/);
  assert.doesNotMatch(settingsSource, /GitHub 설정으로/);
  assert.doesNotMatch(settingsSource, /Gateway 설정으로/);
});

test("workspace settings keeps destructive workspace actions inside a confirmation modal", () => {
  assert.match(settingsSource, /deleteOpen/);
  assert.match(settingsSource, /<Modal/);
  assert.match(settingsSource, /open=\{deleteOpen\}/);
  assert.match(settingsSource, /title="워크스페이스 영구 삭제"/);
  assert.match(settingsSource, /disableEscapeClose=\{deleteBusy\}/);
});

test("workspace settings supports invite creation and invite list actions", () => {
  assert.match(settingsSource, /type InviteCreateMode = "email" \| "link";/);
  assert.match(settingsSource, /<option value="ALL">전체<\/option>/);
  assert.match(settingsSource, /<option value="PENDING">대기 중<\/option>/);
  assert.match(settingsSource, /inviteCreateMode/);
  assert.match(settingsSource, /inviteRole/);
  assert.match(settingsSource, /inviteExpiresInDays/);
  assert.match(settingsSource, /inviteCreateMode === "email"/);
  assert.match(settingsSource, /createInviteLink/);
  assert.match(settingsSource, /\.\.\.\(inviteCreateMode === "email" \? \{ targetEmail \} : \{\}\)/);
  assert.doesNotMatch(settingsSource, /email: trimmedInviteEmail/);
  assert.match(settingsSource, /copyToClipboard/);
  assert.match(settingsSource, /createdInviteUrl/);
  assert.match(settingsSource, /createdInviteCopied/);
  assert.match(settingsSource, /extendInvite/);
  assert.match(settingsSource, /deleteInvite/);
  assert.match(settingsSource, /handleCopyInvite/);
  assert.match(settingsSource, /"복사됨" : "복사"/);
  assert.match(settingsSource, /7일 연장/);
  assert.match(settingsSource, /삭제/);
  assert.match(settingsSource, /invite\.status === "ACCEPTED"/);
  assert.match(settingsSource, /router\.replace\("\/workspaces"\)/);
});

test("invite client exposes management endpoints used by settings", () => {
  assert.match(inviteApiSource, /targetEmail\?: string/);
  assert.doesNotMatch(inviteApiSource, /email\?: string/);
  assert.match(inviteApiSource, /inviteUrl\?: string/);
  assert.match(inviteApiSource, /type ExtendWorkspaceInviteInput/);
  assert.match(inviteApiSource, /additionalDays/);
  assert.match(inviteApiSource, /extendWorkspaceInvite/);
  assert.match(inviteApiSource, /extendInvite/);
  assert.match(inviteApiSource, /\/invites\/\$\{inviteId\}\/extend/);
  assert.match(inviteApiSource, /deleteWorkspaceInvite/);
  assert.match(inviteApiSource, /deleteInvite/);
  assert.match(inviteAliasSource, /extendWorkspaceInvite/);
  assert.match(inviteAliasSource, /deleteWorkspaceInvite/);
});

test("workspace settings section controls and modal have stable styles", () => {
  assert.match(cssSource, /\.grid/);
  assert.match(cssSource, /\.flex/);
  assert.match(cssSource, /\.truncate/);
  assert.match(cssSource, /\[class~="lg:grid-cols-\[280px_minmax\(0,1fr\)\]"\]/);
  assert.match(cssSource, /\.ao-modal/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
