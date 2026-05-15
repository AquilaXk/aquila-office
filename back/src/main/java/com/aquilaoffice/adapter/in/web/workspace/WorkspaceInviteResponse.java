package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 생성 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
public record WorkspaceInviteResponse(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt) {

    static WorkspaceInviteResponse from(CreateWorkspaceInviteResult result) {
        return new WorkspaceInviteResponse(
                result.inviteId(),
                result.workspaceId(),
                result.email(),
                result.token(),
                result.status(),
                result.createdAt(),
                result.expiresAt());
    }
}
