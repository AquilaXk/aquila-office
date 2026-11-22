import { apiFetch } from "./api/client.ts";
import { searchBitapItems } from "./bitap-fuzzy-search.ts";

export const MODEL_CATALOG_PATH = "/api/v1/model-catalog";
export const MAX_CATALOG_PRESETS = 64;
export const MAX_CATALOG_MODELS_PER_PRESET = 256;

export type ModelProviderKind =
  | "OPENAI"
  | "ANTHROPIC"
  | "GOOGLE"
  | "OPENROUTER"
  | "DEEPSEEK"
  | "XAI"
  | "GROQ"
  | "OLLAMA"
  | "CUSTOM_OPENAI";

export type WireProtocol =
  | "OPENAI_CHAT"
  | "OPENAI_RESPONSES"
  | "ANTHROPIC_MESSAGES"
  | "GOOGLE_GEMINI"
  | "OLLAMA_NATIVE";

export type ModelProviderTier = "PAID" | "FREE" | "LOCAL";

export type CatalogModelEntry = {
  id: string;
  displayName: string;
  contextWindow: number;
  maxOutputTokens: number;
  supportsReasoning: boolean;
  supportsTools: boolean;
  supportsVision: boolean;
};

export type ModelCatalogPreset = {
  providerKind: ModelProviderKind;
  displayName: string;
  defaultBaseUrl: string;
  wireProtocol: WireProtocol;
  tier: ModelProviderTier;
  authRequired: boolean;
  models: CatalogModelEntry[];
};

export type ModelCatalogResponse = {
  presets: ModelCatalogPreset[];
};

export const VALID_PROVIDER_KINDS: ReadonlySet<ModelProviderKind> = new Set([
  "OPENAI",
  "ANTHROPIC",
  "GOOGLE",
  "OPENROUTER",
  "DEEPSEEK",
  "XAI",
  "GROQ",
  "OLLAMA",
  "CUSTOM_OPENAI",
]);

export const VALID_WIRE_PROTOCOLS: ReadonlySet<WireProtocol> = new Set([
  "OPENAI_CHAT",
  "OPENAI_RESPONSES",
  "ANTHROPIC_MESSAGES",
  "GOOGLE_GEMINI",
  "OLLAMA_NATIVE",
]);

export const VALID_PROVIDER_TIERS: ReadonlySet<ModelProviderTier> = new Set([
  "PAID",
  "FREE",
  "LOCAL",
]);

