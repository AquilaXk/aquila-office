package com.aquilaoffice.global.config;

import com.aquilaoffice.global.config.properties.AuthTokenProperties;
import com.aquilaoffice.global.config.properties.AuthRefreshCookieProperties;
import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 운영 계열 profile에서는 개발용 인증/암호화 secret으로 부팅되지 않게 차단한다.
 */
@Component
public class AuthTokenRuntimeGuard {

    private static final Set<String> RUNTIME_PROFILES = Set.of("prod", "production", "staging");

    private final AuthTokenProperties authTokenProperties;
    private final ExternalTokenEncryptionProperties externalTokenEncryptionProperties;
    private final AuthRefreshCookieProperties authRefreshCookieProperties;
    private final Environment environment;

    public AuthTokenRuntimeGuard(
            AuthTokenProperties authTokenProperties,
            ExternalTokenEncryptionProperties externalTokenEncryptionProperties,
            AuthRefreshCookieProperties authRefreshCookieProperties,
            Environment environment) {
        this.authTokenProperties = Objects.requireNonNull(authTokenProperties, "authTokenProperties must not be null");
        this.externalTokenEncryptionProperties = Objects.requireNonNull(
                externalTokenEncryptionProperties, "externalTokenEncryptionProperties must not be null");
        this.authRefreshCookieProperties = Objects.requireNonNull(
                authRefreshCookieProperties, "authRefreshCookieProperties must not be null");
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    @PostConstruct
    public void validate() {
        if (!usesRuntimeProfile()) {
            return;
        }
        if (authTokenProperties.usesDefaultDevelopmentSecret()) {
            throw new IllegalStateException(
                    "AQUILA_AUTH_TOKEN_SECRET must be configured for runtime profiles");
        }
        if (externalTokenEncryptionProperties.usesDefaultDevelopmentKey()) {
            throw new IllegalStateException(
                    "AQUILA_EXTERNAL_TOKEN_KEY must be configured for runtime profiles");
        }
        if (externalTokenEncryptionProperties.usesDefaultDevelopmentKeyId()) {
            throw new IllegalStateException(
                    "AQUILA_EXTERNAL_TOKEN_KEY_ID must be configured for runtime profiles");
        }
        if (!authRefreshCookieProperties.secure()) {
            throw new IllegalStateException(
                    "AQUILA_AUTH_REFRESH_COOKIE_SECURE must be true for runtime profiles");
        }
    }

    private boolean usesRuntimeProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(RUNTIME_PROFILES::contains);
    }
}
