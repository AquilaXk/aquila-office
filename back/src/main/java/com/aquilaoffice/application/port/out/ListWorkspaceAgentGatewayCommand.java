package com.aquilaoffice.application.port.out;

/**
 * Gateway에 Agent 목록 조회를 요청할 때 필요한 command다.
 */
public record ListWorkspaceAgentGatewayCommand(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken) {}
