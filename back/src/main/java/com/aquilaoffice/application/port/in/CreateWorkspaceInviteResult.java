package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 생성 처리 후 web adapter에 반환할 application 계층 결과값이다.
 */
public record CreateWorkspaceInviteResult(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt) {}
