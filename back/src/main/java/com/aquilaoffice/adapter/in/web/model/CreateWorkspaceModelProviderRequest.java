package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderCommand;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 워크스페이스 AI 모델 공급자 생성 HTTP 요청이다.
 */
@Schema(description = "워크스페이스 AI 모델 공급자 생성 요청")
public record CreateWorkspaceModelProviderRequest(
        @NotNull ModelProviderKind providerKind,
        @Schema(nullable = true) ModelWireProtocol wireProtocol,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 500) String baseUrl,
        @Schema(nullable = true) @Size(max = 1000) String apiKey,
        @NotBlank @Size(max = 160) String defaultModel,
        @Schema(nullable = true) @Size(max = 100) List<String> enabledModels,
        @Schema(nullable = true) Integer priorityOrder,
        @Schema(nullable = true) Boolean enabled) {

    public CreateWorkspaceModelProviderCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateWorkspaceModelProviderCommand(
                workspaceId,
                requesterGoogleSub,
                providerKind,
                wireProtocol,
                displayName,
                baseUrl,
                apiKey,
                defaultModel,
                enabledModels,
                priorityOrder,
                enabled);
    }
}
