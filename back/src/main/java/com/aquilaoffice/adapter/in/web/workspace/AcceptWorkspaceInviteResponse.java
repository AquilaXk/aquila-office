package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteResult;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 수락 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
public record AcceptWorkspaceInviteResponse(
        Long inviteId,
        Long workspaceId,
        String memberGoogleSub,
        WorkspaceMemberRole role,
        LocalDateTime acceptedAt) {

    static AcceptWorkspaceInviteResponse from(AcceptWorkspaceInviteResult result) {
        return new AcceptWorkspaceInviteResponse(
                result.inviteId(),
                result.workspaceId(),
                result.memberGoogleSub(),
                result.role(),
                result.acceptedAt());
    }
}
