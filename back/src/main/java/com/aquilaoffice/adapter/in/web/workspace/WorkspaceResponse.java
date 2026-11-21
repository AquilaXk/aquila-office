package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 워크스페이스 생성 결과를 클라이언트 응답 형태로 노출하는 DTO다.
 */
@Schema(description = "워크스페이스 응답")
public record WorkspaceResponse(
        Long workspaceId,
        String name,
        String description,
        String creatorGoogleSub,
        WorkspaceMemberRole myRole,
        LocalDateTime joinedAt,
        @Schema(description = "일일 토큰 예산 상한 (null이면 무제한)", nullable = true)
        Integer dailyTokenLimit) {

    public WorkspaceResponse(
            Long workspaceId,
            String name,
            String description,
            String creatorGoogleSub,
            WorkspaceMemberRole myRole,
            LocalDateTime joinedAt) {
        this(workspaceId, name, description, creatorGoogleSub, myRole, joinedAt, null);
    }

    static WorkspaceResponse from(CreateWorkspaceResult result) {
        // application result의 필드명을 API 응답 계약에 맞춰 그대로 매핑한다.
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt(),
                result.dailyTokenLimit());
    }

    static WorkspaceResponse from(ListWorkspacesResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt(),
                result.dailyTokenLimit());
    }

    static WorkspaceResponse from(GetWorkspaceDetailResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt(),
                result.dailyTokenLimit());
    }

    static WorkspaceResponse from(UpdateWorkspaceResult result) {
        return new WorkspaceResponse(
                result.workspaceId(),
                result.name(),
                result.description(),
                result.creatorGoogleSub(),
                result.myRole(),
                result.joinedAt(),
                result.dailyTokenLimit());
    }
}
