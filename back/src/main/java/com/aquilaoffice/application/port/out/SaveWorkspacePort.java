package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;

/**
 * 워크스페이스와 최초 멤버 저장을 application 계층에서 요청하기 위한 outbound port다.
 */
public interface SaveWorkspacePort {

    SavedWorkspace save(Workspace workspace, WorkspaceMember workspaceMember);
}
