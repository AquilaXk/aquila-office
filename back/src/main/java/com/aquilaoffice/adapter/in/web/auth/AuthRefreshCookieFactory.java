package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import com.aquilaoffice.global.config.properties.AuthRefreshCookieProperties;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh token을 브라우저 JavaScript에서 읽을 수 없는 HttpOnly cookie로 만든다.
 */
@Component
public class AuthRefreshCookieFactory {

    public static final String COOKIE_NAME = "AQUILA_REFRESH_TOKEN";

    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";

    private final Duration refreshTokenMaxAge;
    private final boolean secure;

    @Autowired
    public AuthRefreshCookieFactory(
            AuthTokenProperties authTokenProperties,
            AuthRefreshCookieProperties authRefreshCookieProperties) {
        this(authTokenProperties.refreshTokenTtl(), authRefreshCookieProperties.secure());
    }

    AuthRefreshCookieFactory(Duration refreshTokenMaxAge) {
        this(refreshTokenMaxAge, false);
    }

    AuthRefreshCookieFactory(Duration refreshTokenMaxAge, boolean secure) {
        if (refreshTokenMaxAge == null || refreshTokenMaxAge.isZero() || refreshTokenMaxAge.isNegative()) {
            throw new IllegalArgumentException("refreshTokenMaxAge must be positive");
        }
        this.refreshTokenMaxAge = refreshTokenMaxAge;
        this.secure = secure;
    }

    public ResponseCookie issue(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(refreshTokenMaxAge)
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH);
    }
}
