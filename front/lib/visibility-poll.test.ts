import assert from "node:assert/strict";
import test from "node:test";
import {
  hostDocumentHidden,
  onHostVisibilityChange,
  startVisibilityPoll,
} from "./visibility-poll.ts";

function installFakeBrowserEnv(initialHidden: boolean) {
  let hidden = initialHidden;
  let hostVisible: boolean | undefined = undefined;
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
    get __AQUILA_HOST_VISIBLE__() {
      return hostVisible;
    },
    set __AQUILA_HOST_VISIBLE__(v: boolean | undefined) {
      hostVisible = v;
    },
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
    emitHostVisible(next: boolean) {
      const ev = { detail: next } as unknown as Event;
      for (const fn of winListeners) fn(ev);
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

test("startVisibilityPoll rejects invalid or dangerously fast intervals fail-closed", () => {
  assert.throws(() => startVisibilityPoll(() => {}, 0), RangeError);
  assert.throws(() => startVisibilityPoll(() => {}, 10), RangeError);
  assert.throws(() => startVisibilityPoll(() => {}, Number.NaN), RangeError);
  assert.throws(() => startVisibilityPoll(() => {}, 100_000_000), RangeError);
});

test("startVisibilityPoll pauses timer when hidden and fires make-up tick on resume", () => {
  const env = installFakeBrowserEnv(false);
  try {
    let calls = 0;
    const stop = startVisibilityPoll(() => {
      calls += 1;
    }, 200);

    assert.equal(env.intervals.size, 1);
    env.tickAll();
    assert.equal(calls, 1);

    env.setDocumentHidden(true);
    assert.equal(hostDocumentHidden(), true);
    assert.equal(env.intervals.size, 0);

    // Restoring visibility performs 1 immediate make-up tick and re-arms timer.
    env.setDocumentHidden(false);
    assert.equal(hostDocumentHidden(), false);
    assert.equal(calls, 2);
    assert.equal(env.intervals.size, 1);

    stop();
    assert.equal(env.intervals.size, 0);
  } finally {
    env.restore();
  }
});

test("onHostVisibilityChange deduplicates repeated hide signals across document and desktop host", () => {
  const env = installFakeBrowserEnv(false);
  try {
    let transitions = 0;
    const off = onHostVisibilityChange(() => {
      transitions += 1;
    });

    env.setDocumentHidden(true);
    assert.equal(transitions, 1);

    // Duplicate hide signal from desktop host event does not fire twice.
    env.emitHostVisible(false);
    assert.equal(transitions, 1);

    // Showing document while desktop host flag was false still keeps hidden until host is true.
    env.setDocumentHidden(false);
    assert.equal(transitions, 1);

    env.emitHostVisible(true);
    assert.equal(transitions, 2);

    off();
  } finally {
    env.restore();
  }
});
