package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderCommand;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 AI 모델 공급자 연결 테스트 HTTP 요청이다.
 */
@Schema(description = "워크스페이스 AI 모델 공급자 연결 테스트 요청")
public record TestWorkspaceModelProviderRequest(
        @Schema(nullable = true) Long providerId,
        @Schema(nullable = true) ModelProviderKind providerKind,
        @Schema(nullable = true) ModelWireProtocol wireProtocol,
        @Schema(nullable = true) @Size(max = 500) String baseUrl,
        @Schema(nullable = true) @Size(max = 1000) String apiKey,
        @Schema(nullable = true) @Size(max = 160) String modelId,
        @Schema(nullable = true) String mode) {

    public TestWorkspaceModelProviderCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new TestWorkspaceModelProviderCommand(
                workspaceId,
                requesterGoogleSub,
                providerId,
                providerKind,
                wireProtocol,
                baseUrl,
                apiKey,
                modelId,
                mode);
    }
}
