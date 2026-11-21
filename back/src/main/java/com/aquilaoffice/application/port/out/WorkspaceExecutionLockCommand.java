package com.aquilaoffice.application.port.out;

public record WorkspaceExecutionLockCommand(
        Long workspaceId,
        Long taskId,
        Long agentId,
        String workspacePath) {

    public WorkspaceExecutionLockCommand {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        if (agentId == null || agentId <= 0) {
            throw new IllegalArgumentException("agentId must be positive");
        }
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("workspacePath must not be blank");
        }
        workspacePath = workspacePath.trim();
    }
}
