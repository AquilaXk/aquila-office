package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 연결 테스트 HTTP 응답이다.
 */
@Schema(description = "AI 모델 공급자 연결 테스트 응답")
public record WorkspaceModelConnectionTestResponse(
        boolean ok,
        ModelProviderConnectionStatus status,
        long latencyMs,
        List<String> discoveredModels,
        @Schema(nullable = true) String resolvedModel,
        @Schema(nullable = true) String replyPreview,
        @Schema(nullable = true) Integer promptTokens,
        @Schema(nullable = true) Integer completionTokens,
        String message) {

    public static WorkspaceModelConnectionTestResponse from(WorkspaceModelConnectionTestResult result) {
        return new WorkspaceModelConnectionTestResponse(
                result.ok(),
                result.status(),
                result.latencyMs(),
                result.discoveredModels(),
                result.resolvedModel(),
                result.replyPreview(),
                result.promptTokens(),
                result.completionTokens(),
                result.message());
    }
}
