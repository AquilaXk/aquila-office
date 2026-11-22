package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 등록 요청 command다.
 */
public record CreateWorkspaceModelProviderCommand(
        Long workspaceId,
        String requesterGoogleSub,
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String displayName,
        String baseUrl,
        String apiKey,
        String defaultModel,
        List<String> enabledModels,
        Integer priorityOrder,
        Boolean enabled) {}
