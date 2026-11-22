/*
 * 동일 주기 폴링을 단일 타이머로 병합(coalescing)하고, in-flight 중복 실행을 건너뛰며,
 * 가시성 변화에 따라 중단/재개하는 PollBucket, 타임아웃 데드라인, 지터 지수 백오프 유틸리티.
 */

import {
  MAX_POLL_INTERVAL_MS,
  MIN_POLL_INTERVAL_MS,
  hostDocumentHidden,
  onHostVisibilityChange,
} from "./visibility-poll.ts";

export const DEFAULT_FETCH_DEADLINE_MS = 10_000;
export const MIN_FETCH_DEADLINE_MS = 100;
export const MAX_FETCH_DEADLINE_MS = 120_000;
export const DEFAULT_BACKOFF_BASE_MS = 1000;
export const DEFAULT_BACKOFF_MAX_MS = 30_000;
export const BACKOFF_JITTER_RATIO = 0.2;

export type BackoffDelayOptions = {
  baseMs?: number;
  maxMs?: number;
  jitterRatio?: number;
  random?: () => number;
};

export type FetchDeadlineOptions = {
  timeoutMs?: number;
  signal?: AbortSignal | null;
};

export function computeBackoffDelayMs(
  attempt: number,
  options: BackoffDelayOptions = {},
): number {
  const baseMs =
    typeof options.baseMs === "number" && Number.isFinite(options.baseMs) && options.baseMs > 0
      ? options.baseMs
      : DEFAULT_BACKOFF_BASE_MS;
  const maxMs =
    typeof options.maxMs === "number" && Number.isFinite(options.maxMs) && options.maxMs >= baseMs
      ? options.maxMs
      : Math.max(baseMs, DEFAULT_BACKOFF_MAX_MS);
  const jitterRatio =
    typeof options.jitterRatio === "number" &&
    Number.isFinite(options.jitterRatio) &&
    options.jitterRatio >= 0 &&
    options.jitterRatio <= 1
      ? options.jitterRatio
      : BACKOFF_JITTER_RATIO;
  const randomFn = options.random ?? Math.random;

  const exp = Math.min(30, Math.max(0, Math.floor(Number.isFinite(attempt) ? attempt : 0)));
  const raw = Math.min(maxMs, baseMs * 2 ** exp);
  const sample = Math.min(1, Math.max(0, Number(randomFn())));
  const factor = 1 + (sample * 2 - 1) * jitterRatio;
  return Math.max(0, Math.round(raw * factor));
}

export function createDeadlineSignal(
  timeoutMs = DEFAULT_FETCH_DEADLINE_MS,
  upstreamSignal?: AbortSignal | null,
): { signal: AbortSignal; dispose: () => void } {
  if (
    typeof timeoutMs !== "number" ||
    !Number.isFinite(timeoutMs) ||
    timeoutMs < MIN_FETCH_DEADLINE_MS ||
    timeoutMs > MAX_FETCH_DEADLINE_MS
  ) {
    throw new RangeError(
      `요청 타임아웃(${timeoutMs}ms)은 ${MIN_FETCH_DEADLINE_MS}ms..${MAX_FETCH_DEADLINE_MS}ms 범위여야 합니다.`,
    );
  }

  let timer: ReturnType<typeof setTimeout> | null = null;
  let timeoutSignal: AbortSignal;
  if (typeof AbortSignal !== "undefined" && typeof AbortSignal.timeout === "function") {
    timeoutSignal = AbortSignal.timeout(timeoutMs);
  } else {
    const fallback = new AbortController();
    timer = setTimeout(() => fallback.abort(new DOMException("TimeoutError", "TimeoutError")), timeoutMs);
    timeoutSignal = fallback.signal;
  }

  if (!upstreamSignal) {
    return {
      signal: timeoutSignal,
      dispose: () => {
        if (timer !== null) {
          clearTimeout(timer);
          timer = null;
        }
      },
    };
  }

  if (typeof AbortSignal !== "undefined" && typeof AbortSignal.any === "function") {
    return {
      signal: AbortSignal.any([upstreamSignal, timeoutSignal]),
      dispose: () => {
        if (timer !== null) {
          clearTimeout(timer);
          timer = null;
        }
      },
    };
  }

  const combined = new AbortController();
  const abortFromUpstream = () => combined.abort(upstreamSignal.reason);
  const abortFromTimeout = () => combined.abort(timeoutSignal.reason);

  if (upstreamSignal.aborted) {
    abortFromUpstream();
  } else if (timeoutSignal.aborted) {
    abortFromTimeout();
  } else {
    upstreamSignal.addEventListener("abort", abortFromUpstream, { once: true });
    timeoutSignal.addEventListener("abort", abortFromTimeout, { once: true });
  }

  return {
    signal: combined.signal,
    dispose: () => {
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
      }
      upstreamSignal.removeEventListener("abort", abortFromUpstream);
      timeoutSignal.removeEventListener("abort", abortFromTimeout);
    },
  };
}

