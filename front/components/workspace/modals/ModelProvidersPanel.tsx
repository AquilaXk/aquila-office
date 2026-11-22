"use client";

import { useMemo, useState, type FormEvent } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Cpu,
  KeyRound,
  ListFilter,
  Loader2,
  Pencil,
  Play,
  RefreshCw,
  Search,
  Sparkles,
  Trash2,
} from "lucide-react";
import {
  filterCatalogPresets,
  findCatalogPreset,
  providerKindLabel,
  providerTierLabel,
  wireProtocolLabel,
  type ModelCatalogPreset,
  type ModelProviderKind,
  type ModelProviderTier,
  type WireProtocol,
} from "@/lib/model-catalog";
import {
  DEFAULT_MODEL_PROVIDER_PRIORITY,
  MAX_MODEL_PROVIDER_API_KEY_CHARS,
  MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS,
  MAX_MODEL_PROVIDER_MODEL_ID_CHARS,
  MAX_MODEL_PROVIDER_PRIORITY,
  MAX_MODEL_PROVIDER_URL_CHARS,
  MIN_MODEL_PROVIDER_PRIORITY,
  type WorkspaceModelProvider,
} from "@/lib/model-providers";
import { formatProviderHealthStatus } from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceModelProvidersController } from "@/lib/use-workspace-model-providers";

type TierFilter = "ALL" | ModelProviderTier;

const TIER_TABS: ReadonlyArray<{ id: TierFilter; label: string }> = [
  { id: "ALL", label: "전체" },
  { id: "PAID", label: "유료 API" },
  { id: "FREE", label: "무료" },
  { id: "LOCAL", label: "로컬" },
];

const WIRE_PROTOCOL_OPTIONS: readonly WireProtocol[] = [
  "OPENAI_CHAT",
  "OPENAI_RESPONSES",
  "ANTHROPIC_MESSAGES",
  "GOOGLE_GEMINI",
  "OLLAMA_NATIVE",
];

export type ModelProvidersPanelProps = {
  controller: WorkspaceModelProvidersController;
  compact?: boolean;
};

