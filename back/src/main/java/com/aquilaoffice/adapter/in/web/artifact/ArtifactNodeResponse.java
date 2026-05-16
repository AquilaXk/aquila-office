package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.ArtifactNodeResult;
import java.util.List;

public record ArtifactNodeResponse(
        String name,
        String path,
        String type,
        String contentType,
        Long sizeBytes,
        List<ArtifactNodeResponse> children) {

    static ArtifactNodeResponse from(ArtifactNodeResult result) {
        return new ArtifactNodeResponse(
                result.name(),
                result.path(),
                result.type(),
                result.contentType(),
                result.sizeBytes(),
                result.children().stream().map(ArtifactNodeResponse::from).toList());
    }
}
