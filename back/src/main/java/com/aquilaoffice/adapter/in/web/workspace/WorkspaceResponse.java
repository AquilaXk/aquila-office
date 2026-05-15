package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 생성 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
public record WorkspaceResponse(
        Long workspaceId,
        String name,
        String description,
        String creatorGoogleSub,
        WorkspaceMemberRole myRole,
        LocalDateTime joinedAt) {

    static WorkspaceResponse from(CreateWorkspaceResult result) {
        // application result의 필드명을 API 응답 계약에 맞춰 그대로 매핑한다.
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt());
    }

    static WorkspaceResponse from(ListWorkspacesResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt());
    }

    static WorkspaceResponse from(GetWorkspaceDetailResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt());
    }

    static WorkspaceResponse from(UpdateWorkspaceResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt());
    }
}
