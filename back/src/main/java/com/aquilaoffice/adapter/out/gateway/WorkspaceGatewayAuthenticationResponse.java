package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

record WorkspaceGatewayAuthenticationResponse(
        GatewayConnectionStatus status,
        boolean connected,
        String gatewayUrl,
        String deviceId,
        String deviceName,
        String message,
        int agentCount,
        String failureReason) {}
