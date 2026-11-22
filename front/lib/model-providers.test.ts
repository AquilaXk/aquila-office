import assert from "node:assert/strict";
import test from "node:test";
import {
  BUILTIN_MODEL_CATALOG_PRESETS,
  filterCatalogPresets,
  findCatalogModel,
  formatTokenCountShort,
  normalizeModelCatalogResponse,
} from "./model-catalog.ts";
import {
  MAX_MODEL_PROVIDER_API_KEY_CHARS,
  MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS,
  MAX_MODEL_PROVIDER_PRIORITY,
  MAX_MODEL_PROVIDER_URL_CHARS,
  computeReorderedProviderPriorities,
  createWorkspaceModelProvider,
  deleteWorkspaceModelProvider,
  fetchWorkspaceModelProviders,
  fetchWorkspaceModelTelemetry,
  normalizeWorkspaceModelProviders,
  normalizeWorkspaceModelTelemetry,
  sortModelProvidersByPriority,
  testWorkspaceModelProviderConnection,
  updateWorkspaceModelProvider,
  validateModelProviderInput,
  validateModelProviderTestInput,
  workspaceModelProviderTelemetryPath,
  type WorkspaceModelProvider,
} from "./model-providers.ts";
import {
  type CreateWorkspaceAgentInput,
  type UpdateWorkspaceAgentInput,
  type WorkspaceAgent,
} from "./agents-utils.ts";

test("builtin model catalog covers all 9 required providers across PAID/FREE/LOCAL tiers", () => {
  const kinds = new Set(BUILTIN_MODEL_CATALOG_PRESETS.map((preset) => preset.providerKind));
  for (const expected of [
    "OPENAI",
    "ANTHROPIC",
    "GOOGLE",
    "OPENROUTER",
    "DEEPSEEK",
    "XAI",
    "GROQ",
    "OLLAMA",
    "CUSTOM_OPENAI",
  ] as const) {
    assert.ok(kinds.has(expected), `missing preset ${expected}`);
  }

  const tiers = new Set(BUILTIN_MODEL_CATALOG_PRESETS.map((preset) => preset.tier));
  assert.ok(tiers.has("PAID"));
  assert.ok(tiers.has("FREE"));
  assert.ok(tiers.has("LOCAL"));

  for (const preset of BUILTIN_MODEL_CATALOG_PRESETS) {
    assert.ok(preset.models.length > 0, `${preset.providerKind} must expose models`);
  }
});

test("filterCatalogPresets narrows by tier and Bitap fuzzy query across presets and model ids", () => {
  const localOnly = filterCatalogPresets(BUILTIN_MODEL_CATALOG_PRESETS, "LOCAL", "");
  assert.ok(localOnly.some((preset) => preset.providerKind === "OLLAMA"));
  assert.ok(localOnly.every((preset) => preset.tier === "LOCAL"));

  const geminiMatches = filterCatalogPresets(BUILTIN_MODEL_CATALOG_PRESETS, "ALL", "gemini-2.5");
  assert.ok(geminiMatches.some((preset) => preset.providerKind === "GOOGLE"));

  const freeGroq = filterCatalogPresets(BUILTIN_MODEL_CATALOG_PRESETS, "FREE", "groq");
  assert.equal(freeGroq.length, 1);
  assert.equal(freeGroq[0]?.providerKind, "GROQ");
});

test("normalizeModelCatalogResponse falls back safely when payload is empty or malformed", () => {
  const fallback = normalizeModelCatalogResponse(null);
  assert.equal(fallback.presets.length, BUILTIN_MODEL_CATALOG_PRESETS.length);

  const custom = normalizeModelCatalogResponse({
    presets: [
      {
        providerKind: "OPENAI",
        displayName: "OpenAI",
        defaultBaseUrl: "https://api.openai.com/v1/",
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
        ],
      },
    ],
  });
  assert.equal(custom.presets.length, 1);
  assert.equal(custom.presets[0]?.defaultBaseUrl, "https://api.openai.com/v1");
});

