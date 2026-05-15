package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

public record WorkspaceResponse(
        Long workspaceId,
        String name,
        String description,
        String creatorGoogleSub,
        WorkspaceMemberRole myRole,
        LocalDateTime joinedAt) {

    static WorkspaceResponse from(CreateWorkspaceResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt());
    }
}
