package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * gateway binding 등록 HTTP request body다.
 */
public record CreateWorkspaceGatewayBindingRequest(
        @NotBlank String gatewayUrl,
        @NotBlank String token,
        Boolean validateConnection) {

    public CreateWorkspaceGatewayBindingCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateWorkspaceGatewayBindingCommand(
                workspaceId,
                requesterGoogleSub,
                gatewayUrl,
                token,
                Boolean.TRUE.equals(validateConnection));
    }
}
