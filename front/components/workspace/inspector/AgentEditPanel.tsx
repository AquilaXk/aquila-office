"use client";

import { useId, useMemo, useState, type FormEvent } from "react";
import { AlertCircle, CheckCircle2, Loader2, Plus, Save, Trash2 } from "lucide-react";
import { ModelCapabilityBadges } from "@/components/workspace/ModelCapabilityBadges";
import {
  AGENT_CATEGORY_OPTIONS,
  createLocalId,
  formatAgentCategory,
  messageFromError,
  type AgentMutationState,
  type AgentSkillFileDraft,
} from "@/components/workspace/workspace-ui-helpers";
import type {
  UpdateWorkspaceAgentInput,
  WorkspaceAgent,
  WorkspaceAgentCategory,
} from "@/lib/agents";
import {
  MAX_AGENT_SKILL_CONTENT_CHARS,
  MAX_AGENT_SKILL_FILE_NAME_CHARS,
  MAX_AGENT_SKILL_FILES,
  validateAgentSkillFiles,
} from "@/lib/agents-utils";
import { providerKindLabel } from "@/lib/model-catalog";
import {
  MAX_MODEL_PROVIDER_MODEL_ID_CHARS,
  suggestModelsForProvider,
  type WorkspaceModelProvider,
} from "@/lib/model-providers";

export const ACTOR_CATEGORY_OPTIONS_FALLBACK = AGENT_CATEGORY_OPTIONS;

export type AgentEditPanelProps = {
  agent: WorkspaceAgent;
  modelProviders?: readonly WorkspaceModelProvider[];
  canManage: boolean;
  agentDeleteConfirmId: number | null;
  agentMutation: AgentMutationState;
  onUpdateAgent: (agentId: number, input: UpdateWorkspaceAgentInput) => Promise<void>;
  onRequestDeleteAgent: (agentId: number | null) => void;
  onConfirmDeleteAgent: (agent: WorkspaceAgent) => void;
};

