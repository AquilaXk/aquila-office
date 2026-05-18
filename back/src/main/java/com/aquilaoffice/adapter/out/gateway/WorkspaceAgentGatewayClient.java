package com.aquilaoffice.adapter.out.gateway;

interface WorkspaceAgentGatewayClient {

    WorkspaceAgentCreateGatewayResponse create(WorkspaceAgentCreateGatewayRequest request);

    WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request);

    WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request);
}
