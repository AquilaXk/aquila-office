package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 멤버 목록 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
public record WorkspaceMemberResponse(
        Long memberId,
        String memberGoogleSub,
        String email,
        String name,
        WorkspaceMemberRole role,
        LocalDateTime joinedAt) {

    static WorkspaceMemberResponse from(ListWorkspaceMembersResult result) {
        return new WorkspaceMemberResponse(
                result.memberId(),
                result.memberGoogleSub(),
                result.email(),
                result.name(),
                result.role(),
                result.joinedAt());
    }
}
