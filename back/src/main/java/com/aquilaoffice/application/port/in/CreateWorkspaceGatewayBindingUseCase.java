package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 gateway binding 등록 inbound port다.
 */
public interface CreateWorkspaceGatewayBindingUseCase {

    CreateWorkspaceGatewayBindingResult createWorkspaceGatewayBinding(CreateWorkspaceGatewayBindingCommand command);
}
