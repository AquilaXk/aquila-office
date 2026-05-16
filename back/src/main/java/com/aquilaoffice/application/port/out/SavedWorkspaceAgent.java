package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * persistence adapter가 저장/조회한 Agent 스냅샷이다.
 */
public record SavedWorkspaceAgent(
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
        List<SavedWorkspaceAgentSkillFile> skillFiles) {

    public SavedWorkspaceAgent {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }
}
