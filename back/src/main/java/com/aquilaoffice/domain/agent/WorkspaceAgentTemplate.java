package com.aquilaoffice.domain.agent;

/**
 * Agent bootstrap에 함께 전달할 기본 skill 파일 템플릿이다.
 */
public record WorkspaceAgentTemplate(String fileName, String content) {

    public WorkspaceAgentTemplate {
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
