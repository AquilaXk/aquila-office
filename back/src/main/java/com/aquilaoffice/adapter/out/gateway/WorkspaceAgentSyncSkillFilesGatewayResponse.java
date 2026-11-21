package com.aquilaoffice.adapter.out.gateway;

import java.util.List;

record WorkspaceAgentSyncSkillFilesGatewayResponse(
        boolean success,
        String failureReason,
        List<WorkspaceAgentCreateGatewayResponse.SkillFileResult> skillFiles) {

    WorkspaceAgentSyncSkillFilesGatewayResponse {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }
}
