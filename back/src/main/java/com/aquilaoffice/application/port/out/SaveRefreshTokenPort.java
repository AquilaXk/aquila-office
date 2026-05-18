package com.aquilaoffice.application.port.out;

import java.time.Instant;

/**
 * 로그인 성공 시 회원의 refresh token 저장값을 생성하거나 교체하는 outbound port다.
 */
public interface SaveRefreshTokenPort {

    void saveOrRotate(Long memberId, String refreshToken, Instant refreshTokenExpiresAt);
}
