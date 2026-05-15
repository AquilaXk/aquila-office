package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * persistence adapter가 워크스페이스 초대 목록 조회 결과를 application 계층에 전달하기 위한 projection이다.
 */
public record WorkspaceInviteManagement(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt) {}
