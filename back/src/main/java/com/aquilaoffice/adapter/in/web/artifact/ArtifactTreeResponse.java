package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.ArtifactTreeResult;
import java.util.List;

public record ArtifactTreeResponse(
        Long workspaceId,
        String rootPath,
        List<ArtifactNodeResponse> children) {

    static ArtifactTreeResponse from(ArtifactTreeResult result) {
        return new ArtifactTreeResponse(
                result.workspaceId(),
                result.rootPath(),
                result.children().stream().map(ArtifactNodeResponse::from).toList());
    }
}
