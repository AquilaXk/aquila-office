package com.aquilaoffice.application.port.in;

public record ArtifactFileResult(
        Long workspaceId,
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        String content) {}
