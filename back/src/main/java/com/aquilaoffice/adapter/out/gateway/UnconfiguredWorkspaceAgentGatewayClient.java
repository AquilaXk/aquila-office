package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

final class UnconfiguredWorkspaceAgentGatewayClient implements WorkspaceAgentGatewayClient {

    private static final String FAILURE_REASON = "workspace agent gateway client is not configured";

    @Override
    public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
        return new WorkspaceAgentGatewayResponse(false, null, FAILURE_REASON);
    }

    @Override
    public WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request) {
        return new WorkspaceGatewayAuthenticationResponse(
                GatewayConnectionStatus.FAILED,
                false,
                request.gatewayUrl(),
                null,
                null,
                FAILURE_REASON,
                0,
                FAILURE_REASON);
    }
}
