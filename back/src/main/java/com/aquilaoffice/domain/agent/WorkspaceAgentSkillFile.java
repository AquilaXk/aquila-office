package com.aquilaoffice.domain.agent;

/**
 * Agent에 함께 전달할 skill 파일과 동기화 상태를 표현한다.
 */
public class WorkspaceAgentSkillFile {

    public static final int MAX_SKILL_FILES = 20;
    public static final int MAX_FILE_NAME_LENGTH = 200;
    public static final int MAX_CONTENT_LENGTH = 65_536;

    private final String fileName;
    private final String content;
    private WorkspaceAgentSkillFileSyncStatus syncStatus;
    private String syncError;

    private WorkspaceAgentSkillFile(String fileName, String content) {
        this.fileName = validateFileName(fileName);
        this.content = validateContent(content);
        this.syncStatus = WorkspaceAgentSkillFileSyncStatus.PENDING;
    }

    public static WorkspaceAgentSkillFile create(String fileName, String content) {
        return new WorkspaceAgentSkillFile(fileName, content);
    }

    public static String validateFileName(String value) {
        String trimmed = requireNotBlank(value, "fileName");
        if (trimmed.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("fileName exceeds max length");
        }
        if (".".equals(trimmed)
                || trimmed.contains("..")
                || trimmed.indexOf('/') >= 0
                || trimmed.indexOf('\\') >= 0
                || trimmed.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("fileName contains unsafe path characters");
        }
        return trimmed;
    }

    public static String validateContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("skill file content exceeds max length");
        }
        return content;
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
