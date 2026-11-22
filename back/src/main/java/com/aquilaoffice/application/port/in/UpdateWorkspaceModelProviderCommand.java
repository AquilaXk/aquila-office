package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelWireProtocol;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 수정 요청 command다.
 */
public record UpdateWorkspaceModelProviderCommand(
        Long workspaceId,
        Long providerId,
        String requesterGoogleSub,
        String displayName,
        ModelWireProtocol wireProtocol,
        String baseUrl,
        String apiKey,
        String defaultModel,
        List<String> enabledModels,
        Integer priorityOrder,
        Boolean enabled) {}
