package com.aquilaoffice.application.port.in;

/**
 * Task를 사용자 승인 대기(WAITING_USER) 상태로 전환할 때 사용하는 command다.
 */
public record RequestTaskApprovalCommand(
        Long workspaceId,
        Long taskId,
        String requesterGoogleSub,
        String reason) {

    public static final int MAX_REASON_LENGTH = 500;

    public RequestTaskApprovalCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        taskId = requirePositive(taskId, "taskId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        reason = normalizeOptional(reason);
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("reason must not exceed 500 characters");
        }
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
