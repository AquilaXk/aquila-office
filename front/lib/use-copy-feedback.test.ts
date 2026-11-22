import assert from "node:assert/strict";
import test from "node:test";
import {
  createCopyController,
  MAX_COPY_CHARS,
} from "./use-copy-feedback.ts";

test("createCopyController prevents out-of-order async races and clears timer safely", async () => {
  const feedbackLog: Array<{ scope: string; outcome: "copied" | "failed" } | null> = [];
  const pendingWrites: Array<(ok: boolean) => void> = [];

  const controller = createCopyController<string>({
    durationMs: 25,
    onFeedbackChange: (next) => feedbackLog.push(next),
    writeText: () =>
      new Promise<boolean>((resolve) => {
        pendingWrites.push(resolve);
      }),
  });

  const first = controller.copy("first-log", "logs");
  const second = controller.copy("second-diff", "diff");

  // Resolve second first, then resolve stale first; stale first must be ignored.
  pendingWrites[1](true);
  await second;
  assert.deepEqual(feedbackLog.at(-1), { scope: "diff", outcome: "copied" });

  pendingWrites[0](false);
  await first;
  assert.deepEqual(feedbackLog.at(-1), { scope: "diff", outcome: "copied" });

  await new Promise((resolve) => setTimeout(resolve, 45));
  assert.equal(feedbackLog.at(-1), null);
  controller.dispose();
});

test("createCopyController fails closed when payload exceeds MAX_COPY_CHARS", async () => {
  const feedbackLog: Array<{ scope: string; outcome: "copied" | "failed" } | null> = [];
  let called = false;
  const controller = createCopyController<string>({
    onFeedbackChange: (next) => feedbackLog.push(next),
    writeText: async () => {
      called = true;
      return true;
    },
  });

  const oversized = "a".repeat(MAX_COPY_CHARS + 1);
  const ok = await controller.copy(oversized, "huge");
  assert.equal(ok, false);
  assert.equal(called, false);
  assert.deepEqual(feedbackLog.at(-1), { scope: "huge", outcome: "failed" });
  controller.dispose();
});
