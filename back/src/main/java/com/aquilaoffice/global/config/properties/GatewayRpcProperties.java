package com.aquilaoffice.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway WebSocket RPC 연결 경로와 요청 timeout 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.gateway-rpc")
public record GatewayRpcProperties(
        Duration requestTimeout,
        String path) {

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_PATH = "/rpc";

    public GatewayRpcProperties {
        requestTimeout = defaultIfNull(requestTimeout, DEFAULT_REQUEST_TIMEOUT);
        path = normalizePath(path);
    }

    private static Duration defaultIfNull(Duration value, Duration defaultValue) {
        Duration resolvedValue = value == null ? defaultValue : value;
        if (resolvedValue.isZero() || resolvedValue.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        return resolvedValue;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PATH;
        }
        String normalizedValue = value.trim();
        if (!normalizedValue.startsWith("/")) {
            normalizedValue = "/" + normalizedValue;
        }
        while (normalizedValue.length() > 1 && normalizedValue.endsWith("/")) {
            normalizedValue = normalizedValue.substring(0, normalizedValue.length() - 1);
        }
        return normalizedValue;
    }
}
