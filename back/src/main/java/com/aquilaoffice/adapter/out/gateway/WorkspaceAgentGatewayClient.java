package com.aquilaoffice.adapter.out.gateway;

interface WorkspaceAgentGatewayClient {

    WorkspaceAgentCreateGatewayResponse create(WorkspaceAgentCreateGatewayRequest request);

    WorkspaceAgentSyncSkillFilesGatewayResponse syncSkillFiles(
            WorkspaceAgentSyncSkillFilesGatewayRequest request);

    WorkspaceAgentListGatewayResponse list(WorkspaceAgentListGatewayRequest request);

    WorkspaceAgentDeleteGatewayResponse delete(WorkspaceAgentDeleteGatewayRequest request);

    WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request);

    WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request);
}