export const BUILTIN_MODEL_CATALOG_PRESETS: readonly ModelCatalogPreset[] = [
  {
    providerKind: "OPENAI",
    displayName: "OpenAI",
    defaultBaseUrl: "https://api.openai.com/v1",
    wireProtocol: "OPENAI_RESPONSES",
    tier: "PAID",
    authRequired: true,
    models: [
      {
        id: "gpt-4.1",
        displayName: "GPT-4.1",
        contextWindow: 1_047_576,
        maxOutputTokens: 32_768,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: true,
      },
      {
        id: "o3-mini",
        displayName: "o3-mini",
        contextWindow: 200_000,
        maxOutputTokens: 100_000,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: false,
      },
      {
        id: "gpt-4o",
        displayName: "GPT-4o",
        contextWindow: 128_000,
        maxOutputTokens: 16_384,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: true,
      },
    ],
  },
  {
    providerKind: "ANTHROPIC",
    displayName: "Anthropic",
    defaultBaseUrl: "https://api.anthropic.com/v1",
    wireProtocol: "ANTHROPIC_MESSAGES",
    tier: "PAID",
    authRequired: true,
    models: [
      {
        id: "claude-sonnet-4-20250514",
        displayName: "Sonnet 4",
        contextWindow: 200_000,
        maxOutputTokens: 64_000,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: true,
      },
      {
        id: "claude-3-7-sonnet-20250219",
        displayName: "Sonnet 3.7",
        contextWindow: 200_000,
        maxOutputTokens: 64_000,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: true,
      },
      {
        id: "claude-3-5-haiku-20241022",
        displayName: "Haiku 3.5",
        contextWindow: 200_000,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: true,
      },
    ],
  },
  {
    providerKind: "GOOGLE",
    displayName: "Google AI Studio",
    defaultBaseUrl: "https://generativelanguage.googleapis.com/v1beta",
    wireProtocol: "GOOGLE_GEMINI",
    tier: "PAID",
    authRequired: true,
    models: [
      {
        id: "gemini-2.5-pro",
        displayName: "2.5 Pro",
        contextWindow: 1_048_576,
        maxOutputTokens: 65_536,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: true,
      },
      {
        id: "gemini-2.5-flash",
        displayName: "2.5 Flash",
        contextWindow: 1_048_576,
        maxOutputTokens: 65_536,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: true,
      },
    ],
  },
  {
    providerKind: "DEEPSEEK",
    displayName: "DeepSeek",
    defaultBaseUrl: "https://api.deepseek.com/v1",
    wireProtocol: "OPENAI_CHAT",
    tier: "PAID",
    authRequired: true,
    models: [
      {
        id: "deepseek-chat",
        displayName: "DeepSeek V3",
        contextWindow: 128_000,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
      {
        id: "deepseek-reasoner",
        displayName: "DeepSeek R1",
        contextWindow: 128_000,
        maxOutputTokens: 8_192,
        supportsReasoning: true,
        supportsTools: false,
        supportsVision: false,
      },
    ],
  },
  {
    providerKind: "XAI",
    displayName: "xAI",
    defaultBaseUrl: "https://api.x.ai/v1",
    wireProtocol: "OPENAI_CHAT",
    tier: "PAID",
    authRequired: true,
    models: [
      {
        id: "grok-3",
        displayName: "Grok 3",
        contextWindow: 131_072,
        maxOutputTokens: 16_384,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: false,
      },
      {
        id: "grok-3-mini",
        displayName: "Grok 3 Mini",
        contextWindow: 131_072,
        maxOutputTokens: 16_384,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: false,
      },
    ],
  },
  {
    providerKind: "OPENROUTER",
    displayName: "OpenRouter",
    defaultBaseUrl: "https://openrouter.ai/api/v1",
    wireProtocol: "OPENAI_CHAT",
    tier: "FREE",
    authRequired: true,
    models: [
      {
        id: "google/gemini-2.5-flash:free",
        displayName: "2.5 Flash (Free)",
        contextWindow: 1_048_576,
        maxOutputTokens: 65_536,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: true,
      },
      {
        id: "meta-llama/llama-3.3-70b-instruct:free",
        displayName: "Llama 3.3 70B (Free)",
        contextWindow: 131_072,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
    ],
  },
  {
    providerKind: "GROQ",
    displayName: "Groq",
    defaultBaseUrl: "https://api.groq.com/openai/v1",
    wireProtocol: "OPENAI_CHAT",
    tier: "FREE",
    authRequired: true,
    models: [
      {
        id: "llama-3.3-70b-versatile",
        displayName: "Llama 3.3 70B",
        contextWindow: 128_000,
        maxOutputTokens: 32_768,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
      {
        id: "qwen-qwq-32b",
        displayName: "Qwen QwQ 32B",
        contextWindow: 128_000,
        maxOutputTokens: 16_384,
        supportsReasoning: true,
        supportsTools: true,
        supportsVision: false,
      },
    ],
  },
  {
    providerKind: "OLLAMA",
    displayName: "Ollama (로컬)",
    defaultBaseUrl: "http://127.0.0.1:11434",
    wireProtocol: "OLLAMA_NATIVE",
    tier: "LOCAL",
    authRequired: false,
    models: [
      {
        id: "qwen2.5-coder:14b",
        displayName: "Qwen 2.5 Coder 14B",
        contextWindow: 32_768,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
      {
        id: "llama3.2:latest",
        displayName: "Llama 3.2",
        contextWindow: 128_000,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
    ],
  },
  {
    providerKind: "CUSTOM_OPENAI",
    displayName: "커스텀 호환 API",
    defaultBaseUrl: "http://127.0.0.1:8000/v1",
    wireProtocol: "OPENAI_CHAT",
    tier: "LOCAL",
    authRequired: false,
    models: [
      {
        id: "custom-model",
        displayName: "사용자 지정 모델",
        contextWindow: 128_000,
        maxOutputTokens: 8_192,
        supportsReasoning: false,
        supportsTools: true,
        supportsVision: false,
      },
    ],
  },
];

export const BUILTIN_MODEL_CATALOG: ModelCatalogResponse = {
  presets: [...BUILTIN_MODEL_CATALOG_PRESETS],
};

export function isModelProviderKind(value: unknown): value is ModelProviderKind {
  return typeof value === "string" && VALID_PROVIDER_KINDS.has(value as ModelProviderKind);
}

export function isWireProtocol(value: unknown): value is WireProtocol {
  return typeof value === "string" && VALID_WIRE_PROTOCOLS.has(value as WireProtocol);
}

export function isModelProviderTier(value: unknown): value is ModelProviderTier {
  return typeof value === "string" && VALID_PROVIDER_TIERS.has(value as ModelProviderTier);
}

function normalizeHttpBaseUrl(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (!trimmed || trimmed.length > 512) return null;
  try {
    const parsed = new URL(trimmed);
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") return null;
    if (parsed.username || parsed.password) return null;
    const normalizedPath = parsed.pathname.replace(/\/+$/, "");
    return `${parsed.origin}${normalizedPath}${parsed.search}`;
  } catch {
    return null;
  }
}

function normalizeCatalogModelEntry(raw: unknown): CatalogModelEntry | null {
  if (!raw || typeof raw !== "object") return null;
  const candidate = raw as Partial<CatalogModelEntry>;
  const id = typeof candidate.id === "string" ? candidate.id.trim() : "";
  if (!id || id.length > 160) return null;
  const displayName =
    typeof candidate.displayName === "string" && candidate.displayName.trim()
      ? candidate.displayName.trim().slice(0, 160)
      : id;
  const contextWindow =
    typeof candidate.contextWindow === "number" && Number.isFinite(candidate.contextWindow) && candidate.contextWindow > 0
      ? Math.trunc(candidate.contextWindow)
      : 128_000;
  const maxOutputTokens =
    typeof candidate.maxOutputTokens === "number" && Number.isFinite(candidate.maxOutputTokens) && candidate.maxOutputTokens > 0
      ? Math.trunc(candidate.maxOutputTokens)
      : 8_192;

  return {
    id,
    displayName,
    contextWindow,
    maxOutputTokens,
    supportsReasoning: Boolean(candidate.supportsReasoning),
    supportsTools: candidate.supportsTools !== false,
    supportsVision: Boolean(candidate.supportsVision),
  };
}

export function normalizeModelCatalogResponse(payload: unknown): ModelCatalogResponse {
  if (!payload || typeof payload !== "object") {
    return BUILTIN_MODEL_CATALOG;
  }
  const rawPresets = (payload as { presets?: unknown }).presets;
  if (!Array.isArray(rawPresets) || rawPresets.length === 0) {
    return BUILTIN_MODEL_CATALOG;
  }

  const normalized: ModelCatalogPreset[] = [];
  const limit = Math.min(rawPresets.length, MAX_CATALOG_PRESETS);
  for (let i = 0; i < limit; i += 1) {
    const item = rawPresets[i];
    if (!item || typeof item !== "object") continue;
    const record = item as Partial<ModelCatalogPreset>;
    if (!isModelProviderKind(record.providerKind) || !isWireProtocol(record.wireProtocol)) {
      continue;
    }
    const baseUrl = normalizeHttpBaseUrl(record.defaultBaseUrl);
    if (!baseUrl) continue;
    const displayName =
      typeof record.displayName === "string" && record.displayName.trim()
        ? record.displayName.trim().slice(0, 100)
        : providerKindLabel(record.providerKind);
    const tier: ModelProviderTier = isModelProviderTier(record.tier) ? record.tier : "PAID";
    const rawModels = Array.isArray(record.models) ? record.models : [];
    const models: CatalogModelEntry[] = [];
    const modelLimit = Math.min(rawModels.length, MAX_CATALOG_MODELS_PER_PRESET);
    for (let m = 0; m < modelLimit; m += 1) {
      const parsedModel = normalizeCatalogModelEntry(rawModels[m]);
      if (parsedModel) {
        models.push(parsedModel);
      }
    }
    normalized.push({
      providerKind: record.providerKind,
      displayName,
      defaultBaseUrl: baseUrl,
      wireProtocol: record.wireProtocol,
      tier,
      authRequired: record.authRequired !== false,
      models,
    });
  }

  return normalized.length > 0 ? { presets: normalized } : BUILTIN_MODEL_CATALOG;
}

export function filterCatalogPresets(
  presets: readonly ModelCatalogPreset[],
  tier: "ALL" | ModelProviderTier,
  query: string,
): ModelCatalogPreset[] {
  const byTier = tier === "ALL" ? [...presets] : presets.filter((preset) => preset.tier === tier);
  const trimmed = query.trim();
  if (!trimmed) {
    return byTier;
  }
  return searchBitapItems(
    byTier,
    trimmed,
    (preset) => {
      const modelIds = preset.models.map((m) => `${m.id} ${m.displayName}`).join(" ");
      return `${preset.displayName} ${preset.providerKind} ${preset.wireProtocol} ${modelIds}`;
    },
    { maxDistance: 1 },
  ).map((hit) => hit.item);
}

export function findCatalogPreset(
  presets: readonly ModelCatalogPreset[],
  kind: ModelProviderKind,
): ModelCatalogPreset | undefined {
  return presets.find((preset) => preset.providerKind === kind);
}

export function findCatalogModel(
  modelId: string | null | undefined,
  presets: readonly ModelCatalogPreset[] = BUILTIN_MODEL_CATALOG_PRESETS,
  preferredKind?: ModelProviderKind | null,
): CatalogModelEntry | undefined {
  const normalized = (modelId ?? "").trim().toLowerCase();
  if (!normalized) {
    return undefined;
  }
  const searchOrder = preferredKind
    ? [
        ...presets.filter((preset) => preset.providerKind === preferredKind),
        ...presets.filter((preset) => preset.providerKind !== preferredKind),
        ...BUILTIN_MODEL_CATALOG_PRESETS,
      ]
    : [...presets, ...BUILTIN_MODEL_CATALOG_PRESETS];

  for (const preset of searchOrder) {
    for (const model of preset.models) {
      if (model.id.toLowerCase() === normalized) {
        return model;
      }
    }
  }
  return undefined;
}

export function formatTokenCountShort(tokens: number | null | undefined): string {
  if (typeof tokens !== "number" || !Number.isFinite(tokens) || tokens < 0) {
    return "-";
  }
  const whole = Math.round(tokens);
  if (whole >= 1_000_000) {
    return `${(whole / 1_000_000).toFixed(1).replace(/\.0$/, "")}M`;
  }
  if (whole >= 1_000) {
    return `${Math.round(whole / 1_000)}K`;
  }
  return String(whole);
}

export function providerKindLabel(kind: ModelProviderKind | string): string {
  switch (kind) {
    case "OPENAI":
      return "OpenAI";
    case "ANTHROPIC":
      return "Anthropic";
    case "GOOGLE":
      return "Google Gemini";
    case "OPENROUTER":
      return "OpenRouter";
    case "DEEPSEEK":
      return "DeepSeek";
    case "XAI":
      return "xAI";
    case "GROQ":
      return "Groq";
    case "OLLAMA":
      return "Ollama";
    case "CUSTOM_OPENAI":
      return "커스텀 호환 API";
    default:
      return String(kind);
  }
}

export function wireProtocolLabel(protocol: WireProtocol | string): string {
  switch (protocol) {
    case "OPENAI_CHAT":
      return "Chat Completions";
    case "OPENAI_RESPONSES":
      return "Responses API";
    case "ANTHROPIC_MESSAGES":
      return "Messages API";
    case "GOOGLE_GEMINI":
      return "Google AI API";
    case "OLLAMA_NATIVE":
      return "Ollama Native";
    default:
      return String(protocol);
  }
}

export function providerTierLabel(tier: ModelProviderTier | string): string {
  switch (tier) {
    case "PAID":
      return "유료 API";
    case "FREE":
      return "무료";
    case "LOCAL":
      return "로컬";
    default:
      return String(tier);
  }
}

export async function fetchModelCatalog(accessToken: string): Promise<ModelCatalogResponse> {
  const payload = await apiFetch<unknown>(MODEL_CATALOG_PATH, {
    accessToken,
  });
  return normalizeModelCatalogResponse(payload);
}