export async function withFetchDeadline<T>(
  task: (signal: AbortSignal) => Promise<T>,
  options: number | FetchDeadlineOptions = {},
): Promise<T> {
  const resolved =
    typeof options === "number" ? { timeoutMs: options, signal: undefined } : options;
  const timeoutMs = resolved.timeoutMs ?? DEFAULT_FETCH_DEADLINE_MS;
  const { signal, dispose } = createDeadlineSignal(timeoutMs, resolved.signal);
  try {
    return await task(signal);
  } finally {
    dispose();
  }
}

type BucketEntry = {
  key: string;
  task: (signal: AbortSignal) => Promise<void> | void;
  inFlight: boolean;
  abortController: AbortController | null;
};

type IntervalBucket = {
  intervalMs: number;
  entries: Map<string, BucketEntry>;
  timer: ReturnType<typeof setInterval> | null;
};

function scheduleInterval(fn: () => void, ms: number): ReturnType<typeof setInterval> {
  if (typeof window !== "undefined" && typeof window.setInterval === "function") {
    return window.setInterval(fn, ms) as unknown as ReturnType<typeof setInterval>;
  }
  return setInterval(fn, ms);
}

function cancelInterval(handle: ReturnType<typeof setInterval>): void {
  if (typeof window !== "undefined" && typeof window.clearInterval === "function") {
    window.clearInterval(handle as unknown as number);
    return;
  }
  clearInterval(handle);
}

export class PollBucket {
  private readonly buckets = new Map<number, IntervalBucket>();
  private readonly entryKeyToInterval = new Map<string, number>();
  private unsubscribeVisibility: (() => void) | null = null;
  private disposed = false;

  subscribe(
    key: string,
    intervalMs: number,
    task: (signal: AbortSignal) => Promise<void> | void,
    options?: { immediate?: boolean },
  ): () => void {
    if (this.disposed) {
      throw new Error("Disposed PollBucket cannot accept subscriptions");
    }
    if (!key || typeof key !== "string") {
      throw new RangeError("PollBucket key must be a non-empty string");
    }
    if (
      !Number.isFinite(intervalMs) ||
      intervalMs < MIN_POLL_INTERVAL_MS ||
      intervalMs > MAX_POLL_INTERVAL_MS
    ) {
      throw new RangeError(
        `폴링 주기(${intervalMs}ms)는 ${MIN_POLL_INTERVAL_MS}ms..${MAX_POLL_INTERVAL_MS}ms 범위여야 합니다.`,
      );
    }

    const existingInterval = this.entryKeyToInterval.get(key);
    if (existingInterval !== undefined) {
      this.unsubscribeKey(key, existingInterval);
    }

    let bucket = this.buckets.get(intervalMs);
    if (!bucket) {
      bucket = {
        intervalMs,
        entries: new Map(),
        timer: null,
      };
      this.buckets.set(intervalMs, bucket);
    }

    const entry: BucketEntry = {
      key,
      task,
      inFlight: false,
      abortController: null,
    };
    bucket.entries.set(key, entry);
    this.entryKeyToInterval.set(key, intervalMs);
    this.ensureVisibilityListener();

    if (!hostDocumentHidden()) {
      this.armBucket(bucket);
      if (options?.immediate) {
        this.runEntry(entry);
      }
    }

    return () => {
      this.unsubscribeKey(key, intervalMs);
    };
  }

