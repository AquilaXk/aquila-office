package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 초대 연장 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record ExtendWorkspaceInviteCommand(
        Long workspaceId,
        Long inviteId,
        String requesterGoogleSub,
        Integer additionalDays) {

    public ExtendWorkspaceInviteCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        inviteId = requirePositive(inviteId, "inviteId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        if (additionalDays != null && (additionalDays < 1 || additionalDays > 30)) {
            throw new IllegalArgumentException("additionalDays must be between 1 and 30");
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

