import { apiFetch } from "./api/client.ts";
import {
  BUILTIN_MODEL_CATALOG_PRESETS,
  isModelProviderKind,
  isWireProtocol,
  providerKindLabel,
  type ModelCatalogPreset,
  type ModelProviderKind,
  type WireProtocol,
} from "./model-catalog.ts";

export const MAX_MODEL_PROVIDERS = 100;
export const MAX_DISCOVERED_MODELS = 500;
export const MAX_ENABLED_MODELS = 100;
export const MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS = 100;
export const MAX_MODEL_PROVIDER_URL_CHARS = 512;
export const MAX_MODEL_PROVIDER_API_KEY_CHARS = 512;
export const MAX_MODEL_PROVIDER_MODEL_ID_CHARS = 160;
export const MIN_MODEL_PROVIDER_PRIORITY = 0;
export const MAX_MODEL_PROVIDER_PRIORITY = 1000;
export const DEFAULT_MODEL_PROVIDER_PRIORITY = 100;

export type WorkspaceModelProvider = {
  providerId: number;
  workspaceId: number;
  providerKind: ModelProviderKind;
  wireProtocol: WireProtocol;
  displayName: string;
  baseUrl: string;
  defaultModel: string | null;
  priority: number;
  enabled: boolean;
  maskedApiKey: string | null;
  hasApiKey: boolean;
  lastStatus: string | null;
  lastLatencyMs: number | null;
  lastError: string | null;
  lastCheckedAt: string | null;
  discoveredModels: string[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type CreateWorkspaceModelProviderInput = {
  providerKind: ModelProviderKind;
  wireProtocol: WireProtocol;
  displayName: string;
  baseUrl: string;
  apiKey?: string | null;
  defaultModel?: string | null;
  enabledModels?: string[];
  priority?: number;
  enabled?: boolean;
};

export type UpdateWorkspaceModelProviderInput = {
  providerKind?: ModelProviderKind;
  wireProtocol?: WireProtocol;
  displayName?: string;
  baseUrl?: string;
  apiKey?: string | null;
  defaultModel?: string | null;
  enabledModels?: string[];
  priority?: number;
  enabled?: boolean;
};

export type ModelProviderTestMode = "models" | "completion";

export type ModelProviderConnectionTestInput = {
  mode: ModelProviderTestMode;
  providerId?: number | null;
  providerKind: ModelProviderKind;
  wireProtocol: WireProtocol;
  baseUrl: string;
  apiKey?: string | null;
  modelId?: string | null;
};

export type ModelProviderConnectionTestResult = {
  ok: boolean;
  status: string;
  latencyMs: number | null;
  discoveredModels: string[];
  resolvedModel: string | null;
  replyPreview: string | null;
  promptTokens: number | null;
  completionTokens: number | null;
  message: string;
};

export function workspaceModelProvidersPath(workspaceId: number): string {
  return `/api/v1/workspaces/${workspaceId}/model-providers`;
}

export function workspaceModelProviderPath(workspaceId: number, providerId: number): string {
  return `${workspaceModelProvidersPath(workspaceId)}/${providerId}`;
}

export function workspaceModelProviderConnectionTestPath(workspaceId: number): string {
  return `${workspaceModelProvidersPath(workspaceId)}/connection-test`;
}

function hasControlChars(value: string): boolean {
  for (let i = 0; i < value.length; i += 1) {
    const code = value.charCodeAt(i);
    if (code < 32 || code === 127) {
      return true;
    }
  }
  return false;
}

export function validateModelProviderBaseUrl(rawUrl: string): string {
  if (typeof rawUrl !== "string") {
    throw new RangeError("Base URL을 입력해 주세요.");
  }
  const trimmed = rawUrl.trim();
  if (!trimmed) {
    throw new RangeError("Base URL을 입력해 주세요.");
  }
  if (trimmed.length > MAX_MODEL_PROVIDER_URL_CHARS) {
    throw new RangeError(
      `Base URL은 ${MAX_MODEL_PROVIDER_URL_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  if (hasControlChars(trimmed) || /\s/.test(trimmed)) {
    throw new RangeError("Base URL에 공백이나 제어 문자를 포함할 수 없습니다.");
  }

  let parsed: URL;
  try {
    parsed = new URL(trimmed);
  } catch {
    throw new RangeError("올바른 http:// 또는 https:// Base URL 형식이 아닙니다.");
  }

  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    throw new RangeError("Base URL은 http:// 또는 https:// 프로토콜만 허용됩니다.");
  }
  if (parsed.username || parsed.password) {
    throw new RangeError("Base URL에 인증 정보(username/password)를 포함할 수 없습니다.");
  }
  if (parsed.search) {
    throw new RangeError("Base URL에 쿼리 문자열(?)을 포함할 수 없습니다.");
  }
  if (parsed.hash) {
    throw new RangeError("Base URL에 해시(#) 프래그먼트를 포함할 수 없습니다.");
  }

  const normalizedPath = parsed.pathname.replace(/\/+$/, "");
  return `${parsed.origin}${normalizedPath}`;
}

function validateDisplayName(raw: string): string {
  if (typeof raw !== "string") {
    throw new RangeError("표시 이름을 입력해 주세요.");
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new RangeError("표시 이름을 입력해 주세요.");
  }
  if (trimmed.length > MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS) {
    throw new RangeError(
      `표시 이름은 ${MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  if (hasControlChars(trimmed)) {
    throw new RangeError("표시 이름에 제어 문자를 포함할 수 없습니다.");
  }
  return trimmed;
}

function validateOptionalApiKey(
  raw: string | null | undefined,
  required = false,
): string | undefined {
  if (raw === undefined || raw === null) {
    if (required) {
      throw new RangeError("API 키를 입력해 주세요.");
    }
    return undefined;
  }
  if (typeof raw !== "string") {
    throw new RangeError("API 키 형식이 올바르지 않습니다.");
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    if (required) {
      throw new RangeError("API 키를 입력해 주세요.");
    }
    return undefined;
  }
  if (trimmed.length > MAX_MODEL_PROVIDER_API_KEY_CHARS) {
    throw new RangeError(
      `API 키는 ${MAX_MODEL_PROVIDER_API_KEY_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  if (hasControlChars(trimmed) || /\s/.test(trimmed)) {
    throw new RangeError("API 키에 공백이나 제어 문자를 포함할 수 없습니다.");
  }
  return trimmed;
}

function validateOptionalModelId(raw: string | null | undefined): string | null | undefined {
  if (raw === undefined) return undefined;
  if (raw === null) return null;
  if (typeof raw !== "string") {
    throw new RangeError("모델 ID 형식이 올바르지 않습니다.");
  }
  const trimmed = raw.trim();
  if (!trimmed) return null;
  if (trimmed.length > MAX_MODEL_PROVIDER_MODEL_ID_CHARS) {
    throw new RangeError(
      `모델 ID는 ${MAX_MODEL_PROVIDER_MODEL_ID_CHARS}자를 초과할 수 없습니다.`,
    );
  }
  if (hasControlChars(trimmed)) {
    throw new RangeError("모델 ID에 제어 문자를 포함할 수 없습니다.");
  }
  return trimmed;
}

function validatePriority(raw: number | undefined): number {
  if (raw === undefined) {
    return DEFAULT_MODEL_PROVIDER_PRIORITY;
  }
  if (
    typeof raw !== "number" ||
    !Number.isInteger(raw) ||
    raw < MIN_MODEL_PROVIDER_PRIORITY ||
    raw > MAX_MODEL_PROVIDER_PRIORITY
  ) {
    throw new RangeError(
      `폴백 우선순위는 ${MIN_MODEL_PROVIDER_PRIORITY}~${MAX_MODEL_PROVIDER_PRIORITY} 사이 정수여야 합니다.`,
    );
  }
  return raw;
}

export function validateModelProviderInput(
  input: CreateWorkspaceModelProviderInput,
  options?: { requireApiKey?: boolean },
): CreateWorkspaceModelProviderInput {
  if (!isModelProviderKind(input.providerKind)) {
    throw new RangeError("유효하지 않은 프로바이더 종류입니다.");
  }
  if (!isWireProtocol(input.wireProtocol)) {
    throw new RangeError("유효하지 않은 와이어 프로토콜입니다.");
  }
  const displayName = validateDisplayName(input.displayName);
  const baseUrl = validateModelProviderBaseUrl(input.baseUrl);
  const apiKey = validateOptionalApiKey(input.apiKey, options?.requireApiKey === true);
  const defaultModel = validateOptionalModelId(input.defaultModel) ?? null;
  const enabledModels =
    input.enabledModels !== undefined
      ? normalizeStringList(input.enabledModels, MAX_ENABLED_MODELS)
      : undefined;
  const priority = validatePriority(input.priority);

  return {
    providerKind: input.providerKind,
    wireProtocol: input.wireProtocol,
    displayName,
    baseUrl,
    ...(apiKey !== undefined ? { apiKey } : {}),
    ...(defaultModel !== null ? { defaultModel } : {}),
    ...(enabledModels !== undefined ? { enabledModels } : {}),
    priority,
    enabled: input.enabled ?? true,
  };
}

export function validateModelProviderUpdateInput(
  input: UpdateWorkspaceModelProviderInput,
): UpdateWorkspaceModelProviderInput {
  const result: UpdateWorkspaceModelProviderInput = {};
  if (input.providerKind !== undefined) {
    if (!isModelProviderKind(input.providerKind)) {
      throw new RangeError("유효하지 않은 프로바이더 종류입니다.");
    }
    result.providerKind = input.providerKind;
  }
  if (input.wireProtocol !== undefined) {
    if (!isWireProtocol(input.wireProtocol)) {
      throw new RangeError("유효하지 않은 와이어 프로토콜입니다.");
    }
    result.wireProtocol = input.wireProtocol;
  }
  if (input.displayName !== undefined) {
    result.displayName = validateDisplayName(input.displayName);
  }
  if (input.baseUrl !== undefined) {
    result.baseUrl = validateModelProviderBaseUrl(input.baseUrl);
  }
  if (input.apiKey !== undefined) {
    const validatedKey = validateOptionalApiKey(input.apiKey, false);
    if (validatedKey !== undefined) {
      result.apiKey = validatedKey;
    }
  }
  if (input.defaultModel !== undefined) {
    result.defaultModel = validateOptionalModelId(input.defaultModel) ?? null;
  }
  if (input.enabledModels !== undefined) {
    result.enabledModels = normalizeStringList(input.enabledModels, MAX_ENABLED_MODELS);
  }
  if (input.priority !== undefined) {
    result.priority = validatePriority(input.priority);
  }
  if (input.enabled !== undefined) {
    result.enabled = Boolean(input.enabled);
  }
  return result;
}

export function validateModelProviderTestInput(
  input: ModelProviderConnectionTestInput,
): ModelProviderConnectionTestInput {
  if (input.mode !== "models" && input.mode !== "completion") {
    throw new RangeError("테스트 모드는 models 또는 completion이어야 합니다.");
  }
  if (!isModelProviderKind(input.providerKind)) {
    throw new RangeError("유효하지 않은 프로바이더 종류입니다.");
  }
  if (!isWireProtocol(input.wireProtocol)) {
    throw new RangeError("유효하지 않은 와이어 프로토콜입니다.");
  }
  const baseUrl = validateModelProviderBaseUrl(input.baseUrl);
  const apiKey = validateOptionalApiKey(input.apiKey, false);
  const modelId = validateOptionalModelId(input.modelId) ?? null;

  return {
    mode: input.mode,
    ...(typeof input.providerId === "number" && input.providerId > 0
      ? { providerId: input.providerId }
      : {}),
    providerKind: input.providerKind,
    wireProtocol: input.wireProtocol,
    baseUrl,
    ...(apiKey !== undefined ? { apiKey } : {}),
    ...(modelId !== null ? { modelId } : {}),
  };
}

function normalizeStringList(raw: unknown, maxItems: number): string[] {
  if (!Array.isArray(raw)) return [];
  const items: string[] = [];
  const limit = Math.min(raw.length, maxItems);
  for (let i = 0; i < limit; i += 1) {
    const entry = raw[i];
    if (typeof entry === "string") {
      const trimmed = entry.trim();
      if (trimmed && trimmed.length <= MAX_MODEL_PROVIDER_MODEL_ID_CHARS) {
        items.push(trimmed);
      }
    }
  }
  return items;
}

export function normalizeWorkspaceModelProvider(
  payload: unknown,
): WorkspaceModelProvider | null {
  if (!payload || typeof payload !== "object") return null;
  const record = payload as Record<string, unknown>;
  const rawId = record.providerId ?? record.id;
  if (typeof rawId !== "number" || !Number.isFinite(rawId) || rawId <= 0) {
    return null;
  }

  const providerKind: ModelProviderKind = isModelProviderKind(record.providerKind)
    ? record.providerKind
    : "CUSTOM_OPENAI";
  const wireProtocol: WireProtocol = isWireProtocol(record.wireProtocol)
    ? record.wireProtocol
    : "OPENAI_CHAT";
  const displayName =
    typeof record.displayName === "string" && record.displayName.trim()
      ? record.displayName.trim().slice(0, MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS)
      : providerKindLabel(providerKind);
  const baseUrl =
    typeof record.baseUrl === "string" ? record.baseUrl.trim().slice(0, MAX_MODEL_PROVIDER_URL_CHARS) : "";
  const rawPriority =
    typeof record.priorityOrder === "number" && Number.isFinite(record.priorityOrder)
      ? record.priorityOrder
      : typeof record.priority === "number" && Number.isFinite(record.priority)
        ? record.priority
        : null;
  const priority =
    rawPriority !== null
      ? Math.max(MIN_MODEL_PROVIDER_PRIORITY, Math.min(MAX_MODEL_PROVIDER_PRIORITY, Math.trunc(rawPriority)))
      : DEFAULT_MODEL_PROVIDER_PRIORITY;
  const maskedApiKey =
    typeof record.maskedApiKey === "string" && record.maskedApiKey.trim()
      ? record.maskedApiKey.trim()
      : null;

  return {
    providerId: Math.trunc(rawId),
    workspaceId:
      typeof record.workspaceId === "number" && Number.isFinite(record.workspaceId)
        ? Math.trunc(record.workspaceId)
        : 0,
    providerKind,
    wireProtocol,
    displayName,
    baseUrl,
    defaultModel:
      typeof record.defaultModel === "string" && record.defaultModel.trim()
        ? record.defaultModel.trim()
        : null,
    priority,
    enabled: record.enabled !== false,
    maskedApiKey,
    hasApiKey: record.hasApiKey === true || maskedApiKey !== null,
    lastStatus: typeof record.lastStatus === "string" ? record.lastStatus : null,
    lastLatencyMs:
      typeof record.lastLatencyMs === "number" && Number.isFinite(record.lastLatencyMs)
        ? Math.max(0, Math.round(record.lastLatencyMs))
        : null,
    lastError: typeof record.lastError === "string" ? record.lastError : null,
    lastCheckedAt: typeof record.lastCheckedAt === "string" ? record.lastCheckedAt : null,
    discoveredModels: normalizeStringList(
      record.enabledModels ?? record.discoveredModels,
      MAX_DISCOVERED_MODELS,
    ),
    createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
    updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : null,
  };
}

export function normalizeWorkspaceModelProviders(
  payload: unknown,
): WorkspaceModelProvider[] {
  let rawList: unknown[] | null = null;
  if (Array.isArray(payload)) {
    rawList = payload;
  } else if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.providers)) rawList = record.providers;
    else if (Array.isArray(record.content)) rawList = record.content;
    else if (Array.isArray(record.items)) rawList = record.items;
  }
  if (!rawList) return [];

  const list: WorkspaceModelProvider[] = [];
  const limit = Math.min(rawList.length, MAX_MODEL_PROVIDERS);
  for (let i = 0; i < limit; i += 1) {
    const parsed = normalizeWorkspaceModelProvider(rawList[i]);
    if (parsed) {
      list.push(parsed);
    }
  }
  return sortModelProvidersByPriority(list);
}

export function normalizeModelProviderConnectionTestResult(
  payload: unknown,
): ModelProviderConnectionTestResult {
  if (!payload || typeof payload !== "object") {
    return {
      ok: false,
      status: "FAILED",
      latencyMs: null,
      discoveredModels: [],
      resolvedModel: null,
      replyPreview: null,
      promptTokens: null,
      completionTokens: null,
      message: "응답 형식이 올바르지 않습니다.",
    };
  }
  const record = payload as Record<string, unknown>;
  return {
    ok: record.ok === true,
    status: typeof record.status === "string" ? record.status : record.ok ? "CONNECTED" : "FAILED",
    latencyMs:
      typeof record.latencyMs === "number" && Number.isFinite(record.latencyMs)
        ? Math.max(0, Math.round(record.latencyMs))
        : null,
    discoveredModels: normalizeStringList(record.discoveredModels, MAX_DISCOVERED_MODELS),
    resolvedModel: typeof record.resolvedModel === "string" ? record.resolvedModel : null,
    replyPreview: typeof record.replyPreview === "string" ? record.replyPreview : null,
    promptTokens:
      typeof record.promptTokens === "number" && Number.isFinite(record.promptTokens)
        ? Math.max(0, Math.trunc(record.promptTokens))
        : null,
    completionTokens:
      typeof record.completionTokens === "number" && Number.isFinite(record.completionTokens)
        ? Math.max(0, Math.trunc(record.completionTokens))
        : null,
    message:
      typeof record.message === "string" && record.message.trim()
        ? record.message
        : record.ok
          ? "연결 테스트에 성공했습니다."
          : "연결 테스트에 실패했습니다.",
  };
}

export function sortModelProvidersByPriority(
  providers: readonly WorkspaceModelProvider[],
): WorkspaceModelProvider[] {
  return [...providers].sort((a, b) => {
    if (a.priority !== b.priority) {
      return a.priority - b.priority;
    }
    if (a.enabled !== b.enabled) {
      return a.enabled ? -1 : 1;
    }
    const nameOrder = a.displayName.localeCompare(b.displayName, "ko-KR");
    if (nameOrder !== 0) {
      return nameOrder;
    }
    return a.providerId - b.providerId;
  });
}

export async function fetchWorkspaceModelProviders(
  accessToken: string,
  workspaceId: number,
): Promise<WorkspaceModelProvider[]> {
  const payload = await apiFetch<unknown>(workspaceModelProvidersPath(workspaceId), {
    accessToken,
  });
  return normalizeWorkspaceModelProviders(payload);
}

export async function createWorkspaceModelProvider(
  accessToken: string,
  workspaceId: number,
  input: CreateWorkspaceModelProviderInput,
): Promise<WorkspaceModelProvider> {
  const validated = validateModelProviderInput(input);
  const presetFallbackModel =
    BUILTIN_MODEL_CATALOG_PRESETS.find((preset) => preset.providerKind === validated.providerKind)
      ?.models[0]?.id ?? null;
  const resolvedDefaultModel =
    validated.defaultModel ?? validated.enabledModels?.[0] ?? presetFallbackModel;
  if (!resolvedDefaultModel) {
    throw new RangeError("기본 모델 ID를 입력해 주세요.");
  }

  const requestBody: Record<string, unknown> = {
    providerKind: validated.providerKind,
    wireProtocol: validated.wireProtocol,
    displayName: validated.displayName,
    baseUrl: validated.baseUrl,
    defaultModel: resolvedDefaultModel,
    priorityOrder: validated.priority ?? DEFAULT_MODEL_PROVIDER_PRIORITY,
    enabled: validated.enabled ?? true,
  };
  if (validated.apiKey !== undefined) {
    requestBody.apiKey = validated.apiKey;
  }
  if (validated.enabledModels !== undefined) {
    requestBody.enabledModels = validated.enabledModels;
  }

  const payload = await apiFetch<unknown>(workspaceModelProvidersPath(workspaceId), {
    method: "POST",
    accessToken,
    json: requestBody,
  });
  const parsed = normalizeWorkspaceModelProvider(payload);
  if (!parsed) {
    throw new Error("생성된 프로바이더 응답이 올바르지 않습니다.");
  }
  return parsed;
}

export async function updateWorkspaceModelProvider(
  accessToken: string,
  workspaceId: number,
  providerId: number,
  input: UpdateWorkspaceModelProviderInput,
): Promise<WorkspaceModelProvider> {
  const validated = validateModelProviderUpdateInput(input);
  const requestBody: Record<string, unknown> = {};
  if (validated.displayName !== undefined) requestBody.displayName = validated.displayName;
  if (validated.wireProtocol !== undefined) requestBody.wireProtocol = validated.wireProtocol;
  if (validated.baseUrl !== undefined) requestBody.baseUrl = validated.baseUrl;
  if (validated.apiKey !== undefined) requestBody.apiKey = validated.apiKey;
  if (validated.defaultModel !== undefined) requestBody.defaultModel = validated.defaultModel;
  if (validated.enabledModels !== undefined) requestBody.enabledModels = validated.enabledModels;
  if (validated.priority !== undefined) requestBody.priorityOrder = validated.priority;
  if (validated.enabled !== undefined) requestBody.enabled = validated.enabled;

  const payload = await apiFetch<unknown>(workspaceModelProviderPath(workspaceId, providerId), {
    method: "PATCH",
    accessToken,
    json: requestBody,
  });
  const parsed = normalizeWorkspaceModelProvider(payload);
  if (!parsed) {
    throw new Error("수정된 프로바이더 응답이 올바르지 않습니다.");
  }
  return parsed;
}

export async function deleteWorkspaceModelProvider(
  accessToken: string,
  workspaceId: number,
  providerId: number,
): Promise<void> {
  await apiFetch<void>(workspaceModelProviderPath(workspaceId, providerId), {
    method: "DELETE",
    accessToken,
  });
}

export async function testWorkspaceModelProviderConnection(
  accessToken: string,
  workspaceId: number,
  input: ModelProviderConnectionTestInput,
): Promise<ModelProviderConnectionTestResult> {
  const validated = validateModelProviderTestInput(input);
  const payload = await apiFetch<unknown>(workspaceModelProviderConnectionTestPath(workspaceId), {
    method: "POST",
    accessToken,
    json: validated,
  });
  return normalizeModelProviderConnectionTestResult(payload);
}

export function suggestModelsForProvider(
  providers: readonly WorkspaceModelProvider[],
  selectedProviderId: number | null | undefined,
  presets: readonly ModelCatalogPreset[] = BUILTIN_MODEL_CATALOG_PRESETS,
): string[] {
  const seen = new Set<string>();
  const suggestions: string[] = [];
  const push = (modelId: string | null | undefined) => {
    const trimmed = modelId?.trim();
    if (!trimmed || seen.has(trimmed)) return;
    seen.add(trimmed);
    suggestions.push(trimmed);
  };

  const selected =
    typeof selectedProviderId === "number" && selectedProviderId > 0
      ? providers.find((item) => item.providerId === selectedProviderId)
      : undefined;

  if (selected) {
    push(selected.defaultModel);
    for (const model of selected.discoveredModels) push(model);
    const preset = presets.find((item) => item.providerKind === selected.providerKind);
    for (const model of preset?.models ?? []) push(model.id);
    return suggestions.slice(0, 24);
  }

  for (const provider of providers) {
    if (!provider.enabled) continue;
    push(provider.defaultModel);
    for (const model of provider.discoveredModels) push(model);
  }
  for (const preset of presets) {
    for (const model of preset.models) push(model.id);
  }
  return suggestions.slice(0, 24);
}

