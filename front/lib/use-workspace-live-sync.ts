"use client";

import { useEffect, type Dispatch, type SetStateAction } from "react";
import type { InspectorTarget } from "@/components/workspace/inspector/WorkspaceContextInspector";
import type { WorkspaceViewId } from "@/components/workspace/workspace-ui-helpers";
import { resolveAccessToken } from "@/lib/auth";
import { sharedPollBucket, withFetchDeadline } from "@/lib/poll-bucket";
import {
  chatSessionMessagePageInput,
  fetchChatSessionMessages,
  fetchTaskChatMessages,
  type ChatMessage,
} from "@/lib/chat";
import {
  fetchTaskExecutionLogs,
  fetchTaskReports,
  fetchWorkspaceTasks,
  type AgentReport,
  type TaskExecutionLog,
  type WorkspaceTask,
} from "@/lib/tasks";
import { hostDocumentHidden } from "@/lib/visibility-poll";
import {
  fetchWorkspaceDashboardSummary,
  type WorkspaceDashboardSummary,
} from "@/lib/workspaces";
import {
  isEditableEventTarget,
  matchViewSwitchShortcut,
  type ShortcutStyle,
} from "@/lib/workspace-shortcuts";

export type TaskInspectorSnapshot = {
  logsRes: PromiseSettledResult<TaskExecutionLog[]>;
  reportsRes: PromiseSettledResult<AgentReport[]>;
  chatRes: PromiseSettledResult<ChatMessage[]>;
  resolvedChatMessages: ChatMessage[] | null;
};

export async function fetchTaskInspectorSnapshot(
  token: string,
  workspaceId: number,
  taskId: number,
  signal?: AbortSignal,
): Promise<TaskInspectorSnapshot> {
  const [logsRes, reportsRes, chatRes] = await Promise.allSettled([
    fetchTaskExecutionLogs(token, workspaceId, taskId, signal),
    fetchTaskReports(token, workspaceId, taskId, signal),
    fetchTaskChatMessages(token, workspaceId, taskId, signal),
  ]);

  let resolvedChatMessages: ChatMessage[] | null = null;
  if (chatRes.status === "fulfilled") {
    const sessionId = chatRes.value[0]?.chatSessionId ?? null;
    if (sessionId && !signal?.aborted) {
      try {
        const page = await fetchChatSessionMessages(
          token,
          workspaceId,
          sessionId,
          chatSessionMessagePageInput(),
          signal,
        );
        resolvedChatMessages = page.messages;
      } catch {
        resolvedChatMessages = chatRes.value;
      }
    } else {
      resolvedChatMessages = chatRes.value;
    }
  }

  return { logsRes, reportsRes, chatRes, resolvedChatMessages };
}

export type UseWorkspaceLiveSyncParams = {
  enabled: boolean;
  workspaceId: number;
  hasActiveExecution: boolean;
  inspectorTarget: InspectorTarget;
  actorNodeId: string | null;
  actorChatSessionId: number | null;
  shortcutStyle: ShortcutStyle;
  viewOrder: readonly WorkspaceViewId[];
  commandPaletteOpen: boolean;
  shortcutsOpen: boolean;
  hasOtherOverlay: boolean;
  onSelectView: (view: WorkspaceViewId) => void;
  onSetCommandPaletteOpen: Dispatch<SetStateAction<boolean>>;
  onSetShortcutsOpen: Dispatch<SetStateAction<boolean>>;
  onCloseInspector: () => void;
  onTasksUpdated: (tasks: WorkspaceTask[]) => void;
  onSummaryUpdated: (summary: WorkspaceDashboardSummary) => void;
  onPollActorChatSilent: (sessionId: number, nodeId: string, signal: AbortSignal) => Promise<void>;
  onRefreshInspectorTaskSilent: (taskId: number, signal: AbortSignal) => Promise<void>;
  onRefreshOrchestrationsSilent: (signal: AbortSignal) => Promise<void>;
};

