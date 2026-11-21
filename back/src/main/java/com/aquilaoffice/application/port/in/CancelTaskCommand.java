package com.aquilaoffice.application.port.in;

/**
 * 실행 중이거나 대기 중인 Task 취소를 요청할 때 사용하는 command다.
 */
public record CancelTaskCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {

    public CancelTaskCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        taskId = requirePositive(taskId, "taskId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
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
