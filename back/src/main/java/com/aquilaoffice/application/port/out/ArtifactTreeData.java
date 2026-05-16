package com.aquilaoffice.application.port.out;

import java.util.List;

public record ArtifactTreeData(
        Long workspaceId,
        String rootPath,
        List<ArtifactNodeData> children) {

    public ArtifactTreeData {
        children = List.copyOf(children);
    }
}
