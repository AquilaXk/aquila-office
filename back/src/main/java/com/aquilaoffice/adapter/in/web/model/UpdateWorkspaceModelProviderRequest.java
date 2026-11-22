package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderCommand;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 수정 HTTP 요청이다.
 */
@Schema(description = "워크스페이스 AI 모델 공급자 수정 요청")
public record UpdateWorkspaceModelProviderRequest(
        @Schema(nullable = true) @Size(max = 120) String displayName,
        @Schema(nullable = true) ModelWireProtocol wireProtocol,
        @Schema(nullable = true) @Size(max = 500) String baseUrl,
        @Schema(nullable = true) @Size(max = 1000) String apiKey,
        @Schema(nullable = true) @Size(max = 160) String defaultModel,
        @Schema(nullable = true) @Size(max = 100) List<String> enabledModels,
        @Schema(nullable = true) Integer priorityOrder,
        @Schema(nullable = true) Boolean enabled) {

    public UpdateWorkspaceModelProviderCommand toCommand(
            Long workspaceId,
            Long providerId,
            String requesterGoogleSub) {
        return new UpdateWorkspaceModelProviderCommand(
                workspaceId,
                providerId,
                requesterGoogleSub,
                displayName,
                wireProtocol,
                baseUrl,
                apiKey,
                defaultModel,
                enabledModels,
                priorityOrder,
                enabled);
    }
}
