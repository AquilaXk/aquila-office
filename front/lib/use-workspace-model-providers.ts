"use client";

import { useCallback, useEffect, useState } from "react";
import { messageFromError } from "@/components/workspace/workspace-ui-helpers";
import { resolveAccessToken } from "@/lib/auth";
import {
  BUILTIN_MODEL_CATALOG_PRESETS,
  fetchModelCatalog,
  type ModelCatalogPreset,
} from "@/lib/model-catalog";
import {
  createWorkspaceModelProvider,
  deleteWorkspaceModelProvider,
  fetchWorkspaceModelProviders,
  sortModelProvidersByPriority,
  testWorkspaceModelProviderConnection,
  updateWorkspaceModelProvider,
  type CreateWorkspaceModelProviderInput,
  type ModelProviderConnectionTestInput,
  type ModelProviderConnectionTestResult,
  type WorkspaceModelProvider,
} from "@/lib/model-providers";

export type ModelProvidersLoadStatus = "idle" | "loading" | "ready" | "error";

export type ModelProviderFeedback = {
  tone: "success" | "danger";
  message: string;
};

export type WorkspaceModelProvidersController = {
  workspaceId: number | null;
  canManage: boolean;
  presets: readonly ModelCatalogPreset[];
  providers: WorkspaceModelProvider[];
  loadStatus: ModelProvidersLoadStatus;
  loadError: string | null;
  saving: boolean;
  togglingId: number | null;
  deletingId: number | null;
  testingMode: "models" | "completion" | null;
  testResult: ModelProviderConnectionTestResult | null;
  testError: string | null;
  feedback: ModelProviderFeedback | null;
  reload: () => Promise<void>;
  saveProvider: (
    editingProviderId: number | null,
    input: CreateWorkspaceModelProviderInput,
  ) => Promise<WorkspaceModelProvider | null>;
  toggleProviderEnabled: (provider: WorkspaceModelProvider) => Promise<void>;
  removeProvider: (provider: WorkspaceModelProvider) => Promise<boolean>;
  runConnectionTest: (
    input: ModelProviderConnectionTestInput,
  ) => Promise<ModelProviderConnectionTestResult | null>;
  clearFeedback: () => void;
};

