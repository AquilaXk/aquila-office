package com.aquilaoffice.application.port.in;

import java.util.Locale;

/**
 * 사용자 승인 대기(WAITING_USER) Task를 승인 또는 반려할 때 사용하는 command다.
 */
public record ReviewTaskApprovalCommand(
        Long workspaceId,
        Long taskId,
        String requesterGoogleSub,
        String decision,
        String comment,
        boolean runAfterApprove) {

    public static final String DECISION_APPROVE = "APPROVE";
    public static final String DECISION_REJECT = "REJECT";
    public static final int MAX_COMMENT_LENGTH = 500;

    public ReviewTaskApprovalCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        taskId = requirePositive(taskId, "taskId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        decision = normalizeDecision(decision);
        comment = normalizeOptional(comment);
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("comment must not exceed 500 characters");
        }
    }

    public boolean isApprove() {
        return DECISION_APPROVE.equals(decision);
    }

    private static String normalizeDecision(String value) {
        String normalized = requireNotBlank(value, "decision").toUpperCase(Locale.ROOT);
        if (!DECISION_APPROVE.equals(normalized) && !DECISION_REJECT.equals(normalized)) {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }
        return normalized;
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
