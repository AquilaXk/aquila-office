package com.aquilaoffice.application.port.in;

import java.util.List;

public record ArtifactNodeResult(
        String name,
        String path,
        String type,
        String contentType,
        Long sizeBytes,
        List<ArtifactNodeResult> children) {

    public ArtifactNodeResult {
        children = List.copyOf(children);
    }
}