export function useWorkspaceLiveSync({
  enabled,
  workspaceId,
  hasActiveExecution,
  inspectorTarget,
  actorNodeId,
  actorChatSessionId,
  shortcutStyle,
  viewOrder,
  commandPaletteOpen,
  shortcutsOpen,
  hasOtherOverlay,
  onSelectView,
  onSetCommandPaletteOpen,
  onSetShortcutsOpen,
  onCloseInspector,
  onTasksUpdated,
  onSummaryUpdated,
  onPollActorChatSilent,
  onRefreshInspectorTaskSilent,
  onRefreshOrchestrationsSilent,
}: UseWorkspaceLiveSyncParams): void {
  // Baseline 15s workspace tasks & token budget summary sync
  useEffect(() => {
    if (!enabled) {
      return;
    }
    let active = true;
    const unsubscribe = sharedPollBucket.subscribe(
      `workspace:${workspaceId}:tasks-summary`,
      15_000,
      async (bucketSignal) => {
        const token = resolveAccessToken();
        if (!token || !active || hostDocumentHidden()) {
          return;
        }
        await withFetchDeadline(
          async (deadlineSignal) => {
            const [tasksResult, summaryResult] = await Promise.allSettled([
              fetchWorkspaceTasks(token, workspaceId, deadlineSignal),
              fetchWorkspaceDashboardSummary(token, workspaceId, deadlineSignal),
            ]);
            if (!active || deadlineSignal.aborted) {
              return;
            }
            if (tasksResult.status === "fulfilled") {
              onTasksUpdated(tasksResult.value);
            }
            if (summaryResult.status === "fulfilled") {
              onSummaryUpdated(summaryResult.value);
            }
          },
          { timeoutMs: 10_000, signal: bucketSignal },
        );
      },
      { immediate: false },
    );

    return () => {
      active = false;
      unsubscribe();
    };
  }, [enabled, onSummaryUpdated, onTasksUpdated, workspaceId]);

  // Coalesced 4s live polling while an inspector is open or execution is running
  useEffect(() => {
    const needsLiveSync = enabled && (inspectorTarget !== null || hasActiveExecution);
    if (!needsLiveSync) {
      return;
    }
    let active = true;
    let inFlight = false;
    const effectController = new AbortController();

    const unsubscribe = sharedPollBucket.subscribe(
      `workspace:${workspaceId}:live-sync`,
      4_000,
      async (bucketSignal) => {
        const token = resolveAccessToken();
        if (!token || !active || hostDocumentHidden() || inFlight) {
          return;
        }
        inFlight = true;
        const tickController = new AbortController();
        const forwardAbort = () => tickController.abort();
        if (bucketSignal.aborted || effectController.signal.aborted) {
          tickController.abort();
        } else {
          bucketSignal.addEventListener("abort", forwardAbort, { once: true });
          effectController.signal.addEventListener("abort", forwardAbort, { once: true });
        }

        try {
          await withFetchDeadline(
            async (deadlineSignal) => {
              const jobs: Promise<unknown>[] = [];
              if (inspectorTarget?.type === "task") {
                jobs.push(onRefreshInspectorTaskSilent(inspectorTarget.taskId, deadlineSignal));
              } else if (
                inspectorTarget?.type === "actor" &&
                actorNodeId !== null &&
                actorChatSessionId !== null
              ) {
                jobs.push(onPollActorChatSilent(actorChatSessionId, actorNodeId, deadlineSignal));
              }

              if (hasActiveExecution) {
                jobs.push(
                  fetchWorkspaceTasks(token, workspaceId, deadlineSignal).then((nextTasks) => {
                    if (active && !deadlineSignal.aborted) {
                      onTasksUpdated(nextTasks);
                    }
                  }),
                );
                jobs.push(onRefreshOrchestrationsSilent(deadlineSignal));
              }
              await Promise.allSettled(jobs);
            },
            { timeoutMs: 6_000, signal: tickController.signal },
          );
        } finally {
          bucketSignal.removeEventListener("abort", forwardAbort);
          effectController.signal.removeEventListener("abort", forwardAbort);
          inFlight = false;
        }
      },
      { immediate: false },
    );

    return () => {
      active = false;
      effectController.abort();
      unsubscribe();
    };
  }, [
    actorChatSessionId,
    actorNodeId,
    enabled,
    hasActiveExecution,
    inspectorTarget,
    onPollActorChatSilent,
    onRefreshInspectorTaskSilent,
    onRefreshOrchestrationsSilent,
    onTasksUpdated,
    workspaceId,
  ]);

  // Global keyboard shortcuts (Cmd/Ctrl+K, ?, Esc, View switcher)
  useEffect(() => {
    const anyOverlayOpen = Boolean(commandPaletteOpen || shortcutsOpen || hasOtherOverlay);

    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        onSetShortcutsOpen(false);
        onSetCommandPaletteOpen((open) => !open);
        return;
      }

      if (event.key === "Escape") {
        if (commandPaletteOpen) {
          event.preventDefault();
          onSetCommandPaletteOpen(false);
          return;
        }
        if (shortcutsOpen) {
          event.preventDefault();
          onSetShortcutsOpen(false);
          return;
        }
        if (!hasOtherOverlay && inspectorTarget) {
          event.preventDefault();
          onCloseInspector();
        }
        return;
      }

      if (!anyOverlayOpen && !isEditableEventTarget(event.target)) {
        if (event.key === "?" && !event.metaKey && !event.ctrlKey && !event.altKey) {
          event.preventDefault();
          onSetShortcutsOpen(true);
          return;
        }
        const viewIndex = matchViewSwitchShortcut(shortcutStyle, event, viewOrder.length);
        if (viewIndex !== null) {
          event.preventDefault();
          onSelectView(viewOrder[viewIndex]);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [
    commandPaletteOpen,
    hasOtherOverlay,
    inspectorTarget,
    onCloseInspector,
    onSelectView,
    onSetCommandPaletteOpen,
    onSetShortcutsOpen,
    shortcutStyle,
    shortcutsOpen,
    viewOrder,
  ]);
}
