package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.List;

record WorkspaceAgentCreateGatewayRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String name,
        String emoji,
        WorkspaceAgentCategory category,
        String workspacePath,
        List<SkillFile> skillFiles) {

    WorkspaceAgentCreateGatewayRequest {
        emoji = normalizeOptional(emoji);
        category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    WorkspaceAgentCreateGatewayRequest(
            Long workspaceId,
            String gatewayUrl,
            String gatewayToken,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            List<SkillFile> skillFiles) {
        this(workspaceId, gatewayUrl, gatewayToken, name, null, category, workspacePath, skillFiles);
    }

    record SkillFile(String fileName, String content) {}

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
