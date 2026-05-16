package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.ArtifactNodeResult;
import com.aquilaoffice.application.port.in.ArtifactTreeResult;
import com.aquilaoffice.application.port.out.ArtifactFileData;
import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactTreeData;

final class ArtifactResultMapper {

    private ArtifactResultMapper() {}

    static ArtifactTreeResult toTreeResult(ArtifactTreeData tree) {
        return new ArtifactTreeResult(
                tree.workspaceId(),
                tree.rootPath(),
                tree.children().stream().map(ArtifactResultMapper::toNodeResult).toList());
    }

    static ArtifactFileResult toFileResult(ArtifactFileData file) {
        return new ArtifactFileResult(
                file.workspaceId(),
                file.path(),
                file.name(),
                file.contentType(),
                file.sizeBytes(),
                file.content());
    }

    private static ArtifactNodeResult toNodeResult(ArtifactNodeData node) {
        return new ArtifactNodeResult(
                node.name(),
                node.path(),
                node.type().name(),
                node.contentType(),
                node.sizeBytes(),
                node.children().stream().map(ArtifactResultMapper::toNodeResult).toList());
    }
}
