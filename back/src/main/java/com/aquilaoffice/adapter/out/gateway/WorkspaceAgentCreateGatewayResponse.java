package com.aquilaoffice.adapter.out.gateway;

import java.util.List;

record WorkspaceAgentCreateGatewayResponse(
        boolean success,
        String openClawAgentId,
        String failureReason,
        List<SkillFileResult> skillFiles) {

    WorkspaceAgentCreateGatewayResponse(
            boolean success,
            String openClawAgentId,
            String failureReason) {
        this(success, openClawAgentId, failureReason, List.of());
    }

    WorkspaceAgentCreateGatewayResponse {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    record SkillFileResult(String fileName, boolean success, String failureReason) {}
}