export function useWorkspaceModelProviders(options: {
  workspaceId: number | null;
  canManage: boolean;
  enabled?: boolean;
}): WorkspaceModelProvidersController {
  const { workspaceId, canManage, enabled = true } = options;

  const [presets, setPresets] = useState<readonly ModelCatalogPreset[]>(
    BUILTIN_MODEL_CATALOG_PRESETS,
  );
  const [providers, setProviders] = useState<WorkspaceModelProvider[]>([]);
  const [loadStatus, setLoadStatus] = useState<ModelProvidersLoadStatus>("idle");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [testingMode, setTestingMode] = useState<"models" | "completion" | null>(null);
  const [testResult, setTestResult] = useState<ModelProviderConnectionTestResult | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<ModelProviderFeedback | null>(null);

  const reload = useCallback(async () => {
    if (!workspaceId || !Number.isInteger(workspaceId) || workspaceId <= 0) {
      setProviders([]);
      setLoadStatus("idle");
      setLoadError(null);
      return;
    }

    setLoadStatus("loading");
    setLoadError(null);
    const accessToken = resolveAccessToken();
    const [catalogRes, providersRes] = await Promise.allSettled([
      fetchModelCatalog(accessToken),
      fetchWorkspaceModelProviders(accessToken, workspaceId),
    ]);

    if (catalogRes.status === "fulfilled" && catalogRes.value.presets.length > 0) {
      setPresets(catalogRes.value.presets);
    }

    if (providersRes.status === "fulfilled") {
      setProviders(sortModelProvidersByPriority(providersRes.value));
      setLoadStatus("ready");
    } else {
      setProviders([]);
      setLoadStatus("error");
      setLoadError(
        messageFromError(providersRes.reason, "AI 모델 프로바이더 목록을 불러오지 못했습니다."),
      );
    }
  }, [workspaceId]);

  useEffect(() => {
    if (!enabled || !workspaceId || workspaceId <= 0) {
      return;
    }
    let cancelled = false;
    void Promise.resolve().then(() => {
      if (!cancelled) {
        return reload();
      }
      return undefined;
    });
    return () => {
      cancelled = true;
    };
  }, [enabled, reload, workspaceId]);

  const clearFeedback = useCallback(() => {
    setFeedback(null);
    setTestError(null);
    setTestResult(null);
  }, []);

  const saveProvider = useCallback(
    async (
      editingProviderId: number | null,
      input: CreateWorkspaceModelProviderInput,
    ): Promise<WorkspaceModelProvider | null> => {
      if (!workspaceId || !canManage) {
        return null;
      }
      setSaving(true);
      setFeedback(null);
      try {
        const accessToken = resolveAccessToken();
        const saved =
          editingProviderId !== null
            ? await updateWorkspaceModelProvider(accessToken, workspaceId, editingProviderId, {
                providerKind: input.providerKind,
                wireProtocol: input.wireProtocol,
                displayName: input.displayName,
                baseUrl: input.baseUrl,
                ...(input.apiKey ? { apiKey: input.apiKey } : {}),
                defaultModel: input.defaultModel ?? null,
                ...(input.enabledModels !== undefined
                  ? { enabledModels: input.enabledModels }
                  : {}),
                priority: input.priority,
                enabled: input.enabled,
              })
            : await createWorkspaceModelProvider(accessToken, workspaceId, input);

        setProviders((current) =>
          sortModelProvidersByPriority([
            saved,
            ...current.filter((item) => item.providerId !== saved.providerId),
          ]),
        );
        setFeedback({
          tone: "success",
          message:
            editingProviderId !== null
              ? `'${saved.displayName}' 프로바이더 설정을 저장했습니다.`
              : `'${saved.displayName}' 프로바이더를 등록했습니다.`,
        });
        return saved;
      } catch (error) {
        setFeedback({
          tone: "danger",
          message: messageFromError(error, "AI 모델 프로바이더를 저장하지 못했습니다."),
        });
        return null;
      } finally {
        setSaving(false);
      }
    },
    [canManage, workspaceId],
  );

  const toggleProviderEnabled = useCallback(
    async (provider: WorkspaceModelProvider) => {
      if (!workspaceId || !canManage) {
        return;
      }
      setTogglingId(provider.providerId);
      setFeedback(null);
      try {
        const updated = await updateWorkspaceModelProvider(
          resolveAccessToken(),
          workspaceId,
          provider.providerId,
          { enabled: !provider.enabled },
        );
        setProviders((current) =>
          sortModelProvidersByPriority(
            current.map((item) => (item.providerId === updated.providerId ? updated : item)),
          ),
        );
        setFeedback({
          tone: "success",
          message: `'${updated.displayName}' 상태를 ${updated.enabled ? "활성" : "비활성"}으로 전환했습니다.`,
        });
      } catch (error) {
        setFeedback({
          tone: "danger",
          message: messageFromError(error, "프로바이더 활성 상태를 변경하지 못했습니다."),
        });
      } finally {
        setTogglingId(null);
      }
    },
    [canManage, workspaceId],
  );

  const removeProvider = useCallback(
    async (provider: WorkspaceModelProvider): Promise<boolean> => {
      if (!workspaceId || !canManage) {
        return false;
      }
      setDeletingId(provider.providerId);
      setFeedback(null);
      try {
        await deleteWorkspaceModelProvider(
          resolveAccessToken(),
          workspaceId,
          provider.providerId,
        );
        setProviders((current) =>
          current.filter((item) => item.providerId !== provider.providerId),
        );
        setFeedback({
          tone: "success",
          message: `'${provider.displayName}' 프로바이더를 삭제했습니다.`,
        });
        return true;
      } catch (error) {
        setFeedback({
          tone: "danger",
          message: messageFromError(error, "프로바이더를 삭제하지 못했습니다."),
        });
        return false;
      } finally {
        setDeletingId(null);
      }
    },
    [canManage, workspaceId],
  );

  const runConnectionTest = useCallback(
    async (
      input: ModelProviderConnectionTestInput,
    ): Promise<ModelProviderConnectionTestResult | null> => {
      if (!workspaceId || !canManage) {
        return null;
      }
      setTestingMode(input.mode);
      setTestError(null);
      setTestResult(null);
      try {
        const result = await testWorkspaceModelProviderConnection(
          resolveAccessToken(),
          workspaceId,
          input,
        );
        setTestResult(result);
        return result;
      } catch (error) {
        setTestError(messageFromError(error, "프로바이더 연결 테스트에 실패했습니다."));
        return null;
      } finally {
        setTestingMode(null);
      }
    },
    [canManage, workspaceId],
  );

  return {
    workspaceId,
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
  };
}
