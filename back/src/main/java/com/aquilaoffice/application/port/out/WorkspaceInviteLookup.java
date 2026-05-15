package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * token 조회 결과를 application 계층에 전달하기 위한 초대 projection이다.
 */
public record WorkspaceInviteLookup(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt) {}
