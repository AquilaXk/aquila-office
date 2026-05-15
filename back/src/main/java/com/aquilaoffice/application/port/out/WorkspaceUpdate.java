package com.aquilaoffice.application.port.out;

/**
 * persistence adapter가 워크스페이스 정보 수정 상태 전이를 저장할 때 사용하는 요청값이다.
 */
public record WorkspaceUpdate(Long workspaceId, String name, String description) {

    public WorkspaceUpdate {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        name = requireNotBlank(name, "name");
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
