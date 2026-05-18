import assert from "node:assert/strict";
import test from "node:test";
import {
  CRITICAL_PRODUCT_SCENARIOS,
  productContractReady,
} from "./product-contract.ts";

test("critical product contract covers core user journey and recovery states", () => {
  assert.deepEqual(
    CRITICAL_PRODUCT_SCENARIOS.map((scenario) => scenario.key),
    [
      "auth.google-login",
      "workspace.create",
      "invite.accept",
      "agent.create-sync",
      "chat.send",
      "artifact.step-read",
      "gateway.failure",
      "auth.expired",
      "ui.empty-state",
    ],
  );
  assert.equal(productContractReady(CRITICAL_PRODUCT_SCENARIOS), true);
});
