package com.aquilaoffice.adapter.out.gateway;

import java.util.List;

record WorkspaceAgentSyncSkillFilesGatewayRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String openClawAgentId,
        List<WorkspaceAgentCreateGatewayRequest.SkillFile> skillFiles) {

    WorkspaceAgentSyncSkillFilesGatewayRequest {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }
}
