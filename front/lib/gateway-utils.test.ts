import test from "node:test";
import assert from "node:assert/strict";
import {
  gatewayConnectionStatusLabel,
  gatewayStatusLabel,
  isGatewayUrl,
  maskGatewayToken,
  normalizeGatewayRegistryForm,
  normalizeGatewayForm,
  OPENCLAW_GATEWAYS_STORAGE_KEY,
  workspaceGatewayBindingPath,
  workspaceGatewayConnectionTestPath,
  workspaceGatewayStatusPath,
} from "./gateway-utils.ts";

test("normalizeGatewayForm trims values and validates http gateway URL", () => {
  const form = normalizeGatewayForm({
    gatewayUrl: " https://gateway.example.com ",
    token: " gate_1234567890abcdef ",
    validateConnection: undefined,
  });

  assert.deepEqual(form, {
    gatewayUrl: "https://gateway.example.com",
    token: "gate_1234567890abcdef",
    validateConnection: true,
    urlError: null,
    tokenError: null,
    isValid: true,
  });
});

test("normalizeGatewayForm returns field errors for invalid input", () => {
  const form = normalizeGatewayForm({
    gatewayUrl: "ftp://gateway.example.com",
    token: " ",
    validateConnection: false,
  });

  assert.equal(form.isValid, false);
  assert.equal(form.urlError, "gateway URL은 http:// 또는 https:// 로 시작해야 합니다.");
  assert.equal(form.tokenError, "gateway token을 입력해 주세요.");
});

test("isGatewayUrl accepts only http and https URLs", () => {
  assert.equal(isGatewayUrl("https://gateway.example.com"), true);
  assert.equal(isGatewayUrl("http://localhost:4317"), true);
  assert.equal(isGatewayUrl("ftp://gateway.example.com"), false);
  assert.equal(isGatewayUrl("gateway.example.com"), false);
});

test("maskGatewayToken hides short tokens and preserves token edges", () => {
  assert.equal(maskGatewayToken("short"), "****");
  assert.equal(maskGatewayToken("gate_1234567890abcdef"), "gate...cdef");
});

test("normalizeGatewayRegistryForm validates display name url and token", () => {
  const form = normalizeGatewayRegistryForm({
    displayName: " Local Gateway ",
    gatewayUrl: " http://localhost:4317 ",
    token: " gate-secret ",
  });

  assert.deepEqual(form, {
    displayName: "Local Gateway",
    gatewayUrl: "http://localhost:4317",
    token: "gate-secret",
    displayNameError: null,
    urlError: null,
    tokenError: null,
    isValid: true,
  });
});

test("normalizeGatewayRegistryForm returns field errors for registry inputs", () => {
  const form = normalizeGatewayRegistryForm({
    displayName: " ",
    gatewayUrl: "ftp://localhost",
    token: " ",
  });

  assert.equal(OPENCLAW_GATEWAYS_STORAGE_KEY, "aio.openclaw.gateways");
  assert.equal(form.isValid, false);
  assert.equal(form.displayNameError, "표시 이름은 필수입니다.");
  assert.equal(form.urlError, "게이트웨이 URL은 http:// 또는 https:// 로 시작해야 합니다.");
  assert.equal(form.tokenError, "토큰은 필수입니다.");
});

test("gateway labels match settings screen copy", () => {
  assert.equal(gatewayStatusLabel("UNBOUND"), "미연결");
  assert.equal(gatewayStatusLabel("BOUND"), "연결됨");
  assert.equal(gatewayConnectionStatusLabel("CONNECTED"), "연결 가능");
  assert.equal(gatewayConnectionStatusLabel("TOKEN_INVALID"), "토큰 오류");
  assert.equal(gatewayConnectionStatusLabel("TIMEOUT"), "시간 초과");
});

test("workspace gateway paths target a single workspace", () => {
  assert.equal(workspaceGatewayStatusPath(101), "/api/v1/workspaces/101/gateway");
  assert.equal(
    workspaceGatewayConnectionTestPath(101),
    "/api/v1/workspaces/101/gateway/connection-test",
  );
  assert.equal(
    workspaceGatewayBindingPath(101),
    "/api/v1/workspaces/101/gateway/binding",
  );
});
