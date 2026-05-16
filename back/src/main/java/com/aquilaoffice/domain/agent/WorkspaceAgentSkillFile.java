package com.aquilaoffice.domain.agent;

/**
 * Agent에 함께 전달할 skill 파일과 동기화 상태를 표현한다.
 */
public class WorkspaceAgentSkillFile {

    private final String fileName;
    private final String content;
    private WorkspaceAgentSkillFileSyncStatus syncStatus;
    private String syncError;

    private WorkspaceAgentSkillFile(String fileName, String content) {
        this.fileName = requireNotBlank(fileName, "fileName");
        this.content = content == null ? "" : content;
        this.syncStatus = WorkspaceAgentSkillFileSyncStatus.PENDING;
    }

    public static WorkspaceAgentSkillFile create(String fileName, String content) {
        return new WorkspaceAgentSkillFile(fileName, content);
    }

    public void markSynced() {
        this.syncStatus = WorkspaceAgentSkillFileSyncStatus.SYNCED;
        this.syncError = null;
    }

    public void markFailed(String syncError) {
        this.syncStatus = WorkspaceAgentSkillFileSyncStatus.FAILED;
        this.syncError = normalizeError(syncError);
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public WorkspaceAgentSkillFileSyncStatus getSyncStatus() {
        return syncStatus;
    }

    public String getSyncError() {
        return syncError;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
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
