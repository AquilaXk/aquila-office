package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 생성 기능을 외부 입력 어댑터에 공개하는 inbound port다.
 */
public interface CreateWorkspaceUseCase {

    CreateWorkspaceResult createWorkspace(CreateWorkspaceCommand command);
}
