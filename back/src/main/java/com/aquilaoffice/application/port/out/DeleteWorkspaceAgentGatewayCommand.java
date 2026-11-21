package com.aquilaoffice.application.port.out;

/**
 * Gateway에서 Agent를 제거할 때 필요한 command다.
 */
public record DeleteWorkspaceAgentGatewayCommand(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String openClawAgentId) {}
