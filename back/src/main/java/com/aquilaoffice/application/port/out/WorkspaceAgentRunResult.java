package com.aquilaoffice.application.port.out;

public record WorkspaceAgentRunResult(boolean success, String finalText, String failureReason) {

    public WorkspaceAgentRunResult {
        finalText = normalize(finalText);
        failureReason = normalize(failureReason);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
