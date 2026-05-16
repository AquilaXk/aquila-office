package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.ArtifactFileResult;

public record ArtifactFileContentResponse(
        Long workspaceId,
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        String content) {

    static ArtifactFileContentResponse from(ArtifactFileResult result) {
        return new ArtifactFileContentResponse(
                result.workspaceId(),
                result.path(),
                result.name(),
                result.contentType(),
                result.sizeBytes(),
                result.content());
    }
}
