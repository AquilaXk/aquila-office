package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import com.aquilaoffice.global.config.properties.AuthRefreshCookieProperties;
import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthTokenRuntimeGuardTest {

    @Test
    void allowsDefaultSecretOutsideRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties(AuthTokenProperties.DEFAULT_DEVELOPMENT_SECRET),
                externalTokenProperties(null, null),
                refreshCookieProperties(false),
                new MockEnvironment().withProperty("spring.profiles.active", "dev"));

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsDefaultSecretInRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties(AuthTokenProperties.DEFAULT_DEVELOPMENT_SECRET),
                externalTokenProperties("runtime-external-token-key-0123456789abcdef", "runtime"),
                refreshCookieProperties(true),
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AQUILA_AUTH_TOKEN_SECRET");
    }

    @Test
    void rejectsDefaultExternalTokenKeyInRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties("runtime-jwt-secret-0123456789abcdef-runtime-only"),
                externalTokenProperties(null, "runtime"),
                refreshCookieProperties(true),
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AQUILA_EXTERNAL_TOKEN_KEY");
    }

    @Test
    void rejectsDefaultExternalTokenKeyIdInRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties("runtime-jwt-secret-0123456789abcdef-runtime-only"),
                externalTokenProperties("runtime-external-token-key-0123456789abcdef", null),
                refreshCookieProperties(true),
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AQUILA_EXTERNAL_TOKEN_KEY_ID");
    }

    @Test
    void rejectsInsecureRefreshCookieInRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties("runtime-jwt-secret-0123456789abcdef-runtime-only"),
                externalTokenProperties("runtime-external-token-key-0123456789abcdef", "runtime"),
                refreshCookieProperties(false),
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AQUILA_AUTH_REFRESH_COOKIE_SECURE");
    }

    @Test
    void allowsCustomSecretsInRuntimeProfiles() {
        AuthTokenRuntimeGuard guard = new AuthTokenRuntimeGuard(
                properties("runtime-jwt-secret-0123456789abcdef-runtime-only"),
                externalTokenProperties("runtime-external-token-key-0123456789abcdef", "runtime"),
                refreshCookieProperties(true),
                new MockEnvironment().withProperty("spring.profiles.active", "staging"));

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    private AuthTokenProperties properties(String secret) {
        return new AuthTokenProperties(
                "aquila-office",
                secret,
                Duration.ofMinutes(15),
                Duration.ofDays(14));
    }

    private ExternalTokenEncryptionProperties externalTokenProperties(String key, String keyId) {
        return new ExternalTokenEncryptionProperties(key, keyId);
    }

    private AuthRefreshCookieProperties refreshCookieProperties(boolean secure) {
        return new AuthRefreshCookieProperties(secure);
    }
}
