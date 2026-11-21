package com.aquilaoffice.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * refresh token cookie의 브라우저 전송 보안 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.auth-refresh-cookie")
public record AuthRefreshCookieProperties(boolean secure) {}
