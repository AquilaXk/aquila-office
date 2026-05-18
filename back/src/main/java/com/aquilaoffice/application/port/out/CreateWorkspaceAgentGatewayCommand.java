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
        WorkspaceAgentCategory category,
        String workspacePath,
        List<SkillFile> skillFiles) {

    public CreateWorkspaceAgentGatewayCommand {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public record SkillFile(String fileName, String content) {}
}
