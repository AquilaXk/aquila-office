package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import java.util.List;

/**
 * 워크스페이스 Agent 메타데이터와 skill 파일 수정 요청을 application 계층으로 전달하는 command다.
 */
public record UpdateWorkspaceAgentCommand(
        Long workspaceId,
        Long agentId,
        String requesterGoogleSub,
        String name,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<WorkspaceAgentSkillFileCommand> skillFiles,
        Long modelProviderId,
        String modelId,
        String fallbackModelId,
        boolean updateModelConfig) {

    public UpdateWorkspaceAgentCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        agentId = requirePositive(agentId, "agentId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        name = requireNotBlank(name, "name");
        workspacePath = workspacePath == null ? null : workspacePath.trim();
        if (skillFiles != null) {
            skillFiles = List.copyOf(skillFiles);
            if (skillFiles.size() > WorkspaceAgentSkillFile.MAX_SKILL_FILES) {
                throw new IllegalArgumentException("skillFiles must not exceed 20 entries");
            }
        }
        if (modelProviderId != null && modelProviderId <= 0) {
            throw new IllegalArgumentException("modelProviderId must be positive");
        }
        modelId = normalizeOptional(modelId);
        fallbackModelId = normalizeOptional(fallbackModelId);
    }

    public UpdateWorkspaceAgentCommand(
            Long workspaceId,
            Long agentId,
            String requesterGoogleSub,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            List<WorkspaceAgentSkillFileCommand> skillFiles) {
        this(workspaceId, agentId, requesterGoogleSub, name, category, workspacePath, skillFiles,
                null, null, null, false);
    }

    public UpdateWorkspaceAgentCommand(
            Long workspaceId,
            Long agentId,
            String requesterGoogleSub,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            List<WorkspaceAgentSkillFileCommand> skillFiles,
            Long modelProviderId,
            String modelId,
            String fallbackModelId) {
        this(workspaceId, agentId, requesterGoogleSub, name, category, workspacePath, skillFiles,
                modelProviderId, modelId, fallbackModelId,
                modelProviderId != null || modelId != null || fallbackModelId != null);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
