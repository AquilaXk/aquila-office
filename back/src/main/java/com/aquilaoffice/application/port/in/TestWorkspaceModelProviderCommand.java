package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;

/**
 * 워크스페이스 AI 모델 공급자 연결 테스트 command다.
 */
public record TestWorkspaceModelProviderCommand(
        Long workspaceId,
        String requesterGoogleSub,
        Long providerId,
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String baseUrl,
        String apiKey,
        String modelId,
        String mode) {}
