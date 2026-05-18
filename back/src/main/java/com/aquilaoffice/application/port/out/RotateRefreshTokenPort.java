package com.aquilaoffice.application.port.out;

import java.time.Instant;

/**
 * 저장된 refresh token이 현재 요청 token과 일치할 때만 새 token으로 교체하는 outbound port다.
 */
public interface RotateRefreshTokenPort {

    boolean rotateIfMatch(
            Long memberId,
            String currentRefreshToken,
            String newRefreshToken,
            Instant newRefreshTokenExpiresAt,
            Instant now);
}
