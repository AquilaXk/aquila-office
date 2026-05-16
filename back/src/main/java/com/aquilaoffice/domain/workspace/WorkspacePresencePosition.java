package com.aquilaoffice.domain.workspace;

/**
 * 워크스페이스 presence에서 공유할 화면 위치 값을 표현한다.
 */
public record WorkspacePresencePosition(String left, String top) {

    public WorkspacePresencePosition {
        left = requireLimitedText(left, "left");
        top = requireLimitedText(top, "top");
    }

    private static String requireLimitedText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 30) {
            throw new IllegalArgumentException(fieldName + " must be 30 characters or less");
        }
        return trimmed;
    }
}
