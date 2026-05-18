package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.List;

record WorkspaceAgentCreateGatewayRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String name,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<SkillFile> skillFiles) {

    WorkspaceAgentCreateGatewayRequest {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    record SkillFile(String fileName, String content) {}
}
