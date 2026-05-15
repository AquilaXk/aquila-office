package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * persistence adapter가 초대 수락 처리 후 application 계층에 돌려주는 결과값이다.
 */
public record AcceptedWorkspaceInvite(
        Long inviteId,
        Long workspaceId,
        String memberGoogleSub,
        WorkspaceMemberRole role,
        LocalDateTime acceptedAt) {}
