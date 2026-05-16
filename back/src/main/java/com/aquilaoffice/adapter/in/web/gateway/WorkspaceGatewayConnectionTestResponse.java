package com.aquilaoffice.adapter.in.web.gateway;

import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionResult;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

/**
 * gateway 연결 테스트 HTTP response body다.
 */
public record WorkspaceGatewayConnectionTestResponse(
        GatewayConnectionStatus status,
        boolean connected,
        String gatewayUrl,
        String message,
        int agentCount) {

    public static WorkspaceGatewayConnectionTestResponse from(TestWorkspaceGatewayConnectionResult result) {
        return new WorkspaceGatewayConnectionTestResponse(
                result.status(),
                result.connected(),
                result.gatewayUrl(),
                result.message(),
                result.agentCount());
    }
}
