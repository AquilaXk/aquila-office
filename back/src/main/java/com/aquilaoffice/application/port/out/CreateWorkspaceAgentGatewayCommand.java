package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.List;

/**
 * Gateway에 Agent 생성을 요청할 때 필요한 command다.
 */
public record CreateWorkspaceAgentGatewayCommand(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String name,
        String emoji,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<SkillFile> skillFiles) {

    public CreateWorkspaceAgentGatewayCommand {
        emoji = normalizeOptional(emoji);
        category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public CreateWorkspaceAgentGatewayCommand(
            Long workspaceId,
            String gatewayUrl,
            String gatewayToken,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            List<SkillFile> skillFiles) {
        this(workspaceId, gatewayUrl, gatewayToken, name, null, category, workspacePath, skillFiles);
    }

    public record SkillFile(String fileName, String content) {}

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
