package com.aquilaoffice.application.port.out;

/**
 * 저장된 refresh token이 현재 요청 token과 일치할 때만 삭제하는 outbound port다.
 */
public interface DeleteRefreshTokenPort {

    boolean deleteIfMatch(Long memberId, String refreshToken);
}