export function AgentEditPanel({
  agent,
  modelProviders = [],
  canManage,
  agentDeleteConfirmId,
  agentMutation,
  onUpdateAgent,
  onRequestDeleteAgent,
  onConfirmDeleteAgent,
}: AgentEditPanelProps) {
  const modelListId = useId();
  const [name, setName] = useState(agent.name);
  const [category, setCategory] = useState<WorkspaceAgentCategory>(agent.category);
  const [workspacePath, setWorkspacePath] = useState(agent.workspacePath ?? "");
  const [modelProviderId, setModelProviderId] = useState<number | null>(
    agent.modelProviderId ?? null,
  );
  const [modelId, setModelId] = useState(agent.modelId ?? "");
  const [fallbackModelId, setFallbackModelId] = useState(agent.fallbackModelId ?? "");
  const [skillFiles, setSkillFiles] = useState<AgentSkillFileDraft[]>(() =>
    (agent.skillFiles ?? []).map((file) => ({
      id: createLocalId(),
      fileName: file.fileName,
      content: file.content ?? "",
    })),
  );
  const suggestedModels = useMemo(
    () => suggestModelsForProvider(modelProviders, modelProviderId),
    [modelProviderId, modelProviders],
  );
  const [saving, setSaving] = useState(false);
  const [saveNotice, setSaveNotice] = useState<{
    tone: "success" | "danger";
    message: string;
  } | null>(null);

  const confirmingDelete = agentDeleteConfirmId === agent.agentId;
  const deleting =
    agentMutation.status === "submitting" && agentMutation.agentId === agent.agentId;

  function addSkillFile() {
    if (skillFiles.length >= MAX_AGENT_SKILL_FILES) {
      return;
    }
    setSkillFiles((current) => [...current, { id: createLocalId(), fileName: "", content: "" }]);
  }

  function updateSkillFile(id: string, field: "fileName" | "content", value: string) {
    setSkillFiles((current) =>
      current.map((file) => (file.id === id ? { ...file, [field]: value } : file)),
    );
  }

  function removeSkillFile(id: string) {
    setSkillFiles((current) => current.filter((file) => file.id !== id));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      setSaveNotice({ tone: "danger", message: "에이전트 이름을 입력해 주세요." });
      return;
    }

    const nonEmptyDrafts = skillFiles
      .map((file) => ({
        fileName: file.fileName.trim(),
        content: file.content,
      }))
      .filter((file) => file.fileName.length > 0 || file.content.trim().length > 0);

    let validatedSkillFiles;
    try {
      validatedSkillFiles = validateAgentSkillFiles(nonEmptyDrafts);
    } catch (error) {
      setSaveNotice({
        tone: "danger",
        message: messageFromError(error, "스킬 파일 입력이 올바르지 않습니다."),
      });
      return;
    }

    setSaving(true);
    setSaveNotice(null);
    try {
      await onUpdateAgent(agent.agentId, {
        name: trimmedName,
        category,
        workspacePath: workspacePath.trim() || undefined,
        modelProviderId: modelProviderId ?? null,
        modelId: modelId.trim() || null,
        fallbackModelId: fallbackModelId.trim() || null,
        updateModelConfig: true,
        skillFiles: validatedSkillFiles,
      });
      setSaveNotice({ tone: "success", message: "에이전트 설정을 저장했습니다." });
    } catch (error) {
      setSaveNotice({
        tone: "danger",
        message: messageFromError(error, "에이전트 설정 저장에 실패했습니다."),
      });
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <form className="flex flex-col gap-2.5" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>
            <strong>에이전트 이름</strong>
          </span>
          <input
            aria-label="에이전트 이름 수정"
            disabled={!canManage || saving}
            maxLength={120}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </label>

        <label className="form-field">
          <span>
            <strong>역할 분류</strong>
          </span>
          <select
            aria-label="에이전트 역할 수정"
            disabled={!canManage || saving}
            value={category}
            onChange={(event) => setCategory(event.target.value as WorkspaceAgentCategory)}
          >
            {AGENT_CATEGORY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {formatAgentCategory(option)}
              </option>
            ))}
          </select>
        </label>

        <label className="form-field">
          <span>
            <strong>작업 경로</strong>
          </span>
          <input
            aria-label="에이전트 작업 경로 수정"
            disabled={!canManage || saving}
            maxLength={255}
            placeholder="/workspace/backend"
            value={workspacePath}
            onChange={(event) => setWorkspacePath(event.target.value)}
          />
        </label>

        <label className="form-field">
          <span>
            <strong>AI 모델 프로바이더</strong>
          </span>
          <select
            aria-label="에이전트 모델 프로바이더 수정"
            disabled={!canManage || saving}
            value={modelProviderId ? String(modelProviderId) : ""}
            onChange={(event) => {
              const parsed = event.target.value ? Number.parseInt(event.target.value, 10) : null;
              setModelProviderId(parsed && parsed > 0 ? parsed : null);
            }}
          >
            <option value="">워크스페이스 기본 체인 (자동)</option>
            {modelProviders.map((provider) => (
              <option key={provider.providerId} value={provider.providerId}>
                {provider.displayName} ({providerKindLabel(provider.providerKind)})
              </option>
            ))}
          </select>
        </label>

        <div className="workspace-gateway-form-grid">
          <label className="form-field">
            <span>
              <strong>기본 모델</strong>
            </span>
            <input
              aria-label="에이전트 기본 모델 수정"
              list={modelListId}
              maxLength={MAX_MODEL_PROVIDER_MODEL_ID_CHARS}
              disabled={!canManage || saving}
              placeholder="예: gpt-4.1"
              value={modelId}
              onChange={(event) => setModelId(event.target.value)}
            />
            <ModelCapabilityBadges modelId={modelId} />
          </label>

          <label className="form-field">
            <span>
              <strong>폴백 모델</strong>
            </span>
            <input
              aria-label="에이전트 폴백 모델 수정"
              list={modelListId}
              maxLength={MAX_MODEL_PROVIDER_MODEL_ID_CHARS}
              disabled={!canManage || saving}
              placeholder="예: gemini-2.5-flash"
              value={fallbackModelId}
              onChange={(event) => setFallbackModelId(event.target.value)}
            />
          </label>
        </div>

        <datalist id={modelListId}>
          {suggestedModels.map((candidateModel) => (
            <option key={candidateModel} value={candidateModel} />
          ))}
        </datalist>

        <div className="flex items-center justify-between pt-1">
          <strong className="text-[12px] text-[var(--ui-text-strong)]">
            스킬 파일 ({skillFiles.length}/{MAX_AGENT_SKILL_FILES})
          </strong>
          {canManage ? (
            <button
              type="button"
              className="tb-btn"
              onClick={addSkillFile}
              disabled={saving || skillFiles.length >= MAX_AGENT_SKILL_FILES}
            >
              <Plus size={12} aria-hidden="true" />
              <span>스킬 추가</span>
            </button>
          ) : null}
        </div>

        {skillFiles.map((file, index) => (
          <div
            key={file.id}
            className="p-2 rounded-[var(--radius-md)] border border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)] flex flex-col gap-1.5"
          >
            <div className="flex items-center justify-between gap-2">
              <span className="text-[12px] font-medium text-[var(--ui-text-muted)]">
                스킬 #{index + 1}
              </span>
              {canManage ? (
                <button
                  type="button"
                  className="tb-btn"
                  onClick={() => removeSkillFile(file.id)}
                  disabled={saving}
                >
                  삭제
                </button>
              ) : null}
            </div>
            <input
              aria-label={`스킬 ${index + 1} 파일명`}
              maxLength={MAX_AGENT_SKILL_FILE_NAME_CHARS}
              placeholder="skill.md"
              disabled={!canManage || saving}
              value={file.fileName}
              onChange={(event) => updateSkillFile(file.id, "fileName", event.target.value)}
              className="h-8 px-2 rounded-[var(--radius-sm)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)]"
            />
            <textarea
              aria-label={`스킬 ${index + 1} 내용`}
              rows={4}
              maxLength={MAX_AGENT_SKILL_CONTENT_CHARS}
              placeholder="# 지침"
              disabled={!canManage || saving}
              value={file.content}
              onChange={(event) => updateSkillFile(file.id, "content", event.target.value)}
              className="p-2 rounded-[var(--radius-sm)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[12px] text-[var(--ui-text)] font-mono"
            />
          </div>
        ))}

        {saveNotice ? (
          <div
            className={`ao-inline-notice ${
              saveNotice.tone === "danger" ? "ao-inline-notice-danger" : "ao-inline-notice-success"
            }`}
            role={saveNotice.tone === "danger" ? "alert" : "status"}
          >
            {saveNotice.tone === "danger" ? (
              <AlertCircle size={14} aria-hidden="true" />
            ) : (
              <CheckCircle2 size={14} aria-hidden="true" />
            )}
            <span>{saveNotice.message}</span>
          </div>
        ) : null}

        {canManage ? (
          <button
            type="submit"
            className="primary-action primary-action-active"
            disabled={saving}
          >
            {saving ? (
              <Loader2 size={14} className="spin" aria-hidden="true" />
            ) : (
              <Save size={14} aria-hidden="true" />
            )}
            <span>{saving ? "저장 중" : "설정 저장"}</span>
          </button>
        ) : null}
      </form>

      {canManage ? (
        <div className="pt-3 border-t border-[var(--ui-border-subtle)] flex flex-col gap-2">
          <strong className="text-[12px] text-[var(--ui-text-muted)]">위험 구역</strong>
          {confirmingDelete ? (
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="danger-action flex-1"
                disabled={deleting}
                onClick={() => onConfirmDeleteAgent(agent)}
              >
                {deleting ? (
                  <Loader2 size={14} className="spin" aria-hidden="true" />
                ) : (
                  <Trash2 size={14} aria-hidden="true" />
                )}
                <span>{deleting ? "삭제 중" : "삭제 확인"}</span>
              </button>
              <button
                type="button"
                className="secondary-action"
                disabled={deleting}
                onClick={() => onRequestDeleteAgent(null)}
              >
                취소
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="danger-action"
              onClick={() => onRequestDeleteAgent(agent.agentId)}
            >
              <Trash2 size={14} aria-hidden="true" />
              <span>에이전트 삭제</span>
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
}
