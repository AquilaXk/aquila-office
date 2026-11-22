package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;

/**
 * 모델 공급자 연결 테스트 요청 모델이다.
 */
public record WorkspaceModelConnectionTestRequest(
        ModelProviderKind providerKind,
        ModelWireProtocol wireProtocol,
        String baseUrl,
        String apiKey,
        String modelId,
        String mode) {}
