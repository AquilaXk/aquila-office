package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkspaceInviteResponse(
        Long inviteId,
        Long workspaceId,
        String email,
        String token,
        WorkspaceInviteStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt) {

    static WorkspaceInviteResponse from(CreateWorkspaceInviteResult result) {
        return new WorkspaceInviteResponse(
                result.inviteId(),
                result.workspaceId(),
                result.email(),
                result.token(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                null);
    }

    static WorkspaceInviteResponse from(GetWorkspaceInviteResult result) {
        return new WorkspaceInviteResponse(
                result.inviteId(),
                result.workspaceId(),
                result.email(),
                result.token(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                result.acceptedAt());
    }
}
