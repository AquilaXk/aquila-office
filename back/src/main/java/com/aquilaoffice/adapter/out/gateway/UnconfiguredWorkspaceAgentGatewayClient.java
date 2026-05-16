package com.aquilaoffice.adapter.out.gateway;

final class UnconfiguredWorkspaceAgentGatewayClient implements WorkspaceAgentGatewayClient {

    private static final String FAILURE_REASON = "workspace agent gateway client is not configured";

    @Override
    public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
        return new WorkspaceAgentGatewayResponse(false, null, FAILURE_REASON);
    }
}
