package com.aquilaoffice.application.port.out;

public record DeleteWorkspaceAgentGatewayResult(
        boolean success,
        String failureReason) {}
