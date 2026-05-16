package com.aquilaoffice.application.port.in;

/**
 * gateway binding 등록 결과를 inbound adapter로 돌려주는 DTO다.
 */
public record CreateWorkspaceGatewayBindingResult(
        Long id,
        Long workspaceId,
        String mode,
        String gatewayUrl,
        String maskedToken) {}
