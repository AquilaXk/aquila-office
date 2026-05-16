package com.aquilaoffice.application.port.in;

/**
 * Google 로그인 성공 후 client에 반환할 application 결과다.
 */
public record GoogleLoginResult(
        Long memberId,
        String name,
        String email,
        String role,
        String accessToken,
        String refreshToken) {}
