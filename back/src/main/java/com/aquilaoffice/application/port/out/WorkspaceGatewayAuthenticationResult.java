package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

/**
 * Gateway 장치 challenge/connect 인증 결과다.
 */
public record WorkspaceGatewayAuthenticationResult(
        GatewayConnectionStatus status,
        boolean connected,
        String gatewayUrl,
        String deviceId,
        String deviceName,
        String message,
        int agentCount,
        String failureReason) {}
