package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.WorkspaceModelProviderResult;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 HTTP 응답이다.
 */
@Schema(description = "워크스페이스 AI 모델 공급자 응답")
public record WorkspaceModelProviderResponse(
        Long providerId,
        Long workspaceId,
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String displayName,
        String baseUrl,
        boolean hasApiKey,
        @Schema(nullable = true) String maskedApiKey,
        String defaultModel,
        List<String> enabledModels,
        int priorityOrder,
        boolean enabled,
        @Schema(nullable = true) ModelProviderConnectionStatus lastStatus,
        @Schema(nullable = true) Long lastLatencyMs,
        @Schema(nullable = true) LocalDateTime lastCheckedAt,
        @Schema(nullable = true) String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static WorkspaceModelProviderResponse from(WorkspaceModelProviderResult result) {
        return new WorkspaceModelProviderResponse(
                result.providerId(),
                result.workspaceId(),
                result.providerKind(),
                result.wireProtocol(),
                result.displayName(),
                result.baseUrl(),
                result.hasApiKey(),
                result.maskedApiKey(),
                result.defaultModel(),
                result.enabledModels(),
                result.priorityOrder(),
                result.enabled(),
                result.lastStatus(),
                result.lastLatencyMs(),
                result.lastCheckedAt(),
                result.lastError(),
                result.createdAt(),
                result.updatedAt());
    }
}
