package com.aquilaoffice.application.port.in;

/**
 * Agent 생성 시 함께 저장할 skill 파일 입력이다.
 */
public record WorkspaceAgentSkillFileCommand(String fileName, String content) {

    public WorkspaceAgentSkillFileCommand {
        fileName = requireNotBlank(fileName, "fileName");
        content = content == null ? "" : content;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
