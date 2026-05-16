package com.aquilaoffice.domain.agent;

/**
 * 워크스페이스에서 작업을 수행하는 Agent의 메타데이터와 동기화 상태를 관리한다.
 */
public class WorkspaceAgent {

    private final Long workspaceId;
    private final String name;
    private final WorkspaceAgentCategory category;
    private String openClawAgentId;
    private final String workspacePath;
    private WorkspaceAgentStatus status;
    private String syncError;
    private final String createdByGoogleSub;

    private WorkspaceAgent(
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            String createdByGoogleSub) {
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.name = requireNotBlank(name, "name");
        this.category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        this.workspacePath = normalizeWorkspacePath(workspacePath, this.workspaceId);
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.status = WorkspaceAgentStatus.CREATING;
    }

    public static WorkspaceAgent create(
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            String createdByGoogleSub) {
        return new WorkspaceAgent(workspaceId, name, category, workspacePath, createdByGoogleSub);
    }

    public void markReady(String openClawAgentId) {
        this.openClawAgentId = requireNotBlank(openClawAgentId, "openClawAgentId");
        this.status = WorkspaceAgentStatus.READY;
        this.syncError = null;
    }

    public void markSyncFailed(String syncError) {
        this.status = WorkspaceAgentStatus.SYNC_FAILED;
        this.syncError = normalizeError(syncError);
    }

    public void markError(String syncError) {
        this.status = WorkspaceAgentStatus.ERROR;
        this.syncError = normalizeError(syncError);
    }

    public void disable() {
        this.status = WorkspaceAgentStatus.DISABLED;
        this.syncError = null;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public WorkspaceAgentCategory getCategory() {
        return category;
    }

    public String getOpenClawAgentId() {
        return openClawAgentId;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public WorkspaceAgentStatus getStatus() {
        return status;
    }

    public String getSyncError() {
        return syncError;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
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

    private static String normalizeWorkspacePath(String workspacePath, Long workspaceId) {
        if (workspacePath == null || workspacePath.isBlank()) {
            return ".aquila/workspaces/" + workspaceId;
        }
        return workspacePath.trim();
    }

    private static String normalizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        String trimmed = error.trim();
        if (trimmed.length() <= 1000) {
            return trimmed;
        }
        return trimmed.substring(0, 1000);
    }
}
