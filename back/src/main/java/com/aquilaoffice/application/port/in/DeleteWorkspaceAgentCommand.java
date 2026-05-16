package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 Agent 비활성화 command다.
 */
public record DeleteWorkspaceAgentCommand(Long workspaceId, Long agentId, String requesterGoogleSub) {

    public DeleteWorkspaceAgentCommand {
        workspaceId = requirePositive(workspaceId, "workspaceId");
        agentId = requirePositive(agentId, "agentId");
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
