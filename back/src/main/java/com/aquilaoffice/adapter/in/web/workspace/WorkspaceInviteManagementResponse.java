package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * 워크스페이스 관리자용 초대 목록 항목을 클라이언트 응답 형태로 노출하는 DTO다.
 */
public record WorkspaceInviteManagementResponse(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt) {

    static WorkspaceInviteManagementResponse from(ListWorkspaceInvitesResult result) {
        return new WorkspaceInviteManagementResponse(
                result.inviteId(),
                result.workspaceId(),
                result.email(),
                result.token(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                result.acceptedAt(),
                result.revokedAt());
    }

    static WorkspaceInviteManagementResponse from(ExtendWorkspaceInviteResult result) {
        return new WorkspaceInviteManagementResponse(
                result.inviteId(),
                result.workspaceId(),
                result.email(),
                result.token(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                result.acceptedAt(),
                result.revokedAt());
    }
}
