package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingResult;

/**
 * gateway binding 등록 결과 HTTP response body다.
 */
public record WorkspaceGatewayBindingResponse(
        Long id,
        Long workspaceId,
        String mode,
        String gatewayUrl,
        String maskedToken) {

    public static WorkspaceGatewayBindingResponse from(CreateWorkspaceGatewayBindingResult result) {
        return new WorkspaceGatewayBindingResponse(
                result.id(),
                result.workspaceId(),
                result.mode(),
                result.gatewayUrl(),
                result.maskedToken());
    }
}
