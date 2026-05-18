package com.aquilaoffice.application.port.out;

public record WorkspaceArtifactFileCommand(
        Long workspaceId,
        String path,
        String content) {

    public WorkspaceArtifactFileCommand {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("artifact path must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        path = path.trim();
    }
}
