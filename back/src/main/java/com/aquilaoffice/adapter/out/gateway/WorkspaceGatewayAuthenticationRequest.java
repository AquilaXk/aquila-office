package com.aquilaoffice.adapter.out.gateway;

record WorkspaceGatewayAuthenticationRequest(
        Long workspaceId,
        String gatewayUrl,
        String gatewayToken) {}
