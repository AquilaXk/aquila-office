package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleCommand;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * 워크스페이스 멤버 역할 변경 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
public record ChangeWorkspaceMemberRoleRequest(@NotNull WorkspaceMemberRole role) {

    ChangeWorkspaceMemberRoleCommand toCommand(Long workspaceId, Long memberId, String requesterGoogleSub) {
        return new ChangeWorkspaceMemberRoleCommand(workspaceId, memberId, requesterGoogleSub, role);
    }
}
