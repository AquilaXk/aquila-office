package com.aquilaoffice.global.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 origin 패턴을 외부 설정에서 읽어오는 properties 객체다.
 */
@ConfigurationProperties(prefix = "custom.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS =
            List.of("http://localhost:3000", "http://localhost:5173");

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            // 로컬 개발 기본값을 제공해 별도 설정 없이 프론트 개발 서버와 연동할 수 있게 한다.
            allowedOriginPatterns = DEFAULT_ALLOWED_ORIGIN_PATTERNS;
        } else {
            allowedOriginPatterns = List.copyOf(allowedOriginPatterns);
        }
    }
}
