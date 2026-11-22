package com.aquilaoffice.domain.agent;

/**
 * 워크스페이스에서 작업을 수행하는 Agent의 메타데이터와 동기화 상태를 관리한다.
 */
public class WorkspaceAgent {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_WORKSPACE_PATH_LENGTH = 512;
    private static final int MAX_MODEL_ID_LENGTH = 160;

    private final Long id;
    private final Long workspaceId;
    private String name;
    private WorkspaceAgentCategory category;
    private String openClawAgentId;
    private String workspacePath;
    private WorkspaceAgentStatus status;
    private String syncError;
    private final String createdByGoogleSub;
    private Long modelProviderId;
    private String modelId;
    private String fallbackModelId;

    private WorkspaceAgent(
            Long id,
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String openClawAgentId,
            String workspacePath,
            WorkspaceAgentStatus status,
            String syncError,
            String createdByGoogleSub,
            Long modelProviderId,
            String modelId,
            String fallbackModelId) {
        this.id = id;
        this.workspaceId = requirePositive(workspaceId, "workspaceId");
        this.name = validateName(name);
        this.category = category == null ? WorkspaceAgentCategory.CUSTOM : category;
        this.openClawAgentId = normalizeNullable(openClawAgentId);
        this.workspacePath = normalizeWorkspacePath(workspacePath, this.workspaceId);
        this.status = status == null ? WorkspaceAgentStatus.CREATING : status;
        this.syncError = normalizeError(syncError);
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
        this.modelProviderId = validateOptionalPositive(modelProviderId, "modelProviderId");
        this.modelId = validateOptionalModelId(modelId, "modelId");
        this.fallbackModelId = validateOptionalModelId(fallbackModelId, "fallbackModelId");
    }

    public static WorkspaceAgent create(
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            String createdByGoogleSub) {
        return create(workspaceId, name, category, workspacePath, createdByGoogleSub, null, null, null);
    }

    public static WorkspaceAgent create(
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String workspacePath,
            String createdByGoogleSub,
            Long modelProviderId,
            String modelId,
            String fallbackModelId) {
        return new WorkspaceAgent(
                null,
                workspaceId,
                name,
                category,
                null,
                workspacePath,
                WorkspaceAgentStatus.CREATING,
                null,
                createdByGoogleSub,
                modelProviderId,
                modelId,
                fallbackModelId);
    }

    public static WorkspaceAgent restore(
            Long id,
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String openClawAgentId,
            String workspacePath,
            WorkspaceAgentStatus status,
            String syncError,
            String createdByGoogleSub) {
        return restore(
                id,
                workspaceId,
                name,
                category,
                openClawAgentId,
                workspacePath,
                status,
                syncError,
                createdByGoogleSub,
                null,
                null,
                null);
    }

    public static WorkspaceAgent restore(
            Long id,
            Long workspaceId,
            String name,
            WorkspaceAgentCategory category,
            String openClawAgentId,
            String workspacePath,
            WorkspaceAgentStatus status,
            String syncError,
            String createdByGoogleSub,
            Long modelProviderId,
            String modelId,
            String fallbackModelId) {
        return new WorkspaceAgent(
                requirePositive(id, "id"),
                workspaceId,
                name,
                category,
                openClawAgentId,
                workspacePath,
                status,
                syncError,
                createdByGoogleSub,
                modelProviderId,
                modelId,
                fallbackModelId);
    }

    public void update(String name, WorkspaceAgentCategory category, String workspacePath) {
        if (name != null) {
            this.name = validateName(name);
        }
        if (category != null) {
            this.category = category;
        }
        if (workspacePath != null && !workspacePath.isBlank()) {
            this.workspacePath = validateWorkspacePath(workspacePath);
        }
    }

    public void updateModelConfig(Long modelProviderId, String modelId, String fallbackModelId) {
        this.modelProviderId = validateOptionalPositive(modelProviderId, "modelProviderId");
        this.modelId = validateOptionalModelId(modelId, "modelId");
        this.fallbackModelId = validateOptionalModelId(fallbackModelId, "fallbackModelId");
    }

    public Long getId() {
        return id;
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

    public Long getModelProviderId() {
        return modelProviderId;
    }

    public String getModelId() {
        return modelId;
    }

    public String getFallbackModelId() {
        return fallbackModelId;
    }

    private static Long validateOptionalPositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requirePositive(value, fieldName);
    }

    private static String validateOptionalModelId(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > MAX_MODEL_ID_LENGTH) {
            throw new IllegalArgumentException(fieldName + " exceeds max length");
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

    private static String normalizeWorkspacePath(String workspacePath, Long workspaceId) {
        if (workspacePath == null || workspacePath.isBlank()) {
            return ".aquila/workspaces/" + workspaceId;
        }
        return validateWorkspacePath(workspacePath);
    }

    private static String validateWorkspacePath(String workspacePath) {
        String trimmed = requireNotBlank(workspacePath, "workspacePath");
        if (trimmed.length() > MAX_WORKSPACE_PATH_LENGTH) {
            throw new IllegalArgumentException("workspacePath exceeds max length");
        }
        if (trimmed.contains("..") || trimmed.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("workspacePath contains unsafe characters");
        }
        return trimmed;
    }

    private static String validateName(String name) {
        String trimmed = requireNotBlank(name, "name");
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name exceeds max length");
        }
        return trimmed;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
