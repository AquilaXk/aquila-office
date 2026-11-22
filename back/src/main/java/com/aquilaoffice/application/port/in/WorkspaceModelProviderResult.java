package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 응답 결과 모델이다. 평문 API 키는 절대 노출하지 않는다.
 */
public record WorkspaceModelProviderResult(
        Long providerId,
        Long workspaceId,
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String displayName,
        String baseUrl,
        boolean hasApiKey,
        String maskedApiKey,
        String defaultModel,
        List<String> enabledModels,
        int priorityOrder,
        boolean enabled,
        ModelProviderConnectionStatus lastStatus,
        Long lastLatencyMs,
        LocalDateTime lastCheckedAt,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public WorkspaceModelProviderResult {
        enabledModels = enabledModels == null ? List.of() : List.copyOf(enabledModels);
    }

    public static WorkspaceModelProviderResult from(WorkspaceModelProvider provider) {
        String apiKey = provider.getApiKey();
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        return new WorkspaceModelProviderResult(
                provider.getId(),
                provider.getWorkspaceId(),
                provider.getProviderKind(),
                provider.getWireProtocol(),
                provider.getDisplayName(),
                provider.getBaseUrl(),
                hasKey,
                WorkspaceModelProvider.maskApiKey(apiKey),
                provider.getDefaultModel(),
                provider.getEnabledModels(),
                provider.getPriorityOrder(),
                provider.isEnabled(),
                provider.getLastStatus(),
                provider.getLastLatencyMs(),
                provider.getLastCheckedAt(),
                provider.getLastError(),
                provider.getCreatedAt(),
                provider.getUpdatedAt());
    }
}
