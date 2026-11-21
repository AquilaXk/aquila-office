package com.aquilaoffice.application.port.in;

/**
 * inbound adapter가 워크스페이스 정보 수정 요청을 application 계층으로 넘길 때 사용하는 command다.
 */
public record UpdateWorkspaceCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String name,
        String description,
        Integer dailyTokenLimit) {

    public UpdateWorkspaceCommand(
            Long workspaceId,
            String requesterGoogleSub,
            String name,
            String description) {
        this(workspaceId, requesterGoogleSub, name, description, null);
    }

    public UpdateWorkspaceCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        requesterGoogleSub = requireNotBlank(requesterGoogleSub, "requesterGoogleSub");
        name = requireNotBlank(name, "name");
        dailyTokenLimit = normalizeDailyTokenLimit(dailyTokenLimit);
    }

    private static Integer normalizeDailyTokenLimit(Integer dailyTokenLimit) {
        if (dailyTokenLimit == null || dailyTokenLimit <= 0) {
            return null;
        }
        return dailyTokenLimit;
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
