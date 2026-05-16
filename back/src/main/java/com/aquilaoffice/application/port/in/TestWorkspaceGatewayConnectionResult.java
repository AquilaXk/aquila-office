package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

/**
 * gateway 연결 테스트 결과를 inbound adapter로 돌려주는 DTO다.
 */
public record TestWorkspaceGatewayConnectionResult(
        GatewayConnectionStatus status,
        boolean connected,
        String gatewayUrl,
        String message,
        int agentCount) {}
