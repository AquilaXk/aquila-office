package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 Agent HTTP 응답 모델이다.
 */
public record WorkspaceAgentResponse(
        Long agentId,
        Long workspaceId,
        String name,
        WorkspaceAgentCategory category,
        String openClawAgentId,
        String workspacePath,
        WorkspaceAgentStatus status,
        String syncError,
        String createdByGoogleSub,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
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
