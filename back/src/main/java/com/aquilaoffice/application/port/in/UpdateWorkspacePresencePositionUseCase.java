package com.aquilaoffice.application.port.in;

/**
 * 현재 사용자의 워크스페이스 presence 위치를 갱신하는 inbound port다.
 */
public interface UpdateWorkspacePresencePositionUseCase {

    void updatePosition(UpdateWorkspacePresencePositionCommand command);
}
