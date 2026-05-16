package com.aquilaoffice.application.port.in;

import java.util.List;

public record ArtifactTreeResult(
        Long workspaceId,
        String rootPath,
        List<ArtifactNodeResult> children) {

    public ArtifactTreeResult {
        children = List.copyOf(children);
    }
}
