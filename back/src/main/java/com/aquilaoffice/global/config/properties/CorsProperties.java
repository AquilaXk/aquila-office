package com.aquilaoffice.global.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "custom.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS =
            List.of("http://localhost:3000", "http://localhost:5173");

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = DEFAULT_ALLOWED_ORIGIN_PATTERNS;
        } else {
            allowedOriginPatterns = List.copyOf(allowedOriginPatterns);
        }
    }
}
