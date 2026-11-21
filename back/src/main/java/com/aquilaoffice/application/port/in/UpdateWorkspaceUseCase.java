package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 정보 수정 inbound port다.
 */
public interface UpdateWorkspaceUseCase {

    UpdateWorkspaceResult updateWorkspace(UpdateWorkspaceCommand command);
}
