import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";

const appRoot = process.cwd();
const settingsSource = readSource("app/workspaces/[workspaceId]/settings/page.tsx");
const cssSource = readSource("app/globals.css");
const inviteApiSource = readSource("lib/api/invites.ts");
const inviteAliasSource = readSource("lib/invites.ts");

test("workspace settings exposes the four primary management sections", () => {
  assert.match(
    settingsSource,
    /type WorkspaceSettingsSection = "workspace" \| "members" \| "invite" \| "invites";/,
  );
  assert.match(settingsSource, /workspaceSettingsSections/);
  assert.match(settingsSource, /activeSettingsSection/);
  assert.match(settingsSource, /className="workspace-settings-tabs"/);
  assert.match(settingsSource, /aria-label="워크스페이스 설정 섹션"/);
  assert.match(settingsSource, /activeSettingsSection === "workspace"/);
  assert.match(settingsSource, /activeSettingsSection === "members"/);
  assert.match(settingsSource, /activeSettingsSection === "invite"/);
  assert.match(settingsSource, /activeSettingsSection === "invites"/);
});

test("workspace settings keeps destructive workspace actions inside a confirmation modal", () => {
  assert.match(settingsSource, /deleteWorkspaceModalOpen/);
  assert.match(settingsSource, /workspace-settings-delete-modal/);
  assert.match(settingsSource, /role="dialog"/);
  assert.match(settingsSource, /aria-label="워크스페이스 삭제 확인"/);
});

test("workspace settings supports invite creation and invite list actions", () => {
  assert.match(settingsSource, /type InviteCreateMode = "email" \| "link";/);
  assert.match(settingsSource, /inviteCreateMode/);
  assert.match(settingsSource, /inviteRole/);
  assert.match(settingsSource, /inviteExpiresInDays/);
  assert.match(settingsSource, /createWorkspaceInvite/);
  assert.match(settingsSource, /copyCreatedInviteUrl/);
  assert.match(settingsSource, /createdInvite/);
  assert.match(settingsSource, /extendWorkspaceInvite/);
  assert.match(settingsSource, /deleteWorkspaceInvite/);
  assert.match(settingsSource, /copyInviteManagementUrl/);
  assert.match(settingsSource, /workspace-invite-create-mode/);
  assert.match(settingsSource, /workspace-invite-created-card/);
  assert.match(settingsSource, /workspace-invite-copy/);
  assert.match(settingsSource, /workspace-invite-extend/);
  assert.match(settingsSource, /workspace-invite-delete/);
});

test("invite client exposes management endpoints used by settings", () => {
  assert.match(inviteApiSource, /type ExtendWorkspaceInviteInput/);
  assert.match(inviteApiSource, /additionalDays/);
  assert.match(inviteApiSource, /extendWorkspaceInvite/);
  assert.match(inviteApiSource, /\/invites\/\$\{inviteId\}\/extend/);
  assert.match(inviteApiSource, /deleteWorkspaceInvite/);
  assert.match(inviteAliasSource, /extendWorkspaceInvite/);
  assert.match(inviteAliasSource, /deleteWorkspaceInvite/);
});

test("workspace settings section controls and modal have stable styles", () => {
  assert.match(cssSource, /\.workspace-settings-tabs/);
  assert.match(cssSource, /\.workspace-settings-tab-active/);
  assert.match(cssSource, /\.workspace-settings-delete-modal/);
  assert.match(cssSource, /\.workspace-invite-create-mode/);
  assert.match(cssSource, /\.workspace-invite-created-card/);
  assert.match(cssSource, /\.workspace-invite-copy/);
});

function readSource(filePath: string): string {
  return readFileSync(path.join(appRoot, filePath), "utf8");
}
