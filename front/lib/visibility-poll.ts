/*
 * 가시성 인지 타이머 스케줄러.
 * 브라우저 탭 숨김 및 데스크톱 트레이 최소화 상태에서 setInterval을 해제하고,
 * 화면 복귀 시 즉시 보정 틱 1회 후 주기를 재개한다.
 */

export const MIN_POLL_INTERVAL_MS = 16;
export const MAX_POLL_INTERVAL_MS = 86_400_000;

export type VisibilityPollOptions = {
  /** 기본 true: 창이 숨겨지면 타이머와 콜백을 중단한다. */
  pauseWhenHidden?: boolean;
  /** 시작 직후 1회 즉시 실행 여부(기본 false). */
  immediate?: boolean;
};

declare global {
  interface Window {
    __AQUILA_HOST_VISIBLE__?: boolean;
  }
}

export function hostDocumentHidden(): boolean {
  if (typeof document !== "undefined" && document.visibilityState === "hidden") {
    return true;
  }
  return typeof window !== "undefined" && window.__AQUILA_HOST_VISIBLE__ === false;
}

export function onHostVisibilityChange(callback: () => void): () => void {
  let last = hostDocumentHidden();

  const notify = () => {
    const next = hostDocumentHidden();
    if (next === last) return;
    last = next;
    callback();
  };

  const onHostEvent = (event: Event) => {
    const detail = (event as CustomEvent<unknown>).detail;
    if (typeof window !== "undefined" && typeof detail === "boolean") {
      window.__AQUILA_HOST_VISIBLE__ = detail;
    }
    notify();
  };

  if (typeof document !== "undefined") {
    document.addEventListener("visibilitychange", notify);
  }
  if (typeof window !== "undefined") {
    window.addEventListener("aquila:host-visibility", onHostEvent);
  }

  return () => {
    if (typeof document !== "undefined") {
      document.removeEventListener("visibilitychange", notify);
    }
    if (typeof window !== "undefined") {
      window.removeEventListener("aquila:host-visibility", onHostEvent);
    }
  };
}

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

export function startVisibilityPoll(
  callback: () => void,
  intervalMs: number,
  options?: VisibilityPollOptions,
): () => void {
  if (
    !Number.isFinite(intervalMs) ||
    intervalMs < MIN_POLL_INTERVAL_MS ||
    intervalMs > MAX_POLL_INTERVAL_MS
  ) {
    throw new RangeError(
      `폴링 주기(${intervalMs}ms)는 ${MIN_POLL_INTERVAL_MS}ms..${MAX_POLL_INTERVAL_MS}ms 범위여야 합니다.`,
    );
  }

  const pauseWhenHidden = options?.pauseWhenHidden !== false;
  let timer: ReturnType<typeof setInterval> | null = null;
  let stopped = false;

  const tick = () => {
    try {
      callback();
    } catch {
      // 개별 틱 오류가 폴링 루프를 영구 중단시키지 않도록 방어한다.
    }
  };

  const arm = () => {
    if (timer !== null || stopped) return;
    timer = scheduleInterval(tick, intervalMs);
  };

  const disarm = () => {
    if (timer === null) return;
    cancelInterval(timer);
    timer = null;
  };

  const onVisibility = () => {
    if (!pauseWhenHidden) return;
    if (hostDocumentHidden()) {
      disarm();
      return;
    }
    tick();
    arm();
  };

  if (!pauseWhenHidden || !hostDocumentHidden()) {
    arm();
  }

  const unsubscribeVisibility = pauseWhenHidden ? onHostVisibilityChange(onVisibility) : null;
  if (options?.immediate && (!pauseWhenHidden || !hostDocumentHidden())) {
    tick();
  }

  return () => {
    stopped = true;
    disarm();
    unsubscribeVisibility?.();
  };
}