test("validateModelProviderInput enforces fail-closed URL scheme, character limits, and priority bounds", () => {
  const valid = validateModelProviderInput({
    providerKind: "OPENAI",
    wireProtocol: "OPENAI_RESPONSES",
    displayName: "  Primary OpenAI  ",
    baseUrl: "https://api.openai.com/v1/",
    apiKey: " sk-demo-key-1234567890 ",
    defaultModel: " gpt-4.1 ",
    priority: 10,
    enabled: true,
  });
  assert.equal(valid.displayName, "Primary OpenAI");
  assert.equal(valid.baseUrl, "https://api.openai.com/v1");
  assert.equal(valid.apiKey, "sk-demo-key-1234567890");
  assert.equal(valid.defaultModel, "gpt-4.1");
  assert.equal(valid.priority, 10);

  // Reject unsafe URL schemes and embedded credentials
  for (const badUrl of [
    "",
    "ftp://example.com/v1",
    "javascript:alert(1)",
    "file:///etc/passwd",
    "https://user:pass@api.openai.com/v1",
    "https://api.openai.com/v1?api-version=2025",
    `https://example.com/${"a".repeat(MAX_MODEL_PROVIDER_URL_CHARS)}`,
  ]) {
    assert.throws(
      () =>
        validateModelProviderInput({
          providerKind: "CUSTOM_OPENAI",
          wireProtocol: "OPENAI_CHAT",
          displayName: "Custom",
          baseUrl: badUrl,
        }),
      RangeError,
      `should reject baseUrl=${badUrl.slice(0, 32)}`,
    );
  }

  // Reject display name bounds
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "   ",
        baseUrl: "https://api.openai.com/v1",
      }),
    RangeError,
  );
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "x".repeat(MAX_MODEL_PROVIDER_DISPLAY_NAME_CHARS + 1),
        baseUrl: "https://api.openai.com/v1",
      }),
    RangeError,
  );

  // Reject oversized or control-char API key
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "OpenAI",
        baseUrl: "https://api.openai.com/v1",
        apiKey: "x".repeat(MAX_MODEL_PROVIDER_API_KEY_CHARS + 1),
      }),
    RangeError,
  );
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "OpenAI",
        baseUrl: "https://api.openai.com/v1",
        apiKey: "bad\nkey",
      }),
    RangeError,
  );

  // Reject out-of-range priority
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "OpenAI",
        baseUrl: "https://api.openai.com/v1",
        priority: -1,
      }),
    RangeError,
  );
  assert.throws(
    () =>
      validateModelProviderInput({
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        displayName: "OpenAI",
        baseUrl: "https://api.openai.com/v1",
        priority: MAX_MODEL_PROVIDER_PRIORITY + 1,
      }),
    RangeError,
  );

  // Reject invalid connection test mode
  assert.throws(
    () =>
      validateModelProviderTestInput({
        mode: "invalid" as "models",
        providerKind: "OPENAI",
        wireProtocol: "OPENAI_CHAT",
        baseUrl: "https://api.openai.com/v1",
      }),
    RangeError,
  );
});

test("sortModelProvidersByPriority orders lowest priority value first and enabled ahead of disabled", () => {
  const base: Omit<WorkspaceModelProvider, "providerId" | "priority" | "enabled" | "displayName"> = {
    workspaceId: 101,
    providerKind: "OPENAI",
    wireProtocol: "OPENAI_CHAT",
    baseUrl: "https://api.openai.com/v1",
    defaultModel: "gpt-4o",
    maskedApiKey: "sk-****",
    hasApiKey: true,
    lastStatus: "CONNECTED",
    lastLatencyMs: 120,
    lastError: null,
    lastCheckedAt: null,
    discoveredModels: [],
    createdAt: null,
    updatedAt: null,
  };
  const sorted = sortModelProvidersByPriority([
    { ...base, providerId: 3, priority: 20, enabled: true, displayName: "Secondary" },
    { ...base, providerId: 2, priority: 10, enabled: false, displayName: "Disabled Primary" },
    { ...base, providerId: 1, priority: 10, enabled: true, displayName: "Primary" },
  ]);

  assert.deepEqual(
    sorted.map((item) => item.providerId),
    [1, 2, 3],
  );
});

