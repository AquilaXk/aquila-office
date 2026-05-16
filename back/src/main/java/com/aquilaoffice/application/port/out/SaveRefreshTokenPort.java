package com.aquilaoffice.application.port.out;

/**
 * 로그인 성공 시 회원의 refresh token 저장값을 생성하거나 교체하는 outbound port다.
 */
public interface SaveRefreshTokenPort {

    void saveOrRotate(Long memberId, String refreshToken);
}
