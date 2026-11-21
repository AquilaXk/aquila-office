package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 상세 조회 결과를 adapter와 독립된 application 값으로 표현한다.
 */
public record GetWorkspaceDetailResult(
        Long workspaceId,
        String name,
        String description,
        String creatorGoogleSub,
        WorkspaceMemberRole myRole,
        LocalDateTime joinedAt,
        Integer dailyTokenLimit) {

    public GetWorkspaceDetailResult(
            Long workspaceId,
            String name,
            String description,
            String creatorGoogleSub,
            WorkspaceMemberRole myRole,
            LocalDateTime joinedAt) {
        this(workspaceId, name, description, creatorGoogleSub, myRole, joinedAt, null);
    }
}
