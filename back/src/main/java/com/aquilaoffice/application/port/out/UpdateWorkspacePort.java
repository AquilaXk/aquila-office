package com.aquilaoffice.application.port.out;

/**
 * application 계층이 워크스페이스 정보 수정 저장 방식에 의존하지 않도록 분리한 outbound port다.
 */
public interface UpdateWorkspacePort {

    UpdatedWorkspace update(WorkspaceUpdate update);
}
