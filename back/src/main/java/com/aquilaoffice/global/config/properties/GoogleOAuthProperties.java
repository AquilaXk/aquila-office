package com.aquilaoffice.global.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google ID token 검증에 필요한 OAuth client와 검증 정책 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.google-oauth")
public record GoogleOAuthProperties(
        List<String> clientIds,
        List<String> allowedHostedDomains,
        boolean requireEmailVerified,
        String applicationName) {

    private static final List<String> DEFAULT_CLIENT_IDS =
            List.of("development-client-id.apps.googleusercontent.com");
    private static final String DEFAULT_APPLICATION_NAME = "aquila-office";

    public GoogleOAuthProperties {
        clientIds = normalizeList(clientIds, DEFAULT_CLIENT_IDS, "clientIds");
        allowedHostedDomains = normalizeList(allowedHostedDomains, List.of(), "allowedHostedDomains");
        applicationName = defaultIfBlank(applicationName, DEFAULT_APPLICATION_NAME);
    }

    private static List<String> normalizeList(List<String> values, List<String> defaults, String fieldName) {
        List<String> resolvedValues = values == null || values.isEmpty() ? defaults : values;
        List<String> normalizedValues = resolvedValues.stream()
                .map(value -> defaultIfBlank(value, ""))
                .filter(value -> !value.isBlank())
                .toList();
        if (normalizedValues.isEmpty() && !defaults.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return List.copyOf(normalizedValues);
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
