package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * gateway 연결 테스트 HTTP request body다.
 */
public record TestWorkspaceGatewayConnectionRequest(
        @NotBlank String gatewayUrl,
        @NotBlank String token) {

    public TestWorkspaceGatewayConnectionCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new TestWorkspaceGatewayConnectionCommand(workspaceId, requesterGoogleSub, gatewayUrl, token);
    }
}
