package com.aquilaoffice.application.port.out;

public interface AuthenticateWorkspaceGatewayPort {

    WorkspaceGatewayAuthenticationResult authenticate(WorkspaceGatewayAuthenticationCommand command);
}
