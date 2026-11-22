package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 Agent 목록 항목이다.
 */
public record ListWorkspaceAgentsResult(
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
        List<WorkspaceAgentSkillFileResult> skillFiles,
        Long modelProviderId,
        String modelId,
        String fallbackModelId) {

    public ListWorkspaceAgentsResult(
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
            List<WorkspaceAgentSkillFileResult> skillFiles) {
        this(
                agentId, workspaceId, name, category, openClawAgentId, workspacePath, status,
                syncError, createdByGoogleSub, createdAt, updatedAt, skillFiles, null, null, null);
    }

    public ListWorkspaceAgentsResult {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }
}