export function ModelProvidersPanel({ controller, compact = false }: ModelProvidersPanelProps) {
  const {
    canManage,
    presets,
    providers,
    loadStatus,
    loadError,
    saving,
    togglingId,
    deletingId,
    testingMode,
    testResult,
    testError,
    feedback,
    reload,
    saveProvider,
    toggleProviderEnabled,
    removeProvider,
    runConnectionTest,
    clearFeedback,
  } = controller;

  const [tierFilter, setTierFilter] = useState<TierFilter>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [editingProviderId, setEditingProviderId] = useState<number | null>(null);

  const initialPreset = presets[0];
  const [providerKind, setProviderKind] = useState<ModelProviderKind>(
    initialPreset?.providerKind ?? "OPENAI",
  );
  const [wireProtocol, setWireProtocol] = useState<WireProtocol>(
    initialPreset?.wireProtocol ?? "OPENAI_RESPONSES",
  );
  const [displayName, setDisplayName] = useState(initialPreset?.displayName ?? "OpenAI");
  const [baseUrl, setBaseUrl] = useState(
    initialPreset?.defaultBaseUrl ?? "https://api.openai.com/v1",
  );
  const [apiKey, setApiKey] = useState("");
  const [defaultModel, setDefaultModel] = useState(initialPreset?.models[0]?.id ?? "gpt-4.1");
  const [priorityInput, setPriorityInput] = useState(String(DEFAULT_MODEL_PROVIDER_PRIORITY));

  const filteredPresets = useMemo(
    () => filterCatalogPresets(presets, tierFilter, searchQuery),
    [presets, searchQuery, tierFilter],
  );

  const activePreset = useMemo(
    () => findCatalogPreset(presets, providerKind),
    [presets, providerKind],
  );

  const suggestedModels = useMemo(() => {
    const seen = new Set<string>();
    const list: string[] = [];
    for (const model of activePreset?.models ?? []) {
      if (!seen.has(model.id)) {
        seen.add(model.id);
        list.push(model.id);
      }
    }
    for (const modelId of testResult?.discoveredModels ?? []) {
      if (!seen.has(modelId)) {
        seen.add(modelId);
        list.push(modelId);
      }
    }
    return list;
  }, [activePreset, testResult?.discoveredModels]);

  function applyPreset(preset: ModelCatalogPreset) {
    setEditingProviderId(null);
    setProviderKind(preset.providerKind);
    setWireProtocol(preset.wireProtocol);
    setDisplayName(preset.displayName);
    setBaseUrl(preset.defaultBaseUrl);
    setDefaultModel(preset.models[0]?.id ?? "");
    setApiKey("");
    clearFeedback();
  }

  function startEditProvider(provider: WorkspaceModelProvider) {
    setEditingProviderId(provider.providerId);
    setProviderKind(provider.providerKind);
    setWireProtocol(provider.wireProtocol);
    setDisplayName(provider.displayName);
    setBaseUrl(provider.baseUrl);
    setDefaultModel(provider.defaultModel ?? "");
    setPriorityInput(String(provider.priority));
    setApiKey("");
    clearFeedback();
  }

  function resetForm() {
    setEditingProviderId(null);
    if (initialPreset) {
      applyPreset(initialPreset);
    } else {
      setDisplayName("");
      setBaseUrl("");
      setDefaultModel("");
      setApiKey("");
    }
    setPriorityInput(String(DEFAULT_MODEL_PROVIDER_PRIORITY));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canManage) return;
    const parsedPriority = Number.parseInt(priorityInput, 10);
    const priority = Number.isFinite(parsedPriority)
      ? parsedPriority
      : DEFAULT_MODEL_PROVIDER_PRIORITY;
    const resolvedDefaultModel =
      defaultModel.trim() || activePreset?.models[0]?.id || testResult?.discoveredModels[0] || "";
    const discoveredEnabledModels = testResult?.discoveredModels?.length
      ? testResult.discoveredModels
      : undefined;

    const saved = await saveProvider(editingProviderId, {
      providerKind,
      wireProtocol,
      displayName,
      baseUrl,
      apiKey: apiKey.trim() || undefined,
      defaultModel: resolvedDefaultModel || null,
      ...(discoveredEnabledModels ? { enabledModels: discoveredEnabledModels } : {}),
      priority,
    });
    if (saved) {
      setApiKey("");
      if (editingProviderId !== null) {
        setEditingProviderId(null);
      }
    }
  }

  async function handleRunTest(mode: "models" | "completion") {
    if (!canManage) return;
    const result = await runConnectionTest({
      mode,
      providerId: editingProviderId,
      providerKind,
      wireProtocol,
      baseUrl,
      apiKey: apiKey.trim() || undefined,
      modelId: defaultModel.trim() || null,
    });
    if (result?.resolvedModel && !defaultModel.trim()) {
      setDefaultModel(result.resolvedModel);
    } else if (
      mode === "models" &&
      result &&
      result.discoveredModels.length > 0 &&
      !defaultModel.trim()
    ) {
      setDefaultModel(result.discoveredModels[0]);
    }
  }

  return (
    <div
      className={`ao-model-studio${compact ? " ao-model-studio-compact" : ""}`}
      aria-label="AI 모델 프로바이더 스튜디오"
    >
      {/* 1. Model Catalog Preset Browser */}
      <section className="ao-model-catalog-section" aria-label="모델 프로바이더 카탈로그">
        <div className="ao-model-studio-toolbar">
          <div className="ao-model-tier-tabs" role="tablist" aria-label="프로바이더 요금 분류">
            {TIER_TABS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={tierFilter === tab.id}
                className={`ao-model-tier-tab${tierFilter === tab.id ? " is-active" : ""}`}
                onClick={() => setTierFilter(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <label className="ao-model-search-box">
            <Search size={14} aria-hidden="true" />
            <input
              type="search"
              aria-label="프로바이더 및 모델 검색"
              placeholder="프로바이더·모델 ID 검색"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </label>
        </div>

        {filteredPresets.length === 0 ? (
          <div className="ao-inline-notice" role="status">
            <ListFilter size={14} aria-hidden="true" />
            <span>검색 조건에 맞는 모델 프로바이더 프리셋이 없습니다.</span>
          </div>
        ) : (
          <div className="ao-model-preset-grid">
            {filteredPresets.map((preset) => {
              const selected =
                editingProviderId === null && providerKind === preset.providerKind;
              return (
                <button
                  key={preset.providerKind}
                  type="button"
                  aria-pressed={selected}
                  className={`ao-model-preset-card${selected ? " is-selected" : ""}`}
                  onClick={() => applyPreset(preset)}
                >
                  <div className="ao-model-preset-head">
                    <strong>{preset.displayName}</strong>
                    <span className="role-badge">{providerTierLabel(preset.tier)}</span>
                  </div>
                  <div className="ao-model-preset-meta">
                    <span>{wireProtocolLabel(preset.wireProtocol)}</span>
                    <span>{preset.authRequired ? "API 키 필요" : "로컬/키 선택"}</span>
                  </div>
                  <div className="ao-model-preset-models">
                    {preset.models.slice(0, 2).map((model) => (
                      <code key={model.id}>{model.id}</code>
                    ))}
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </section>

      {/* 2. Failover Priority Chain */}
      <section className="ao-model-chain-section" aria-label="등록된 모델 프로바이더 체인">
        <div className="workspace-invite-management-toolbar">
          <strong>폴백 우선순위 체인 ({providers.length})</strong>
          <button
            type="button"
            className="secondary-action workspace-invite-refresh"
            onClick={() => void reload()}
            disabled={loadStatus === "loading"}
          >
            <RefreshCw
              size={13}
              className={loadStatus === "loading" ? "spin" : undefined}
              aria-hidden="true"
            />
            <span>목록 새로고침</span>
          </button>
        </div>

        {loadError ? (
          <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
            <AlertCircle size={14} aria-hidden="true" />
            <span>{loadError}</span>
          </div>
        ) : null}

        {providers.length === 0 ? (
          <div className="workspace-member-empty">
            <Cpu aria-hidden="true" />
            <strong>등록된 AI 모델 프로바이더가 없습니다</strong>
            <p>위 카탈로그에서 프리셋을 선택해 첫 프로바이더를 등록하세요.</p>
          </div>
        ) : (
          <div className="workspace-connection-list">
            {providers.map((provider) => {
              const isEditing = editingProviderId === provider.providerId;
              const isToggling = togglingId === provider.providerId;
              const isDeleting = deletingId === provider.providerId;
              return (
                <article
                  key={provider.providerId}
                  className={`ao-model-provider-row${isEditing ? " is-editing" : ""}`}
                >
                  <div className="ao-model-provider-main">
                    <div className="ao-model-provider-title">
                      <span className="role-badge" title="폴백 우선순위 (낮을수록 우선)">
                        우선순위 {provider.priority}
                      </span>
                      <strong>{provider.displayName}</strong>
                      <span className="role-badge">
                        {providerKindLabel(provider.providerKind)}
                      </span>
                      {provider.lastStatus ? (
                        <span
                          className={`role-badge ${
                            provider.lastStatus === "CONNECTED" || provider.lastStatus === "OK"
                              ? "ao-tone-ok"
                              : "ao-tone-warn"
                          }`}
                        >
                          {formatProviderHealthStatus(provider.lastStatus)}
                          {provider.lastLatencyMs !== null ? ` · ${provider.lastLatencyMs}ms` : ""}
                        </span>
                      ) : null}
                    </div>
                    <p className="ao-model-provider-sub">
                      <span>{wireProtocolLabel(provider.wireProtocol)}</span>
                      <span>·</span>
                      <span className="font-mono">{provider.defaultModel || "기본 모델 미지정"}</span>
                      <span>·</span>
                      <span className="font-mono">
                        {provider.maskedApiKey ?? (provider.hasApiKey ? "****" : "키 없음")}
                      </span>
                    </p>
                  </div>

                  <div className="ao-model-provider-actions">
                    <button
                      type="button"
                      className={`tb-btn${provider.enabled ? " tb-btn-primary" : ""}`}
                      aria-pressed={provider.enabled}
                      disabled={!canManage || isToggling || isDeleting}
                      onClick={() => void toggleProviderEnabled(provider)}
                    >
                      {isToggling ? (
                        <Loader2 size={12} className="spin" aria-hidden="true" />
                      ) : null}
                      <span>{provider.enabled ? "활성" : "비활성"}</span>
                    </button>
                    <button
                      type="button"
                      className="secondary-action workspace-github-row-button"
                      disabled={!canManage || isDeleting}
                      onClick={() => startEditProvider(provider)}
                    >
                      <Pencil size={12} aria-hidden="true" />
                      <span>편집</span>
                    </button>
                    <button
                      type="button"
                      className="danger-action workspace-github-row-button"
                      disabled={!canManage || isDeleting}
                      onClick={() => void removeProvider(provider)}
                    >
                      {isDeleting ? (
                        <Loader2 size={12} className="spin" aria-hidden="true" />
                      ) : (
                        <Trash2 size={12} aria-hidden="true" />
                      )}
                      <span>삭제</span>
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* 3. Create / Edit Form + Live Discovery & Completion Test */}
      <form className="workspace-connection-form" onSubmit={handleSubmit}>
        <div className="ao-surface-panel-header">
          <h3>
            {editingProviderId !== null
              ? `프로바이더 수정 (#${editingProviderId})`
              : "새 모델 프로바이더 등록"}
          </h3>
          {editingProviderId !== null ? (
            <button type="button" className="tb-btn" onClick={resetForm}>
              신규 등록으로 전환
            </button>
          ) : null}
        </div>

        <div className="workspace-gateway-form-grid">
          <label className="form-field">
            <span>
              <strong>프로바이더 종류</strong>
            </span>
            <select
              aria-label="프로바이더 종류"
              disabled={!canManage || saving}
              value={providerKind}
              onChange={(event) => {
                const nextKind = event.target.value as ModelProviderKind;
                setProviderKind(nextKind);
                const preset = findCatalogPreset(presets, nextKind);
                if (preset && editingProviderId === null) {
                  setWireProtocol(preset.wireProtocol);
                  setDisplayName(preset.displayName);
                  setBaseUrl(preset.defaultBaseUrl);
                  setDefaultModel(preset.models[0]?.id ?? "");
                }
              }}
            >
              {presets.map((preset) => (
                <option key={preset.providerKind} value={preset.providerKind}>
                  {preset.displayName} ({preset.providerKind})
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>
              <strong>와이어 프로토콜</strong>
            </span>
            <select
              aria-label="와이어 프로토콜"
              disabled={!canManage || saving}
              value={wireProtocol}
              onChange={(event) => setWireProtocol(event.target.value as WireProtocol)}
            >
              {WIRE_PROTOCOL_OPTIONS.map((protocol) => (
                <option key={protocol} value={protocol}>
                  {wireProtocolLabel(protocol)}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="workspace-gateway-form-grid">
          <label className="form-field">
            <span>
              <strong>표시 이름</strong>
              <small>
                {displayName.length}/{MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS}
              </small>
            </span>
            <input
              aria-label="표시 이름"
              maxLength={MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS}
              disabled={!canManage || saving}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>폴백 우선순위</strong>
              <small>0~1000</small>
            </span>
            <input
              type="number"
              aria-label="폴백 우선순위"
              min={MIN_MODEL_PROVIDER_PRIORITY}
              max={MAX_MODEL_PROVIDER_PRIORITY}
              disabled={!canManage || saving}
              value={priorityInput}
              onChange={(event) => setPriorityInput(event.target.value)}
            />
          </label>
        </div>

        <div className="workspace-gateway-form-grid">
          <label className="form-field">
            <span>
              <strong>Base URL</strong>
              <small>http/https</small>
            </span>
            <input
              aria-label="Base URL"
              maxLength={MAX_MODEL_PROVIDER_URL_CHARS}
              disabled={!canManage || saving}
              placeholder="https://api.openai.com/v1"
              value={baseUrl}
              onChange={(event) => setBaseUrl(event.target.value)}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>API 키</strong>
              <small>
                {editingProviderId !== null
                  ? "변경 시에만 입력"
                  : activePreset?.authRequired === false
                    ? "선택"
                    : "권장"}
              </small>
            </span>
            <input
              type="password"
              aria-label="API 키"
              maxLength={MAX_MODEL_PROVIDER_API_KEY_CHARS}
              disabled={!canManage || saving}
              placeholder={
                editingProviderId !== null ? "비워 두면 기존 암호화 키 유지" : "API 키 입력"
              }
              value={apiKey}
              onChange={(event) => setApiKey(event.target.value)}
            />
          </label>
        </div>

        <label className="form-field">
          <span>
            <strong>기본 모델</strong>
            <small>{defaultModel.length}/{MAX_MODEL_PROVIDER_MODEL_ID_CHARS}</small>
          </span>
          <input
            aria-label="기본 모델"
            maxLength={MAX_MODEL_PROVIDER_MODEL_ID_CHARS}
            disabled={!canManage || saving}
            placeholder="예: gpt-4.1"
            value={defaultModel}
            onChange={(event) => setDefaultModel(event.target.value)}
          />
          {suggestedModels.length > 0 ? (
            <div className="ao-dep-chip-row" aria-label="추천 모델 선택">
              {suggestedModels.slice(0, 8).map((modelId) => (
                <button
                  key={modelId}
                  type="button"
                  className="ao-dep-chip"
                  aria-pressed={defaultModel.trim() === modelId}
                  disabled={!canManage || saving}
                  onClick={() => setDefaultModel(modelId)}
                >
                  {modelId}
                </button>
              ))}
            </div>
          ) : null}
        </label>

        {feedback ? (
          <div
            className={`ao-inline-notice ${
              feedback.tone === "danger"
                ? "ao-inline-notice-danger"
                : "ao-inline-notice-success"
            }`}
            role={feedback.tone === "danger" ? "alert" : "status"}
          >
            {feedback.tone === "danger" ? (
              <AlertCircle size={14} aria-hidden="true" />
            ) : (
              <CheckCircle2 size={14} aria-hidden="true" />
            )}
            <span>{feedback.message}</span>
          </div>
        ) : null}

        {testError ? (
          <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
            <AlertCircle size={14} aria-hidden="true" />
            <span>{testError}</span>
          </div>
        ) : null}

        {testResult ? (
          <section
            className={`ao-model-test-result${testResult.ok ? " is-ok" : " is-fail"}`}
            aria-label="모델 연결 테스트 결과"
          >
            <div className="ao-model-test-head">
              <strong>
                {testResult.ok ? "연결 테스트 성공" : "연결 테스트 실패"} ({testResult.status})
              </strong>
              {testResult.latencyMs !== null ? (
                <span className="role-badge">{testResult.latencyMs}ms</span>
              ) : null}
            </div>
            <p className="m-0 text-[13px] text-[var(--ui-text)]">{testResult.message}</p>
            {testResult.resolvedModel || testResult.replyPreview ? (
              <div className="ao-model-completion-preview">
                {testResult.resolvedModel ? (
                  <div className="text-[12px] text-[var(--ui-text-secondary)]">
                    응답 모델: <code className="font-mono">{testResult.resolvedModel}</code>
                    {testResult.promptTokens !== null || testResult.completionTokens !== null
                      ? ` · 토큰 입력 ${testResult.promptTokens ?? 0} / 출력 ${testResult.completionTokens ?? 0}`
                      : ""}
                  </div>
                ) : null}
                {testResult.replyPreview ? (
                  <p className="m-0 mt-1 font-mono text-[12px] text-[var(--ui-text-strong)]">
                    {testResult.replyPreview}
                  </p>
                ) : null}
              </div>
            ) : null}
            {testResult.discoveredModels.length > 0 ? (
              <div className="mt-2">
                <span className="block text-[12px] text-[var(--ui-text-muted)] mb-1">
                  조회된 모델 ({testResult.discoveredModels.length}개)
                </span>
                <div className="ao-dep-chip-row">
                  {testResult.discoveredModels.slice(0, 12).map((modelId) => (
                    <button
                      key={modelId}
                      type="button"
                      className="ao-dep-chip"
                      aria-pressed={defaultModel.trim() === modelId}
                      onClick={() => setDefaultModel(modelId)}
                    >
                      {modelId}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </section>
        ) : null}

        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            className="secondary-action"
            disabled={!canManage || saving || testingMode !== null || !baseUrl.trim()}
            onClick={() => void handleRunTest("models")}
          >
            {testingMode === "models" ? (
              <Loader2 size={14} className="spin" aria-hidden="true" />
            ) : (
              <Sparkles size={14} aria-hidden="true" />
            )}
            <span>모델 목록 조회</span>
          </button>

          <button
            type="button"
            className="secondary-action"
            disabled={!canManage || saving || testingMode !== null || !baseUrl.trim()}
            onClick={() => void handleRunTest("completion")}
          >
            {testingMode === "completion" ? (
              <Loader2 size={14} className="spin" aria-hidden="true" />
            ) : (
              <Play size={14} aria-hidden="true" />
            )}
            <span>실호출 테스트</span>
          </button>

          <button
            type="submit"
            className="primary-action primary-action-active"
            disabled={!canManage || saving || !displayName.trim() || !baseUrl.trim()}
          >
            {saving ? (
              <Loader2 size={14} className="spin" aria-hidden="true" />
            ) : (
              <KeyRound size={14} aria-hidden="true" />
            )}
            <span>
              {saving
                ? "저장 중"
                : editingProviderId !== null
                  ? "프로바이더 수정 저장"
                  : "프로바이더 등록"}
            </span>
          </button>
        </div>
      </form>
    </div>
  );
}