test("model-providers API client exercises list, create, patch, delete, and connection-test routes", async () => {
  const originalFetch = globalThis.fetch;
  const calls: Array<{ url: string; method: string; body: unknown }> = [];

  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    const body = typeof init?.body === "string" ? JSON.parse(init.body) : null;
    calls.push({ url, method, body });

    if (method === "GET" && url.endsWith("/api/v1/workspaces/101/model-providers")) {
      return new Response(
        JSON.stringify({
          success: true,
          code: "OK",
          message: "ok",
          data: [
            {
              providerId: 11,
              workspaceId: 101,
              providerKind: "OPENAI",
              wireProtocol: "OPENAI_RESPONSES",
              displayName: "Primary OpenAI",
              baseUrl: "https://api.openai.com/v1",
              defaultModel: "gpt-4.1",
              enabledModels: ["gpt-4.1", "gpt-4o"],
              priorityOrder: 10,
              enabled: true,
              maskedApiKey: "sk-****7890",
              lastStatus: "OK",
              lastLatencyMs: 85,
            },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (method === "POST" && url.endsWith("/api/v1/workspaces/101/model-providers/connection-test")) {
      return new Response(
        JSON.stringify({
          success: true,
          code: "OK",
          message: "ok",
          data: {
            ok: true,
            status: "CONNECTED",
            latencyMs: 64,
            discoveredModels: ["gpt-4.1", "gpt-4o"],
            resolvedModel: "gpt-4.1",
            replyPreview: "pong",
            promptTokens: 8,
            completionTokens: 2,
            message: "실호출 테스트 성공",
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (method === "DELETE") {
      return new Response(null, { status: 204 });
    }

    return new Response(
      JSON.stringify({
        success: true,
        code: "OK",
        message: "ok",
        data: {
          providerId: 12,
          workspaceId: 101,
          providerKind: "OLLAMA",
          wireProtocol: "OLLAMA_NATIVE",
          displayName: "Local Ollama",
          baseUrl: "http://127.0.0.1:11434",
          defaultModel: "qwen2.5-coder:14b",
          enabledModels: ["qwen2.5-coder:14b"],
          priorityOrder: 20,
          enabled: true,
          maskedApiKey: null,
        },
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  };

  try {
    const listed = await fetchWorkspaceModelProviders("tok", 101);
    assert.equal(listed.length, 1);
    assert.equal(listed[0]?.providerId, 11);
    assert.equal(listed[0]?.priority, 10);
    assert.deepEqual(listed[0]?.discoveredModels, ["gpt-4.1", "gpt-4o"]);

    const created = await createWorkspaceModelProvider("tok", 101, {
      providerKind: "OLLAMA",
      wireProtocol: "OLLAMA_NATIVE",
      displayName: "Local Ollama",
      baseUrl: "http://127.0.0.1:11434/",
      defaultModel: "qwen2.5-coder:14b",
      enabledModels: ["qwen2.5-coder:14b"],
      priority: 20,
    });
    assert.equal(created.providerId, 12);
    assert.equal(created.priority, 20);
    assert.deepEqual((calls[1]?.body as Record<string, unknown>)?.priorityOrder, 20);
    assert.deepEqual((calls[1]?.body as Record<string, unknown>)?.enabledModels, ["qwen2.5-coder:14b"]);

    await updateWorkspaceModelProvider("tok", 101, 12, { enabled: false, priority: 30 });
    assert.deepEqual((calls[2]?.body as Record<string, unknown>)?.priorityOrder, 30);
    await deleteWorkspaceModelProvider("tok", 101, 12);

    const testResult = await testWorkspaceModelProviderConnection("tok", 101, {
      mode: "completion",
      providerKind: "OPENAI",
      wireProtocol: "OPENAI_RESPONSES",
      baseUrl: "https://api.openai.com/v1",
      modelId: "gpt-4.1",
    });
    assert.equal(testResult.ok, true);
    assert.deepEqual(testResult.discoveredModels, ["gpt-4.1", "gpt-4o"]);
    assert.equal(testResult.replyPreview, "pong");
    assert.equal(testResult.promptTokens, 8);
    assert.equal(testResult.completionTokens, 2);
    assert.equal(calls.length, 5);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("WorkspaceAgent payload types support optional modelProviderId, modelId, and fallbackModelId", () => {
  const createPayload: CreateWorkspaceAgentInput = {
    name: "Model Agent",
    category: "BACKEND",
    modelProviderId: 11,
    modelId: "gpt-4.1",
    fallbackModelId: "gpt-4o-mini",
  };
  const updatePayload: UpdateWorkspaceAgentInput = {
    name: "Model Agent",
    modelProviderId: null,
    modelId: null,
    fallbackModelId: null,
  };
  const agent: WorkspaceAgent = {
    agentId: 1,
    workspaceId: 101,
    name: "Model Agent",
    category: "BACKEND",
    openClawAgentId: null,
    workspacePath: "/workspace",
    status: "READY",
    syncError: null,
    createdByGoogleSub: "sub",
    createdAt: null,
    updatedAt: null,
    skillFiles: [],
    modelProviderId: createPayload.modelProviderId,
    modelId: createPayload.modelId,
    fallbackModelId: createPayload.fallbackModelId,
  };

  assert.equal(agent.modelProviderId, 11);
  assert.equal(agent.modelId, "gpt-4.1");
  assert.equal(updatePayload.modelProviderId, null);
});

test("normalizeWorkspaceModelProviders prefers priorityOrder and enabledModels with legacy fallback", () => {
  const canonical = normalizeWorkspaceModelProviders([
    {
      providerId: 21,
      workspaceId: 101,
      providerKind: "OPENAI",
      wireProtocol: "OPENAI_RESPONSES",
      displayName: "Canonical",
      baseUrl: "https://api.openai.com/v1",
      defaultModel: "gpt-5.2",
      enabledModels: ["gpt-5.2", "gpt-5-mini"],
      priorityOrder: 5,
      enabled: true,
    },
    {
      providerId: 22,
      workspaceId: 101,
      providerKind: "OPENAI",
      wireProtocol: "OPENAI_CHAT",
      displayName: "Legacy",
      baseUrl: "https://api.openai.com/v1",
      defaultModel: "gpt-4.1",
      discoveredModels: ["gpt-4.1"],
      priority: 15,
      enabled: true,
    },
  ]);

  assert.equal(canonical[0]?.providerId, 21);
  assert.equal(canonical[0]?.priority, 5);
  assert.deepEqual(canonical[0]?.discoveredModels, ["gpt-5.2", "gpt-5-mini"]);
  assert.equal(canonical[1]?.providerId, 22);
  assert.equal(canonical[1]?.priority, 15);
  assert.deepEqual(canonical[1]?.discoveredModels, ["gpt-4.1"]);
});

test("normalizeWorkspaceModelTelemetry bounds entries and rejects malformed items", () => {
  assert.equal(workspaceModelProviderTelemetryPath(101), "/api/v1/workspaces/101/model-providers/telemetry");
  const normalized = normalizeWorkspaceModelTelemetry([
    {
      modelName: "gpt-5.2",
      executionCount: 12,
      successCount: 11,
      failedCount: 1,
      totalPromptTokens: 4000,
      totalCompletionTokens: 1200,
      totalTokens: 5200,
      avgDurationMs: 640,
    },
    {
      modelName: "claude-sonnet-4-5",
      executionCount: 1,
      successCount: 1,
      failedCount: 0,
      totalPromptTokens: 100,
      totalCompletionTokens: 50,
      totalTokens: 150,
      avgDurationMs: null,
    },
    {
      modelName: "   ",
      executionCount: 5,
    },
    null,
  ]);

  assert.equal(normalized.length, 2);
  assert.equal(normalized[0]?.modelName, "gpt-5.2");
  assert.equal(normalized[0]?.totalTokens, 5200);
  assert.equal(normalized[0]?.avgDurationMs, 640);
  assert.equal(normalized[1]?.avgDurationMs, null);
  assert.equal(typeof fetchWorkspaceModelTelemetry, "function");
});

test("findCatalogModel, formatTokenCountShort, and computeReorderedProviderPriorities", () => {
  const model = findCatalogModel("gemini-2.5-pro");
  assert.ok(model);
  assert.equal(model.supportsVision, true);
  assert.equal(formatTokenCountShort(model.contextWindow), "1M");
  assert.equal(formatTokenCountShort(128_000), "128K");

  const base: Omit<WorkspaceModelProvider, "providerId" | "priority" | "displayName"> = {
    workspaceId: 101,
    providerKind: "OPENAI",
    wireProtocol: "OPENAI_CHAT",
    baseUrl: "https://api.openai.com/v1",
    defaultModel: "gpt-4o",
    enabled: true,
    maskedApiKey: "sk-****",
    hasApiKey: true,
    lastStatus: "CONNECTED",
    lastLatencyMs: 120,
    lastError: null,
    lastCheckedAt: null,
    discoveredModels: [],
    createdAt: null,
    updatedAt: null,
  };
  const providers: WorkspaceModelProvider[] = [
    { ...base, providerId: 1, priority: 10, displayName: "A" },
    { ...base, providerId: 2, priority: 20, displayName: "B" },
  ];
  assert.deepEqual(computeReorderedProviderPriorities(providers, 2, "up"), [
    { providerId: 2, priority: 10 },
    { providerId: 1, priority: 20 },
  ]);

  const tied: WorkspaceModelProvider[] = [
    { ...base, providerId: 1, priority: 100, displayName: "A" },
    { ...base, providerId: 2, priority: 100, displayName: "B" },
  ];
  assert.deepEqual(computeReorderedProviderPriorities(tied, 2, "up"), [
    { providerId: 2, priority: 90 },
  ]);
});
