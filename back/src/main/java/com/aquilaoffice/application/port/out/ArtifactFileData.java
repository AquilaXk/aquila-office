package com.aquilaoffice.application.port.out;

public record ArtifactFileData(
        Long workspaceId,
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        String content) {}
