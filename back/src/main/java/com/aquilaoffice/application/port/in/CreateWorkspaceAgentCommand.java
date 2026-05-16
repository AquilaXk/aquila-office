package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.List;

/**
 * 워크스페이스 Agent 생성 요청을 application 계층으로 전달하는 command다.
 */
public record CreateWorkspaceAgentCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String name,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<WorkspaceAgentSkillFileCommand> skillFiles) {

    public CreateWorkspaceAgentCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        name = requireNotBlank(name, "name");
        category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        workspacePath = workspacePath == null ? null : workspacePath.trim();
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
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
}
