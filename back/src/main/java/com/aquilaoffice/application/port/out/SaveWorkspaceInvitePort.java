package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceInvite;

/**
 * application 계층이 워크스페이스 초대 저장 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface SaveWorkspaceInvitePort {

    SavedWorkspaceInvite save(Long workspaceId, WorkspaceInvite invite);
}
