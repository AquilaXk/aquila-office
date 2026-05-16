package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 나가기 inbound port다.
 */
public interface LeaveWorkspaceUseCase {

    void leaveWorkspace(LeaveWorkspaceCommand command);
}
