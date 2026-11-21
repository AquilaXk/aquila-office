package com.aquilaoffice.application.port.out;

public record WorkspaceAgentRunCommand(
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        String openClawAgentId,
        String workspacePath,
        String gatewayUrl,
        String gatewayToken,
        String sessionKey,
        String prompt,
        boolean createPr) {}
