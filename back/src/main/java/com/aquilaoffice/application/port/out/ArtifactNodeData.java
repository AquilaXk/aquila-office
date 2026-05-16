package com.aquilaoffice.application.port.out;

import java.util.List;

public record ArtifactNodeData(
        String name,
        String path,
        ArtifactNodeType type,
        String contentType,
        Long sizeBytes,
        List<ArtifactNodeData> children) {

    public ArtifactNodeData {
        children = List.copyOf(children);
    }
}
