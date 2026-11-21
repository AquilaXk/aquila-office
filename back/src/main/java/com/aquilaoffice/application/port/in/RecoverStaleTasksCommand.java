package com.aquilaoffice.application.port.in;

/**
 * 고착된 실행·Task 복구 기준(분)과 요청자 정보를 전달하는 command다.
 */
public record RecoverStaleTasksCommand(
        Long workspaceId,
        String requesterGoogleSub,
        int staleMinutes) {

    public static final int DEFAULT_STALE_MINUTES = 15;
    public static final int MIN_STALE_MINUTES = 1;
    public static final int MAX_STALE_MINUTES = 1440;

    public static RecoverStaleTasksCommand of(
            Long workspaceId,
            String requesterGoogleSub,
            Integer staleMinutes) {
        return new RecoverStaleTasksCommand(
                workspaceId,
                requesterGoogleSub,
                staleMinutes == null ? DEFAULT_STALE_MINUTES : staleMinutes);
    }

    public RecoverStaleTasksCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        if (staleMinutes < MIN_STALE_MINUTES || staleMinutes > MAX_STALE_MINUTES) {
            throw new IllegalArgumentException("staleMinutes must be between 1 and 1440");
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
}
