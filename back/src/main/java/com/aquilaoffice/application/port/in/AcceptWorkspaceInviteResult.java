package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 초대 수락 처리 후 adapter에 반환할 application 계층 결과값이다.
 */
public record AcceptWorkspaceInviteResult(
        Long inviteId,
        Long workspaceId,
        String memberGoogleSub,
        WorkspaceMemberRole role,
        LocalDateTime acceptedAt) {}
