package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 정보 수정 처리 후 web adapter에 반환할 application 계층 결과값이다.
 */
public record UpdateWorkspaceResult(
        Long workspaceId,
        String name,
        String description,
        String creatorGoogleSub,
        WorkspaceMemberRole myRole,
        LocalDateTime joinedAt,
        Integer dailyTokenLimit) {

    public UpdateWorkspaceResult(
            Long workspaceId,
            String name,
            String description,
            String creatorGoogleSub,
            WorkspaceMemberRole myRole,
            LocalDateTime joinedAt) {
        this(workspaceId, name, description, creatorGoogleSub, myRole, joinedAt, null);
    }
}
