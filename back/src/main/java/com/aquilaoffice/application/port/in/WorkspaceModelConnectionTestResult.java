package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import java.util.List;

/**
 * AI 모델 공급자 연결 테스트 응답 결과 모델이다.
 */
public record WorkspaceModelConnectionTestResult(
        boolean ok,
        ModelProviderConnectionStatus status,
        long latencyMs,
        List<String> discoveredModels,
        String resolvedModel,
        String replyPreview,
        Integer promptTokens,
        Integer completionTokens,
        String message) {

    public WorkspaceModelConnectionTestResult {
        discoveredModels = discoveredModels == null ? List.of() : List.copyOf(discoveredModels);
    }
}
