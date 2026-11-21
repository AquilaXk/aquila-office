package com.aquilaoffice.adapter.out.gateway;

record WorkspaceAgentListGatewayRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken) {}
