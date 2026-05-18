package com.aquilaoffice.adapter.out.gateway;

interface WorkspaceAgentGatewayClient {

    WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request);

    WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request);
}
