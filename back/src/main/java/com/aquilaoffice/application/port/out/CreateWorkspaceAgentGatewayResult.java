package com.aquilaoffice.application.port.out;

import java.util.List;

public record CreateWorkspaceAgentGatewayResult(
        boolean success,
        String openClawAgentId,
        String failureReason,
        List<SkillFileResult> skillFiles) {

    public CreateWorkspaceAgentGatewayResult(
            boolean success,
            String openClawAgentId,
            String failureReason) {
        this(success, openClawAgentId, failureReason, List.of());
    }

    public CreateWorkspaceAgentGatewayResult {
        skillFiles = skillFiles == null ? List.of() : List.copyOf(skillFiles);
    }

    public record SkillFileResult(String fileName, boolean success, String failureReason) {}
}
