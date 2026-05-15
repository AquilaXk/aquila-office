package com.aquilaoffice.application.port.out;

public record SavedWorkspace(Long workspaceId) {

    public SavedWorkspace {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
    }
}
