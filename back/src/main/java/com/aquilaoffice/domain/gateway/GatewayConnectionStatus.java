package com.aquilaoffice.domain.gateway;

/**
 * gateway 연결 테스트 결과 상태다.
 */
public enum GatewayConnectionStatus {
    CONNECTED,
    UNREACHABLE,
    TOKEN_INVALID,
    TIMEOUT,
    PAIRING_REQUIRED,
    FORBIDDEN,
    FAILED
}
