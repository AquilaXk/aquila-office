"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export const MAX_COPY_CHARS = 2_000_000;
const DEFAULT_FEEDBACK_MS = 2200;

export type CopyOutcome = "copied" | "failed";

export type CopyFeedbackState<Scope> = {
  scope: Scope;
  outcome: CopyOutcome;
};

export type CopyControllerOptions<Scope> = {
  durationMs?: number;
  writeText?: (text: string) => Promise<boolean>;
  onFeedbackChange: (next: CopyFeedbackState<Scope> | null) => void;
};

async function defaultWriteClipboardText(text: string): Promise<boolean> {
  if (typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
    return false;
  }
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

/**
 * 세대 카운터로 연속 복사 요청의 순서 역전을 차단하는 순수 컨트롤러.
 */
export function createCopyController<Scope = string>(options: CopyControllerOptions<Scope>) {
  const durationMs = options.durationMs ?? DEFAULT_FEEDBACK_MS;
  const writeText = options.writeText ?? defaultWriteClipboardText;
  let timer: ReturnType<typeof setTimeout> | null = null;
  let generation = 0;
  let disposed = false;

  const clearTimer = () => {
    if (timer !== null) {
      clearTimeout(timer);
      timer = null;
    }
  };

  const scheduleClear = (expectedGeneration: number) => {
    clearTimer();
    timer = setTimeout(() => {
      timer = null;
      if (!disposed && generation === expectedGeneration) {
        options.onFeedbackChange(null);
      }
    }, durationMs);
  };

  return {
    async copy(text: string, scope: Scope): Promise<boolean> {
      const currentGen = ++generation;
      if (typeof text !== "string" || text.length === 0 || text.length > MAX_COPY_CHARS) {
        if (!disposed) {
          options.onFeedbackChange({ scope, outcome: "failed" });
          scheduleClear(currentGen);
        }
        return false;
      }

      const ok = await writeText(text).catch(() => false);
      if (disposed || generation !== currentGen) {
        return false;
      }
      options.onFeedbackChange({ scope, outcome: ok ? "copied" : "failed" });
      scheduleClear(currentGen);
      return ok;
    },
    dispose() {
      disposed = true;
      generation += 1;
      clearTimer();
    },
  };
}

export function useCopyFeedback<Scope = string>(durationMs = DEFAULT_FEEDBACK_MS): {
  copiedKey: Scope | null;
  outcomeFor: (scope: Scope) => CopyOutcome | null;
  copy: (text: string, scope: Scope) => Promise<boolean>;
} {
  const [feedback, setFeedback] = useState<CopyFeedbackState<Scope> | null>(null);
  const controllerRef = useRef<ReturnType<typeof createCopyController<Scope>> | null>(null);

  useEffect(() => {
    return () => {
      controllerRef.current?.dispose();
      controllerRef.current = null;
    };
  }, [durationMs]);

  const outcomeFor = useCallback(
    (scope: Scope) => (feedback && Object.is(feedback.scope, scope) ? feedback.outcome : null),
    [feedback],
  );

  const copy = useCallback(
    (text: string, scope: Scope) => {
      if (!controllerRef.current) {
        controllerRef.current = createCopyController<Scope>({
          durationMs,
          onFeedbackChange: setFeedback,
        });
      }
      return controllerRef.current.copy(text, scope);
    },
    [durationMs],
  );

  return {
    copiedKey: feedback?.outcome === "copied" ? feedback.scope : null,
    outcomeFor,
    copy,
  };
}