  triggerBucket(intervalMs: number): void {
    const bucket = this.buckets.get(intervalMs);
    if (!bucket || hostDocumentHidden()) {
      return;
    }
    for (const entry of bucket.entries.values()) {
      this.runEntry(entry);
    }
  }

  getBucketCount(): number {
    return this.buckets.size;
  }

  getSubscriberCount(): number {
    return this.entryKeyToInterval.size;
  }

  isInFlight(key: string): boolean {
    const intervalMs = this.entryKeyToInterval.get(key);
    if (intervalMs === undefined) return false;
    return this.buckets.get(intervalMs)?.entries.get(key)?.inFlight ?? false;
  }

  dispose(): void {
    this.disposed = true;
    for (const bucket of this.buckets.values()) {
      this.disarmBucket(bucket);
      for (const entry of bucket.entries.values()) {
        entry.abortController?.abort();
        entry.abortController = null;
        entry.inFlight = false;
      }
      bucket.entries.clear();
    }
    this.buckets.clear();
    this.entryKeyToInterval.clear();
    this.unsubscribeVisibility?.();
    this.unsubscribeVisibility = null;
  }

  private ensureVisibilityListener(): void {
    if (this.unsubscribeVisibility) return;
    this.unsubscribeVisibility = onHostVisibilityChange(() => {
      if (hostDocumentHidden()) {
        for (const bucket of this.buckets.values()) {
          this.disarmBucket(bucket);
        }
        return;
      }
      for (const bucket of this.buckets.values()) {
        this.triggerBucket(bucket.intervalMs);
        this.armBucket(bucket);
      }
    });
  }

  private armBucket(bucket: IntervalBucket): void {
    if (bucket.timer !== null || this.disposed || bucket.entries.size === 0) {
      return;
    }
    bucket.timer = scheduleInterval(() => {
      this.triggerBucket(bucket.intervalMs);
    }, bucket.intervalMs);
  }

  private disarmBucket(bucket: IntervalBucket): void {
    if (bucket.timer === null) return;
    cancelInterval(bucket.timer);
    bucket.timer = null;
  }

  private runEntry(entry: BucketEntry): void {
    if (entry.inFlight || this.disposed) {
      return;
    }
    entry.inFlight = true;
    const controller = new AbortController();
    entry.abortController = controller;

    try {
      Promise.resolve(entry.task(controller.signal))
        .catch(() => {
          // 개별 폴링 오류로 공유 버킷 루프가 중단되지 않도록 격리한다.
        })
        .finally(() => {
          if (entry.abortController === controller) {
            entry.abortController = null;
          }
          entry.inFlight = false;
        });
    } catch {
      if (entry.abortController === controller) {
        entry.abortController = null;
      }
      entry.inFlight = false;
    }
  }

  private unsubscribeKey(key: string, intervalMs: number): void {
    const bucket = this.buckets.get(intervalMs);
    if (!bucket) return;
    const entry = bucket.entries.get(key);
    if (!entry) return;

    entry.abortController?.abort();
    entry.abortController = null;
    entry.inFlight = false;
    bucket.entries.delete(key);
    if (this.entryKeyToInterval.get(key) === intervalMs) {
      this.entryKeyToInterval.delete(key);
    }

    if (bucket.entries.size === 0) {
      this.disarmBucket(bucket);
      this.buckets.delete(intervalMs);
    }
    if (this.buckets.size === 0) {
      this.unsubscribeVisibility?.();
      this.unsubscribeVisibility = null;
    }
  }
}

export const sharedPollBucket = new PollBucket();
