package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 목록 조회 결과를 adapter와 독립된 application 값으로 표현한다.
 */
public record ListWorkspaceInvitesResult(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt) {}
