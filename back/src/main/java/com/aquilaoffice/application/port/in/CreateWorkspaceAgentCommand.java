package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import java.util.List;

/**
 * 워크스페이스 Agent 생성 요청을 application 계층으로 전달하는 command다.
 */
public record CreateWorkspaceAgentCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String name,
        String emoji,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<WorkspaceAgentSkillFileCommand> skillFiles) {

    public CreateWorkspaceAgentCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        name = requireNotBlank(name, "name");
        emoji = normalizeOptional(emoji);
        category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        workspacePath = workspacePath == null ? null : workspacePath.trim();
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
        if (skillFiles.size() > WorkspaceAgentSkillFile.MAX_SKILL_FILES) {
            throw new IllegalArgumentException("skillFiles must not exceed 20 entries");
        }
    }

    public CreateWorkspaceAgentCommand(
            Long workspaceId,
            String requesterGoogleSub,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            List<WorkspaceAgentSkillFileCommand> skillFiles) {
        this(workspaceId, requesterGoogleSub, name, null, category, workspacePath, skillFiles);
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
