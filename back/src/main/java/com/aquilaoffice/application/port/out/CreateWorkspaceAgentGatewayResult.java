package com.aquilaoffice.application.port.out;

public record CreateWorkspaceAgentGatewayResult(
        boolean success,
        String openClawAgentId,
        String failureReason) {}
