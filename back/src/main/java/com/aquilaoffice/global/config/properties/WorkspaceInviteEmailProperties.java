package com.aquilaoffice.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 워크스페이스 초대 메일 발송과 초대 수락 URL 구성에 필요한 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.invite-email")
public record WorkspaceInviteEmailProperties(
        boolean enabled,
        String from,
        String acceptUrlBase) {

    private static final String DEFAULT_FROM = "noreply@aquila-office.local";
    private static final String DEFAULT_ACCEPT_URL_BASE = "http://localhost:3000/invites/accept";

    public WorkspaceInviteEmailProperties {
        if (enabled) {
            from = requireNotBlank(from, "from");
            acceptUrlBase = trimTrailingSlash(requireNotBlank(acceptUrlBase, "acceptUrlBase"));
        } else {
            from = defaultIfBlank(from, DEFAULT_FROM);
            acceptUrlBase = trimTrailingSlash(defaultIfBlank(acceptUrlBase, DEFAULT_ACCEPT_URL_BASE));
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
