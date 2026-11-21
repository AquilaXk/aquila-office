package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.util.List;

/**
 * application 계층이 워크스페이스 초대 목록 저장소 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface LoadWorkspaceInvitesPort {

    List<WorkspaceInviteManagement> findWorkspaceInvites(Long workspaceId, WorkspaceInviteStatus status);
}
