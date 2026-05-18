package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.gateway.GatewayBindingStatus;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;

/**
 * gateway binding 상태 조회 결과를 inbound adapter로 돌려주는 DTO다.
 */
public record GetWorkspaceGatewayStatusResult(
        GatewayBindingStatus status,
        boolean bound,
        Long bindingId,
        String mode,
        String gatewayUrl,
        String maskedToken,
        String deviceId,
        String deviceName,
        String authenticatedAt,
        GatewayConnectionStatus lastStatus,
        String lastCheckedAt,
        String lastError,
        String updatedAt) {}
