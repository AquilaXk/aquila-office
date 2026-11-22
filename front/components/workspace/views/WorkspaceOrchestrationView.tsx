"use client";

import { useId, useMemo, useRef, useState, type FormEvent } from "react";
import {
  AlertCircle,
  CheckCircle2,
  GitFork,
  Loader2,
  Maximize2,
  Play,
  Plus,
  RefreshCw,
  Trash2,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import PixelAvatar from "@/components/arcade/PixelAvatar";
import { layoutTopologicalDag } from "@/lib/pixel-office/dag-bezier-edges";
import {
  createLocalId,
  formatDateTime,
  formatOrchestrationStatus,
  formatOrchestrationStepStatus,
  formatTaskStatus,
  messageFromError,
  type ModalLoadState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import {
  MAX_ORCHESTRATION_INSTRUCTION_CHARS,
  MAX_ORCHESTRATION_STEP_KEY_CHARS,
  MAX_ORCHESTRATION_STEPS,
  validateCreateOrchestrationInput,
  type CreateOrchestrationSessionInput,
  type OrchestrationSession,
  type OrchestrationStep,
} from "@/lib/orchestrations";
import type { WorkspaceTask } from "@/lib/tasks";

type StepDraft = {
  id: string;
  stepKey: string;
  agentId: string;
  dependsOnText: string;
  instruction: string;
};

export type WorkspaceOrchestrationViewProps = {
  tasks: WorkspaceTask[];
  agents: WorkspaceAgent[];
  sessions: OrchestrationSession[];
  sessionsState: ModalLoadState;
  selectedSession: OrchestrationSession | null;
  canManage: boolean;
  onRefreshSessions: () => void;
  onSelectSession: (sessionId: number) => void;
  onCreateSession: (input: CreateOrchestrationSessionInput) => Promise<void>;
  onDispatchSession: (sessionId: number, createPr: boolean) => Promise<void>;
  onInspectTask?: (taskId: number) => void;
};

export function WorkspaceOrchestrationView({
  tasks,
  agents,
  sessions,
  sessionsState,
  selectedSession,
  canManage,
  onRefreshSessions,
  onSelectSession,
  onCreateSession,
  onDispatchSession,
  onInspectTask,
}: WorkspaceOrchestrationViewProps) {
  const [sourceTaskId, setSourceTaskId] = useState<string>(
    tasks[0] ? String(tasks[0].taskId) : "",
  );
  const [steps, setSteps] = useState<StepDraft[]>(() => [
    {
      id: createLocalId(),
      stepKey: "plan",
      agentId: agents[0] ? String(agents[0].agentId) : "",
      dependsOnText: "",
      instruction: "",
    },
  ]);
  const [createPrOnDispatch, setCreatePrOnDispatch] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [dispatchingSessionId, setDispatchingSessionId] = useState<number | null>(null);
  const [selectedStepSelection, setSelectedStepSelection] = useState<{
    sessionId: number;
    stepKey: string;
  } | null>(null);
  const [notice, setNotice] = useState<{ tone: "success" | "danger"; message: string } | null>(
    null,
  );

  const selectedStepKey =
    selectedSession && selectedStepSelection?.sessionId === selectedSession.sessionId
      ? selectedStepSelection.stepKey
      : null;
  const selectedDagStep =
    selectedSession && selectedStepKey
      ? selectedSession.steps.find((s) => s.stepKey === selectedStepKey) ?? null
      : null;

  const agentById = useMemo(() => {
    const map = new Map<number, WorkspaceAgent>();
    for (let i = 0; i < agents.length; i += 1) {
      map.set(agents[i].agentId, agents[i]);
    }
    return map;
  }, [agents]);
  const svgMarkerPrefix = useId().replace(/:/g, "");
  const [dagZoom, setDagZoom] = useState(1);
  const dagScrollRef = useRef<HTMLDivElement | null>(null);
  const taskById = useMemo(() => {
    const map = new Map<number, WorkspaceTask>();
    for (let i = 0; i < tasks.length; i += 1) {
      map.set(tasks[i].taskId, tasks[i]);
    }
    return map;
  }, [tasks]);

  const effectiveSourceTaskId = sourceTaskId || (tasks[0] ? String(tasks[0].taskId) : "");

  function addStepDraft() {
    if (steps.length >= MAX_ORCHESTRATION_STEPS) {
      return;
    }
    const nextIndex = steps.length + 1;
    const prevKey = steps[steps.length - 1]?.stepKey.trim();
    setSteps((current) => [
      ...current,
      {
        id: createLocalId(),
        stepKey: `step-${nextIndex}`,
        agentId: agents[0] ? String(agents[0].agentId) : "",
        dependsOnText: prevKey || "",
        instruction: "",
      },
    ]);
  }

  function updateStep(id: string, field: keyof Omit<StepDraft, "id">, value: string) {
    setSteps((current) =>
      current.map((step) => (step.id === id ? { ...step, [field]: value } : step)),
    );
  }

  function removeStep(id: string) {
    setSteps((current) => (current.length > 1 ? current.filter((step) => step.id !== id) : current));
  }

  function resolvePresetAgentId(preferredCategory: WorkspaceAgent["category"]): string {
    const matched = agents.find((agent) => agent.category === preferredCategory) ?? agents[0];
    return matched ? String(matched.agentId) : "";
  }

  function applyStepPreset(preset: "standard" | "parallel" | "reset") {
    const defaultId = agents[0] ? String(agents[0].agentId) : "";
    if (preset === "reset") {
      setSteps([
        {
          id: createLocalId(),
          stepKey: "plan",
          agentId: defaultId,
          dependsOnText: "",
          instruction: "",
        },
      ]);
      return;
    }
    if (preset === "standard") {
      setSteps([
        {
          id: createLocalId(),
          stepKey: "plan",
          agentId: resolvePresetAgentId("ORCHESTRATOR"),
          dependsOnText: "",
          instruction: "요구사항과 변경 범위를 분석해 실행 계획을 수립합니다.",
        },
        {
          id: createLocalId(),
          stepKey: "implement",
          agentId: resolvePresetAgentId("BACKEND"),
          dependsOnText: "plan",
          instruction: "수립된 계획에 따라 기능 구현 및 단위 테스트를 진행합니다.",
        },
        {
          id: createLocalId(),
          stepKey: "verify",
          agentId: resolvePresetAgentId("QA"),
          dependsOnText: "implement",
          instruction: "통합 테스트와 정적 검증을 실행하고 결과를 요약합니다.",
        },
      ]);
      return;
    }
    setSteps([
      {
        id: createLocalId(),
        stepKey: "plan",
        agentId: resolvePresetAgentId("ORCHESTRATOR"),
        dependsOnText: "",
        instruction: "백엔드와 프론트엔드 작업 범위 및 연동 계약을 정의합니다.",
      },
      {
        id: createLocalId(),
        stepKey: "backend",
        agentId: resolvePresetAgentId("BACKEND"),
        dependsOnText: "plan",
        instruction: "API 엔드포인트, 서비스 로직 및 단위 테스트를 구현합니다.",
      },
      {
        id: createLocalId(),
        stepKey: "frontend",
        agentId: resolvePresetAgentId("FRONTEND"),
        dependsOnText: "plan",
        instruction: "UI 컴포넌트, 상태 연동 및 접근성을 구현합니다.",
      },
      {
        id: createLocalId(),
        stepKey: "review",
        agentId: resolvePresetAgentId("QA"),
        dependsOnText: "backend, frontend",
        instruction: "전체 회귀 테스트를 실행하고 최종 품질을 검토합니다.",
      },
    ]);
  }

  function toggleDependencyKey(stepId: string, dependencyKey: string) {
    setSteps((current) =>
      current.map((step) => {
        if (step.id !== stepId) return step;
        const existing = step.dependsOnText
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean);
        const next = existing.includes(dependencyKey)
          ? existing.filter((item) => item !== dependencyKey)
          : [...existing, dependencyKey];
        return { ...step, dependsOnText: next.join(", ") };
      }),
    );
  }

  async function handleCreateSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const parsedSourceTaskId = Number.parseInt(effectiveSourceTaskId, 10);
    const payload: CreateOrchestrationSessionInput = {
      sourceTaskId: parsedSourceTaskId,
      steps: steps.map((step) => ({
        stepKey: step.stepKey.trim(),
        instruction: step.instruction.trim(),
        agentId: Number.parseInt(step.agentId, 10),
        dependsOn: step.dependsOnText
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
      })),
    };

    let validatedPayload: CreateOrchestrationSessionInput;
    try {
      validatedPayload = validateCreateOrchestrationInput(payload);
    } catch (error) {
      setNotice({
        tone: "danger",
        message: messageFromError(error, "오케스트레이션 구성이 올바르지 않습니다."),
      });
      return;
    }

    setSubmittingCreate(true);
    setNotice(null);
    try {
      await onCreateSession(validatedPayload);
      setNotice({ tone: "success", message: "오케스트레이션 세션을 생성했습니다." });
    } catch (error) {
      setNotice({
        tone: "danger",
        message: messageFromError(error, "오케스트레이션 세션 생성에 실패했습니다."),
      });
    } finally {
      setSubmittingCreate(false);
    }
  }

  async function handleDispatch(sessionId: number) {
    setDispatchingSessionId(sessionId);
    setNotice(null);
    try {
      await onDispatchSession(sessionId, createPrOnDispatch);
      setNotice({
        tone: "success",
        message: `세션 #${sessionId} 실행을 전달했습니다.`,
      });
    } catch (error) {
      setNotice({
        tone: "danger",
        message: messageFromError(error, "오케스트레이션 실행 전달에 실패했습니다."),
      });
    } finally {
      setDispatchingSessionId(null);
    }
  }

  return (
    <div className="chrome-view-scroll" aria-label="오케스트레이션 뷰">
      <div className="ao-page-header">
        <div>
          <p className="ao-page-eyebrow">다중 에이전트 파이프라인</p>
          <h2 className="ao-page-title">오케스트레이션 세션</h2>
          <p className="ao-page-subtitle">
            원본 작업을 기준으로 선후 의존성(DAG)을 갖는 에이전트 단계들을 구성하고 실행합니다.
          </p>
        </div>

        <button
          type="button"
          className="secondary-action"
          onClick={onRefreshSessions}
          disabled={sessionsState.status === "loading"}
        >
          <RefreshCw
            size={14}
            className={sessionsState.status === "loading" ? "spin" : undefined}
            aria-hidden="true"
          />
          <span>세션 새로고침</span>
        </button>
      </div>

      {notice ? (
        <div
          className={`ao-inline-notice ${
            notice.tone === "danger" ? "ao-inline-notice-danger" : "ao-inline-notice-success"
          }`}
          role={notice.tone === "danger" ? "alert" : "status"}
        >
          {notice.tone === "danger" ? (
            <AlertCircle size={15} aria-hidden="true" />
          ) : (
            <CheckCircle2 size={15} aria-hidden="true" />
          )}
          <span>{notice.message}</span>
        </div>
      ) : null}

      <div className="chrome-orchestration-grid">
        <form className="ao-surface-panel flex flex-col gap-3" onSubmit={handleCreateSubmit}>
          <div className="ao-surface-panel-header">
            <h3>새 오케스트레이션 구성</h3>
            <span className="role-badge">
              {steps.length}/{MAX_ORCHESTRATION_STEPS}
            </span>
          </div>

          <label className="form-field">
            <span>
              <strong>원본 작업</strong>
              <small>필수</small>
            </span>
            <select
              aria-label="오케스트레이션 원본 작업"
              value={effectiveSourceTaskId}
              disabled={!canManage || submittingCreate || tasks.length === 0}
              onChange={(event) => setSourceTaskId(event.target.value)}
            >
              {tasks.length === 0 ? <option value="">작업이 없습니다</option> : null}
              {tasks.map((task) => (
                <option key={task.taskId} value={task.taskId}>
                  #{task.taskId} · {task.title}
                </option>
              ))}
            </select>
          </label>

          <div className="flex flex-wrap items-center justify-between gap-2">
            <strong className="text-[13px] text-[var(--ui-text-strong)]">실행 단계 (DAG)</strong>
            <button
              type="button"
              className="tb-btn"
              disabled={!canManage || submittingCreate || steps.length >= MAX_ORCHESTRATION_STEPS}
              onClick={addStepDraft}
            >
              <Plus size={13} aria-hidden="true" />
              <span>단계 추가</span>
            </button>
          </div>

          <div className="ao-preset-bar" aria-label="단계 프리셋">
            <button
              type="button"
              className="tb-btn"
              disabled={!canManage || submittingCreate}
              onClick={() => applyStepPreset("standard")}
            >
              3단계 표준 검증
            </button>
            <button
              type="button"
              className="tb-btn"
              disabled={!canManage || submittingCreate}
              onClick={() => applyStepPreset("parallel")}
            >
              4단계 병렬 구현
            </button>
            <button
              type="button"
              className="tb-btn"
              disabled={!canManage || submittingCreate}
              onClick={() => applyStepPreset("reset")}
            >
              초기화
            </button>
          </div>

          <div className="flex flex-col gap-2">
            {steps.map((step, index) => {
              const priorKeys = steps
                .slice(0, index)
                .map((candidate) => candidate.stepKey.trim())
                .filter(Boolean);
              const selectedDeps = new Set(
                step.dependsOnText
                  .split(",")
                  .map((item) => item.trim())
                  .filter(Boolean),
              );
              return (
              <div className="chrome-step-card" key={step.id}>
                <div className="flex items-center justify-between gap-2">
                  <strong className="text-[12px] text-[var(--ui-text-strong)]">
                    단계 {index + 1}
                  </strong>
                  {steps.length > 1 ? (
                    <button
                      type="button"
                      className="tb-btn"
                      onClick={() => removeStep(step.id)}
                      disabled={submittingCreate}
                      aria-label={`단계 ${index + 1} 삭제`}
                    >
                      <Trash2 size={12} aria-hidden="true" />
                      <span>삭제</span>
                    </button>
                  ) : null}
                </div>

                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>단계 키</strong>
                    </span>
                    <input
                      maxLength={MAX_ORCHESTRATION_STEP_KEY_CHARS}
                      disabled={!canManage || submittingCreate}
                      placeholder="예: backend-api"
                      value={step.stepKey}
                      onChange={(event) => updateStep(step.id, "stepKey", event.target.value)}
                    />
                  </label>
                  <label className="form-field">
                    <span>
                      <strong>담당 에이전트</strong>
                    </span>
                    <select
                      disabled={!canManage || submittingCreate || agents.length === 0}
                      value={step.agentId}
                      onChange={(event) => updateStep(step.id, "agentId", event.target.value)}
                    >
                      <option value="">에이전트 선택</option>
                      {agents.map((agent) => (
                        <option key={agent.agentId} value={agent.agentId}>
                          {agent.name}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <label className="form-field">
                  <span>
                    <strong>선행 단계 키</strong>
                    <small>쉼표 구분</small>
                  </span>
                  <input
                    disabled={!canManage || submittingCreate}
                    placeholder="예: plan, schema"
                    value={step.dependsOnText}
                    onChange={(event) => updateStep(step.id, "dependsOnText", event.target.value)}
                  />
                  {priorKeys.length > 0 ? (
                    <div className="ao-dep-chip-row" aria-label="이전 단계 선택">
                      {priorKeys.map((candidateKey) => {
                        const active = selectedDeps.has(candidateKey);
                        return (
                          <button
                            key={candidateKey}
                            type="button"
                            className="ao-dep-chip"
                            aria-pressed={active}
                            disabled={!canManage || submittingCreate}
                            onClick={() => toggleDependencyKey(step.id, candidateKey)}
                          >
                            {candidateKey}
                          </button>
                        );
                      })}
                    </div>
                  ) : null}
                </label>

                <label className="form-field">
                  <span>
                    <strong>지시 내용</strong>
                    <small>
                      {step.instruction.length}/{MAX_ORCHESTRATION_INSTRUCTION_CHARS}
                    </small>
                  </span>
                  <textarea
                    rows={2}
                    maxLength={MAX_ORCHESTRATION_INSTRUCTION_CHARS}
                    disabled={!canManage || submittingCreate}
                    placeholder="이 단계에서 수행할 작업 지시를 입력하세요."
                    value={step.instruction}
                    onChange={(event) => updateStep(step.id, "instruction", event.target.value)}
                  />
                </label>
              </div>
              );
            })}
          </div>

          <button
            type="submit"
            className="primary-action primary-action-active"
            disabled={!canManage || submittingCreate || tasks.length === 0 || agents.length === 0}
          >
            {submittingCreate ? (
              <Loader2 size={14} className="spin" aria-hidden="true" />
            ) : (
              <GitFork size={14} aria-hidden="true" />
            )}
            <span>{submittingCreate ? "생성 중" : "오케스트레이션 세션 생성"}</span>
          </button>
        </form>

        <section className="flex flex-col gap-3" aria-label="세션 목록 및 상세">
          <div className="ao-surface-panel">
            <div className="ao-surface-panel-header">
              <h3>등록된 세션 ({sessions.length})</h3>
            </div>

            {sessionsState.status === "error" ? (
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={14} aria-hidden="true" />
                <span>{sessionsState.error}</span>
              </div>
            ) : null}

            {sessions.length === 0 ? (
              <p className="m-0 py-4 text-[13px] text-[var(--ui-text-secondary)]">
                생성된 오케스트레이션 세션이 없습니다. 왼쪽 폼에서 다중 에이전트 흐름을 만들어 주세요.
              </p>
            ) : (
              <div className="flex flex-col gap-2">
                {sessions.map((session) => {
                  const selected = selectedSession?.sessionId === session.sessionId;
                  return (
                    <button
                      key={session.sessionId}
                      type="button"
                      onClick={() => onSelectSession(session.sessionId)}
                      className={`chrome-sidebar-item border border-[var(--ui-border-subtle)]${
                        selected ? " chrome-sidebar-item-active" : ""
                      }`}
                    >
                      <span className="inline-flex items-center gap-2 min-w-0">
                        <GitFork size={14} aria-hidden="true" />
                        <strong className="font-mono">세션 #{session.sessionId}</strong>
                        <span className="text-[var(--ui-text-secondary)]">
                          원본 작업 #{session.sourceTaskId} · {session.steps.length}단계
                        </span>
                      </span>
                      <span className="role-badge">{formatOrchestrationStatus(session.status)}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {selectedSession ? (
            <div className="ao-surface-panel" aria-label="선택된 오케스트레이션 세션 상세">
              <div className="ao-surface-panel-header">
                <div>
                  <h3>세션 #{selectedSession.sessionId}</h3>
                  <p className="m-0 text-[12px] text-[var(--ui-text-muted)]">
                    원본 작업 #{selectedSession.sourceTaskId}
                    {selectedSession.createdAt ? ` · ${formatDateTime(selectedSession.createdAt)}` : ""}
                  </p>
                </div>
                <span className="role-badge">
                  {formatOrchestrationStatus(selectedSession.status)}
                </span>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                <label className="inline-flex items-center gap-2 text-[12px] cursor-pointer">
                  <input
                    type="checkbox"
                    checked={createPrOnDispatch}
                    disabled={!canManage || dispatchingSessionId === selectedSession.sessionId}
                    onChange={(event) => setCreatePrOnDispatch(event.target.checked)}
                  />
                  <span>실행 후 GitHub PR 생성 요청</span>
                </label>

                <button
                  type="button"
                  className="primary-action primary-action-active"
                  disabled={!canManage || dispatchingSessionId === selectedSession.sessionId}
                  onClick={() => void handleDispatch(selectedSession.sessionId)}
                >
                  {dispatchingSessionId === selectedSession.sessionId ? (
                    <Loader2 size={14} className="spin" aria-hidden="true" />
                  ) : (
                    <Play size={14} aria-hidden="true" />
                  )}
                  <span>세션 실행</span>
                </button>
              </div>

              {(() => {
                const dagLayout = layoutTopologicalDag(selectedSession.steps, {
                  nodeWidth: 164,
                  nodeHeight: 88,
                  colGap: 34,
                  rowGap: 14,
                  padX: 12,
                  padY: 36,
                  bottomPad: 12,
                });
                if (dagLayout.nodes.length === 0) return null;
                const stepByKey = new Map<string, OrchestrationStep>(
                  selectedSession.steps.map((s) => [s.stepKey, s]),
                );
                const markerIdle = `${svgMarkerPrefix}-arrow-idle`;
                const markerOk = `${svgMarkerPrefix}-arrow-ok`;
                const markerActive = `${svgMarkerPrefix}-arrow-active`;
                const handleFitDag = () => {
                  const width = dagScrollRef.current?.clientWidth ?? 0;
                  if (width <= 0 || dagLayout.width <= 0) {
                    setDagZoom(1);
                    return;
                  }
                  const fitted = Math.max(
                    0.6,
                    Math.min(1.2, Number(((width - 16) / dagLayout.width).toFixed(2))),
                  );
                  setDagZoom(fitted);
                };
                return (
                  <div className="ao-dag-card mb-3" aria-label="오케스트레이션 실행 그래프">
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                      <strong className="text-[12px] text-[var(--ui-text-strong)]">
                        위상 실행 파이프라인 ({selectedSession.steps.length}단계 · {dagLayout.levels.length}개 스테이지)
                      </strong>
                      <div className="ao-dag-zoom-controls">
                        <button
                          type="button"
                          className="tb-btn"
                          aria-label="그래프 축소"
                          title="그래프 축소"
                          disabled={dagZoom <= 0.6}
                          onClick={() =>
                            setDagZoom((z) => Math.max(0.6, Number((z - 0.15).toFixed(2))))
                          }
                        >
                          <ZoomOut size={12} aria-hidden="true" />
                        </button>
                        <span className="ao-dag-zoom-badge">{Math.round(dagZoom * 100)}%</span>
                        <button
                          type="button"
                          className="tb-btn"
                          aria-label="그래프 확대"
                          title="그래프 확대"
                          disabled={dagZoom >= 1.6}
                          onClick={() =>
                            setDagZoom((z) => Math.min(1.6, Number((z + 0.15).toFixed(2))))
                          }
                        >
                          <ZoomIn size={12} aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          className="tb-btn"
                          onClick={handleFitDag}
                        >
                          <Maximize2 size={12} aria-hidden="true" />
                          <span>화면에 맞춤</span>
                        </button>
                      </div>
                    </div>
                    <div className="ao-dag-canvas-scroll" ref={dagScrollRef}>
                      <div
                        className="ao-dag-stage"
                        style={{
                          width: Math.round(dagLayout.width * dagZoom),
                          height: Math.round(dagLayout.height * dagZoom),
                        }}
                      >
                        <div
                          style={{
                            position: "relative",
                            width: dagLayout.width,
                            height: dagLayout.height,
                            transform: `scale(${dagZoom})`,
                            transformOrigin: "top left",
                          }}
                        >
                        {dagLayout.levels.map((lvl) => (
                          <span
                            key={`lvl-${lvl.level}`}
                            className="ao-dag-level-title ao-dag-level-header"
                            style={{ left: lvl.x, width: lvl.width }}
                          >
                            단계 레벨 {lvl.level + 1}
                          </span>
                        ))}

                        <svg
                          className="ao-dag-svg"
                          width={dagLayout.width}
                          height={dagLayout.height}
                          viewBox={`0 0 ${dagLayout.width} ${dagLayout.height}`}
                          aria-hidden="true"
                        >
                          <defs>
                            <marker
                              id={markerIdle}
                              markerWidth="8"
                              markerHeight="8"
                              refX="6.5"
                              refY="4"
                              orient="auto"
                            >
                              <path d="M 0 1 L 7 4 L 0 7 z" fill="var(--ui-text-muted)" />
                            </marker>
                            <marker
                              id={markerOk}
                              markerWidth="8"
                              markerHeight="8"
                              refX="6.5"
                              refY="4"
                              orient="auto"
                            >
                              <path d="M 0 1 L 7 4 L 0 7 z" fill="var(--studio-ok)" />
                            </marker>
                            <marker
                              id={markerActive}
                              markerWidth="8"
                              markerHeight="8"
                              refX="6.5"
                              refY="4"
                              orient="auto"
                            >
                              <path d="M 0 1 L 7 4 L 0 7 z" fill="var(--studio-warn)" />
                            </marker>
                          </defs>
                          {dagLayout.edges.map((edge) => {
                            const tone =
                              edge.status === "SUCCEEDED" || edge.status === "DONE"
                                ? "ok"
                                : edge.status === "RUNNING" || edge.status === "IN_PROGRESS"
                                  ? "active"
                                  : "idle";
                            const markerId =
                              tone === "ok"
                                ? markerOk
                                : tone === "active"
                                  ? markerActive
                                  : markerIdle;
                            return (
                              <g key={edge.id}>
                                <path
                                  d={edge.path}
                                  className={`ao-dag-edge ao-dag-edge--${tone}`}
                                  markerEnd={`url(#${markerId})`}
                                />
                                {tone === "active" ? (
                                  <path d={edge.path} className="ao-dag-edge-flow" />
                                ) : null}
                              </g>
                            );
                          })}
                        </svg>

                        {dagLayout.nodes.map((box) => {
                          const step = stepByKey.get(box.stepKey);
                          if (!step) return null;
                          const agent = agentById.get(step.agentId);
                          const agentName = agent?.name ?? `에이전트 #${step.agentId}`;
                          const isSelected = selectedStepKey === step.stepKey;
                          return (
                            <button
                              type="button"
                              key={step.stepKey}
                              aria-pressed={isSelected}
                              className={`ao-dag-node ao-dag-node-positioned ao-dag-node-btn${
                                isSelected ? " ao-dag-node-selected" : ""
                              }`}
                              data-status={step.status}
                              style={{
                                left: box.x,
                                top: box.y,
                                width: box.width,
                                height: box.height,
                              }}
                              onClick={() =>
                                setSelectedStepSelection((current) =>
                                  current?.sessionId === selectedSession.sessionId &&
                                  current.stepKey === step.stepKey
                                    ? null
                                    : { sessionId: selectedSession.sessionId, stepKey: step.stepKey },
                                )
                              }
                            >
                              <div className="flex items-center justify-between gap-2 min-w-0">
                                <span className="font-mono text-[12px] font-semibold text-[var(--ui-text-strong)] truncate">
                                  {step.stepKey}
                                </span>
                                <span className="role-badge">
                                  {formatOrchestrationStepStatus(step.status)}
                                </span>
                              </div>
                              <div className="flex items-center gap-2 text-[12px] text-[var(--ui-text-secondary)] min-w-0">
                                <PixelAvatar
                                  kind="agent"
                                  variant="mugshot"
                                  variantKey={agentName}
                                  agentCategory={agent?.category}
                                  size={1.65}
                                />
                                <span className="truncate">{agentName}</span>
                              </div>
                              <span className="text-[12px] text-[var(--ui-text-muted)] truncate">
                                {step.dependsOn.length > 0
                                  ? `선행: ${step.dependsOn.join(" → ")}`
                                  : "루트 엔트리 단계"}
                              </span>
                            </button>
                          );
                        })}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })()}

              {selectedDagStep ? (
                <section
                  className="ao-dag-detail-card mb-3"
                  aria-label="선택 단계 상세"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="m-0 text-[13px] font-semibold text-[var(--ui-text-strong)]">
                        선택 단계 상세
                      </h4>
                      <code className="role-badge font-mono">{selectedDagStep.stepKey}</code>
                      <span className="role-badge">
                        {formatOrchestrationStepStatus(selectedDagStep.status)}
                      </span>
                    </div>
                    {selectedDagStep.taskId ? (
                      <button
                        type="button"
                        className="tb-btn tb-btn-primary"
                        onClick={() => onInspectTask?.(selectedDagStep.taskId!)}
                      >
                        <span>작업 인스펙터 열기</span>
                      </button>
                    ) : null}
                  </div>
                  <div className="flex flex-wrap items-center gap-3 text-[12px] text-[var(--ui-text-secondary)]">
                    <span>
                      담당:{" "}
                      <strong className="text-[var(--ui-text-strong)]">
                        {agentById.get(selectedDagStep.agentId)?.name ??
                          `에이전트 #${selectedDagStep.agentId}`}
                      </strong>
                    </span>
                    <span>
                      모델:{" "}
                      <code className="font-mono text-[var(--ui-text-strong)]">
                        {agentById.get(selectedDagStep.agentId)?.modelId ?? "워크스페이스 기본 체인"}
                      </code>
                    </span>
                    <span>
                      선행 단계:{" "}
                      {selectedDagStep.dependsOn.length > 0
                        ? selectedDagStep.dependsOn.join(" → ")
                        : "없음 (루트)"}
                    </span>
                  </div>
                  {(() => {
                    const linkedTask = selectedDagStep.taskId
                      ? taskById.get(selectedDagStep.taskId) ?? null
                      : null;
                    if (!linkedTask) return null;
                    return (
                      <div className="ao-dag-linked-task-card">
                        <div className="ao-dag-linked-task-meta">
                          <strong className="text-[var(--ui-text-strong)]">
                            연결 작업 #{linkedTask.taskId}: {linkedTask.title}
                          </strong>
                          <span className="role-badge">{formatTaskStatus(linkedTask.status)}</span>
                        </div>
                        {linkedTask.description ? (
                          <p className="m-0 text-[12px] text-[var(--ui-text-secondary)] line-clamp-2">
                            {linkedTask.description}
                          </p>
                        ) : null}
                      </div>
                    );
                  })()}
                  <p className="m-0 text-[13px] text-[var(--ui-text)] whitespace-pre-wrap">
                    {selectedDagStep.instruction}
                  </p>
                </section>
              ) : null}

              <div className="flex flex-col gap-2">
                {selectedSession.steps.map((step, index) => (
                  <div className="chrome-step-card" key={`${step.stepKey}-${index}`}>
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <strong className="font-mono text-[13px] text-[var(--ui-text-strong)]">
                          {step.stepKey}
                        </strong>
                        <span className="role-badge">
                          {agentById.get(step.agentId)?.name ?? `에이전트 #${step.agentId}`}
                        </span>
                        {step.taskId ? (
                          <span className="role-badge">작업 #{step.taskId}</span>
                        ) : null}
                      </div>
                      <span className="role-badge">
                        {formatOrchestrationStepStatus(step.status)}
                      </span>
                    </div>

                    <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                      {step.instruction}
                    </p>

                    {step.dependsOn.length > 0 ? (
                      <div className="text-[12px] text-[var(--ui-text-muted)]">
                        선행 단계: {step.dependsOn.join(", ")}
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </section>
      </div>
    </div>
  );
}
