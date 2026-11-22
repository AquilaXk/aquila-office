"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import type { WorkspaceTask } from "@/lib/tasks";

const MAX_PROMPT_HISTORY = 25;
const STORAGE_PREFIX = "aquila-office.prompt-history.";
const inMemoryPromptHistory = new Map<string, string[]>();

function readPromptHistory(storageKey: string): string[] {
  const cached = inMemoryPromptHistory.get(storageKey);
  if (cached) {
    return cached;
  }
  if (typeof window === "undefined") {
    return [];
  }
  try {
    const raw = window.sessionStorage.getItem(`${STORAGE_PREFIX}${storageKey}`);
    if (!raw) {
      return [];
    }
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    const clean = parsed
      .filter((entry): entry is string => typeof entry === "string" && entry.trim().length > 0)
      .slice(-MAX_PROMPT_HISTORY);
    inMemoryPromptHistory.set(storageKey, clean);
    return clean;
  } catch {
    return [];
  }
}

function writePromptHistory(storageKey: string, items: string[]): void {
  const bounded = items.slice(-MAX_PROMPT_HISTORY);
  inMemoryPromptHistory.set(storageKey, bounded);
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.sessionStorage.setItem(`${STORAGE_PREFIX}${storageKey}`, JSON.stringify(bounded));
  } catch {
    // Ignore storage quota / sandbox restriction
  }
}

export function appendComposerToken(currentValue: string, token: string): string {
  const trimmed = currentValue.trimEnd();
  if (!trimmed) {
    return `${token} `;
  }
  return `${trimmed} ${token} `;
}

export function useComposerPromptHistory(
  historyKey: string,
  value: string,
  onChange: (nextValue: string) => void,
) {
  const [revision, setRevision] = useState(0);
  const cursorRef = useRef<number>(-1);
  const draftRef = useRef<string>("");

  const history = useMemo(() => {
    void revision;
    return readPromptHistory(historyKey);
  }, [historyKey, revision]);

  useEffect(() => {
    cursorRef.current = -1;
    draftRef.current = "";
  }, [historyKey]);

  const recordPrompt = useCallback(
    (rawPrompt: string) => {
      const normalized = rawPrompt.trim();
      if (!normalized) {
        return;
      }
      const prev = readPromptHistory(historyKey);
      const filtered = prev.filter((item) => item !== normalized);
      const next = [...filtered, normalized].slice(-MAX_PROMPT_HISTORY);
      writePromptHistory(historyKey, next);
      setRevision((count) => count + 1);
      cursorRef.current = -1;
      draftRef.current = "";
    },
    [historyKey],
  );

  const handleHistoryKeyDown = useCallback(
    (event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) {
        return;
      }
      if (event.key !== "ArrowUp" && event.key !== "ArrowDown") {
        return;
      }
      if (history.length === 0) {
        return;
      }

      const target = event.currentTarget;
      const isSingleLine = !value.includes("\n");
      const atStart = target.selectionStart === 0 && target.selectionEnd === 0;
      const atEnd =
        target.selectionStart === value.length && target.selectionEnd === value.length;

      if (event.key === "ArrowUp") {
        if (!isSingleLine && !atStart && value.length > 0) {
          return;
        }
        event.preventDefault();
        if (cursorRef.current === -1) {
          draftRef.current = value;
          cursorRef.current = history.length - 1;
        } else if (cursorRef.current > 0) {
          cursorRef.current -= 1;
        }
        const entry = history[cursorRef.current];
        if (entry !== undefined) {
          onChange(entry);
        }
        return;
      }

      if (event.key === "ArrowDown" && cursorRef.current !== -1) {
        if (!isSingleLine && !atEnd && value.length > 0) {
          return;
        }
        event.preventDefault();
        if (cursorRef.current < history.length - 1) {
          cursorRef.current += 1;
          const entry = history[cursorRef.current];
          if (entry !== undefined) {
            onChange(entry);
          }
        } else {
          cursorRef.current = -1;
          onChange(draftRef.current);
        }
      }
    },
    [history, onChange, value],
  );

  return {
    historyCount: history.length,
    recordPrompt,
    handleHistoryKeyDown,
  };
}

export function ComposerContextChips({
  tasks,
  onInsert,
}: {
  tasks: readonly WorkspaceTask[];
  onInsert: (token: string) => void;
}) {
  const quickTasks = useMemo(() => {
    const active: WorkspaceTask[] = [];
    const rest: WorkspaceTask[] = [];
    for (const task of tasks) {
      const status = (task.status ?? "").toUpperCase();
      if (status === "IN_PROGRESS" || status === "RUNNING" || status === "TODO" || status === "PENDING") {
        active.push(task);
      } else {
        rest.push(task);
      }
    }
    return [...active, ...rest].slice(0, 4);
  }, [tasks]);

  if (quickTasks.length === 0) {
    return (
      <div className="ao-composer-tools">
        <span className="ao-composer-hint">↑ / ↓ 키로 최근 입력 내역을 불러올 수 있다.</span>
      </div>
    );
  }

  return (
    <div className="ao-composer-tools">
      <div className="ao-composer-chips" aria-label="빠른 작업 참조">
        {quickTasks.map((task) => (
          <button
            key={task.taskId}
            type="button"
            className="ao-composer-chip"
            title={task.title}
            onClick={() => onInsert(`@작업#${task.taskId}`)}
          >
            @{`작업#${task.taskId}`}
          </button>
        ))}
      </div>
      <span className="ao-composer-hint">↑ / ↓ 최근 입력 · @작업 칩 클릭으로 작업 컨텍스트 삽입</span>
    </div>
  );
}
