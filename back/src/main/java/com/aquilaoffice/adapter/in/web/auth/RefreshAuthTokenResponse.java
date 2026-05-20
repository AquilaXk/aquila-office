package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.RefreshAuthTokenResult;

/**
 * Refresh token 재발급 성공 후 client에 반환할 인증 응답 DTO다.
 */
public record RefreshAuthTokenResponse(String accessToken) {

    static RefreshAuthTokenResponse from(RefreshAuthTokenResult result) {
        return new RefreshAuthTokenResponse(result.accessToken());
    }
}
