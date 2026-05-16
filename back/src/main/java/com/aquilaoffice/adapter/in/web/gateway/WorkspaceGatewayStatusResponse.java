package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusResult;
import com.aquilaoffice.domain.gateway.GatewayBindingStatus;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

/**
 * gateway binding 상태 조회 HTTP response body다.
 */
public record WorkspaceGatewayStatusResponse(
        GatewayBindingStatus status,
        boolean bound,
        Long bindingId,
        String mode,
        String gatewayUrl,
        String maskedToken,
        GatewayConnectionStatus lastStatus,
        String lastCheckedAt,
        String lastError,
        String updatedAt) {

    public static WorkspaceGatewayStatusResponse from(GetWorkspaceGatewayStatusResult result) {
        return new WorkspaceGatewayStatusResponse(
                result.status(),
                result.bound(),
                result.bindingId(),
                result.mode(),
                result.gatewayUrl(),
                result.maskedToken(),
                result.lastStatus(),
                result.lastCheckedAt(),
                result.lastError(),
                result.updatedAt());
    }
}
