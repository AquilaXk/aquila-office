package com.aquilaoffice.adapter.in.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthRefreshCookieFactoryTest {

    @Test
    void issueUsesSecureAttributeWhenConfigured() {
        AuthRefreshCookieFactory factory = new AuthRefreshCookieFactory(Duration.ofDays(14), true);

        String cookie = factory.issue("refresh-token").toString();

        assertThat(cookie).contains("Secure");
    }

    @Test
    void clearUsesSecureAttributeWhenConfigured() {
        AuthRefreshCookieFactory factory = new AuthRefreshCookieFactory(Duration.ofDays(14), true);

        String cookie = factory.clear().toString();

        assertThat(cookie).contains("Secure");
    }
}
