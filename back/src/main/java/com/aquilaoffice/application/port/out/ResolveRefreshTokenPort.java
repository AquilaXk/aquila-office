package com.aquilaoffice.application.port.out;

/**
 * Refresh token 문자열에서 재발급에 필요한 subject 정보를 추출하는 outbound port다.
 */
public interface ResolveRefreshTokenPort {

    ResolvedRefreshToken resolve(String refreshToken);
}
