package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 gateway 연결 테스트 inbound port다.
 */
public interface TestWorkspaceGatewayConnectionUseCase {

    TestWorkspaceGatewayConnectionResult testWorkspaceGatewayConnection(TestWorkspaceGatewayConnectionCommand command);
}
