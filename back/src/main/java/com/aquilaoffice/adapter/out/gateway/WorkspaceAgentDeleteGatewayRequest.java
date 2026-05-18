package com.aquilaoffice.adapter.out.gateway;

record WorkspaceAgentDeleteGatewayRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken,
        String openClawAgentId) {}
