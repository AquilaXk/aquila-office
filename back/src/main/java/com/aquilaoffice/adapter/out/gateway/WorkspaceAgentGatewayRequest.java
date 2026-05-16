package com.aquilaoffice.adapter.out.gateway;

record WorkspaceAgentGatewayRequest(
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
