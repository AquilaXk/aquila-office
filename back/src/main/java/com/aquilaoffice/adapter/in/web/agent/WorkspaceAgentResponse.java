package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 Agent HTTP 응답 모델이다.
 */
@Schema(description = "워크스페이스 Agent 응답")
public record WorkspaceAgentResponse(
        @Schema(description = "서비스 내부 Agent id", example = "501")
        Long agentId,
        @Schema(description = "워크스페이스 id", example = "101")
        Long workspaceId,
        @Schema(description = "Agent 이름", example = "Backend Agent")
        String name,
        @Schema(description = "Agent 담당 영역", example = "BACKEND")
        WorkspaceAgentCategory category,
        @Schema(description = "Gateway가 발급한 Agent id", example = "oc-agent-1")
        String openClawAgentId,
        @Schema(description = "Agent가 작업할 workspace path", example = ".aquila/workspaces/101")
        String workspacePath,
        @Schema(description = "Agent 동기화 상태", example = "READY")
        WorkspaceAgentStatus status,
        @Schema(description = "동기화 실패 사유", nullable = true)
        String syncError,
        @Schema(description = "생성자 Google sub", example = "google-1")
        String createdByGoogleSub,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @Schema(description = "Agent skill file 동기화 상태")
        List<WorkspaceAgentSkillFileResponse> skillFiles) {

    public WorkspaceAgentResponse {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public static WorkspaceAgentResponse from(CreateWorkspaceAgentResult result) {
        return new WorkspaceAgentResponse(
                result.agentId(),
                result.workspaceId(),
                result.name(),
                result.category(),
                result.openClawAgentId(),
                result.workspacePath(),
                result.status(),
                result.syncError(),
                result.createdByGoogleSub(),
                result.createdAt(),
                result.updatedAt(),
                toSkillFileResponses(result.skillFiles()));
    }

    public static WorkspaceAgentResponse from(ListWorkspaceAgentsResult result) {
        return new WorkspaceAgentResponse(
                result.agentId(),
                result.workspaceId(),
                result.name(),
                result.category(),
                result.openClawAgentId(),
                result.workspacePath(),
                result.status(),
                result.syncError(),
                result.createdByGoogleSub(),
                result.createdAt(),
                result.updatedAt(),
                toSkillFileResponses(result.skillFiles()));
    }

    public static WorkspaceAgentResponse from(GetWorkspaceAgentResult result) {
        return new WorkspaceAgentResponse(
                result.agentId(),
                result.workspaceId(),
                result.name(),
                result.category(),
                result.openClawAgentId(),
                result.workspacePath(),
                result.status(),
                result.syncError(),
                result.createdByGoogleSub(),
                result.createdAt(),
                result.updatedAt(),
                toSkillFileResponses(result.skillFiles()));
    }

    private static List<WorkspaceAgentSkillFileResponse> toSkillFileResponses(
            List<WorkspaceAgentSkillFileResult> skillFiles) {
        return skillFiles.stream()
                .map(WorkspaceAgentSkillFileResponse::from)
                .toList();
    }
}
