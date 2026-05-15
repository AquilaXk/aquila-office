package com.aquilaoffice.application.port.in;

public record CreateWorkspaceCommand(String name, String description, String creatorGoogleSub) {

    public CreateWorkspaceCommand {
        creatorGoogleSub = requireNotBlank(creatorGoogleSub, "creatorGoogleSub");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
