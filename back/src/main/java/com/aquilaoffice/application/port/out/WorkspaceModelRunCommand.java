package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;

/**
 * 직접 모델 공급자 호출에 필요한 요청 payload다.
 */
public record WorkspaceModelRunCommand(
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String baseUrl,
        String apiKey,
        String modelId,
        String systemPrompt,
        String userPrompt) {}
