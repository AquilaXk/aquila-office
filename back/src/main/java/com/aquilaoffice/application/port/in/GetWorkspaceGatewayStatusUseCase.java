package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 gateway binding 상태 조회 inbound port다.
 */
public interface GetWorkspaceGatewayStatusUseCase {

    GetWorkspaceGatewayStatusResult getWorkspaceGatewayStatus(GetWorkspaceGatewayStatusQuery query);
}
