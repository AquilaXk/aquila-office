package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.GoogleLoginResult;

/**
 * Google 로그인 성공 후 client에 반환할 인증 응답 DTO다.
 */
public record GoogleLoginResponse(
        Long memberId,
        String name,
        String email,
        String role,
        String accessToken) {

    static GoogleLoginResponse from(GoogleLoginResult result) {
        return new GoogleLoginResponse(
                result.memberId(),
                result.name(),
                result.email(),
                result.role(),
                result.accessToken());
    }
}
