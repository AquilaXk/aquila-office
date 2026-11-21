package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 멤버 제거 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record RemoveWorkspaceMemberCommand(Long workspaceId, Long memberId, String requesterGoogleSub) {

    public RemoveWorkspaceMemberCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        memberId = requirePositive(memberId, "memberId");
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
