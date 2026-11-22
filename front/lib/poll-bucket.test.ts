import assert from "node:assert/strict";
import test from "node:test";
import {
  BACKOFF_JITTER_RATIO,
  PollBucket,
  computeBackoffDelayMs,
  withFetchDeadline,
} from "./poll-bucket.ts";

function installFakeBrowserEnv(initialHidden: boolean) {
  let hidden = initialHidden;
  let nextTimerId = 1;
  const intervals = new Map<number, () => void>();
  const docListeners = new Set<() => void>();
  const winListeners = new Set<(ev: Event) => void>();

  const prevWindow = (globalThis as { window?: unknown }).window;
  const prevDocument = (globalThis as { document?: unknown }).document;

  const fakeDoc = {
    get visibilityState() {
      return hidden ? "hidden" : "visible";
    },
    addEventListener(type: string, fn: () => void) {
      if (type === "visibilitychange") docListeners.add(fn);
    },
    removeEventListener(type: string, fn: () => void) {
      if (type === "visibilitychange") docListeners.delete(fn);
    },
  };

  const fakeWin = {
    setInterval(fn: () => void) {
      const id = nextTimerId++;
      intervals.set(id, fn);
      return id;
    },
    clearInterval(id: number) {
      intervals.delete(id);
    },
    addEventListener(type: string, fn: (ev: Event) => void) {
      if (type === "aquila:host-visibility") winListeners.add(fn);
    },
    removeEventListener(type: string, fn: (ev: Event) => void) {
      if (type === "aquila:host-visibility") winListeners.delete(fn);
    },
  };

  Object.assign(globalThis, { document: fakeDoc, window: fakeWin });

  return {
    intervals,
    setDocumentHidden(next: boolean) {
      hidden = next;
      for (const fn of docListeners) fn();
    },
    tickAll() {
      for (const fn of [...intervals.values()]) fn();
    },
    restore() {
      if (prevWindow === undefined) delete (globalThis as { window?: unknown }).window;
      else Object.assign(globalThis, { window: prevWindow });
      if (prevDocument === undefined) delete (globalThis as { document?: unknown }).document;
      else Object.assign(globalThis, { document: prevDocument });
    },
  };
}

test("PollBucket coalesces identical intervals into one timer and disposes cleanly", () => {
  const env = installFakeBrowserEnv(false);
  const bucket = new PollBucket();
  try {
    let callsA = 0;
    let callsB = 0;
    const unsubA = bucket.subscribe("tasks", 15_000, () => {
      callsA += 1;
    });
    const unsubB = bucket.subscribe("summary", 15_000, () => {
      callsB += 1;
    });

    assert.equal(bucket.getBucketCount(), 1);
    assert.equal(bucket.getSubscriberCount(), 2);
    assert.equal(env.intervals.size, 1);

    env.tickAll();
    assert.equal(callsA, 1);
    assert.equal(callsB, 1);

    unsubA();
    assert.equal(bucket.getBucketCount(), 1);
    assert.equal(env.intervals.size, 1);

    unsubB();
    assert.equal(bucket.getBucketCount(), 0);
    assert.equal(env.intervals.size, 0);
  } finally {
    bucket.dispose();
    env.restore();
  }
});

test("PollBucket skips ticks while a subscriber request is in-flight and resumes on visibility restore", async () => {
  const env = installFakeBrowserEnv(false);
  const bucket = new PollBucket();
  try {
    let calls = 0;
    let release!: () => void;

    const unsub = bucket.subscribe("tasks", 15_000, async () => {
      calls += 1;
      await new Promise<void>((resolve) => {
        release = resolve;
      });
    });

    env.tickAll();
    assert.equal(calls, 1);
    assert.equal(bucket.isInFlight("tasks"), true);

    // Next tick while in-flight must be skipped
    env.tickAll();
    assert.equal(calls, 1);

    release();
    await new Promise((resolve) => setTimeout(resolve, 0));
    assert.equal(bucket.isInFlight("tasks"), false);

    // Hide document -> timer disarmed; show document -> immediate make-up tick
    env.setDocumentHidden(true);
    assert.equal(env.intervals.size, 0);
    env.setDocumentHidden(false);
    assert.equal(env.intervals.size, 1);
    assert.equal(calls, 2);

    release();
    await new Promise((resolve) => setTimeout(resolve, 0));
    unsub();
  } finally {
    bucket.dispose();
    env.restore();
  }
});

test("computeBackoffDelayMs applies exponential growth capped by maxMs with +-20% jitter", () => {
  assert.equal(BACKOFF_JITTER_RATIO, 0.2);

  const minDelay = computeBackoffDelayMs(0, { baseMs: 1000, maxMs: 30_000, random: () => 0 });
  const midDelay = computeBackoffDelayMs(0, { baseMs: 1000, maxMs: 30_000, random: () => 0.5 });
  const maxDelay = computeBackoffDelayMs(0, { baseMs: 1000, maxMs: 30_000, random: () => 1 });
  assert.equal(minDelay, 800);
  assert.equal(midDelay, 1000);
  assert.equal(maxDelay, 1200);

  const capped = computeBackoffDelayMs(10, { baseMs: 1000, maxMs: 16_000, random: () => 0.5 });
  assert.equal(capped, 16_000);
});

test("withFetchDeadline aborts when upstream signal or timeout deadline fires and rejects invalid timeouts", async () => {
  await assert.rejects(
    () => withFetchDeadline(async () => "ok", { timeoutMs: 10 }),
    RangeError,
  );

  const upstream = new AbortController();
  let observedSignal: AbortSignal | undefined;
  const pending = withFetchDeadline(
    (signal) =>
      new Promise<string>((_resolve, reject) => {
        observedSignal = signal;
        signal.addEventListener("abort", () => {
          reject(new Error("aborted"));
        });
      }),
    { timeoutMs: 5_000, signal: upstream.signal },
  );

  upstream.abort();
  await assert.rejects(() => pending, /aborted/);
  assert.equal(observedSignal?.aborted, true);
});
