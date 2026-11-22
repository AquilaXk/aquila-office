package com.aquilaoffice.domain.model;

/**
 * 모델 공급자 연결 테스트 상태 코드다.
 */
public enum ModelProviderConnectionStatus {
    CONNECTED,
    UNREACHABLE,
    TOKEN_INVALID,
    TIMEOUT,
    RATE_LIMITED,
    FAILED
}
