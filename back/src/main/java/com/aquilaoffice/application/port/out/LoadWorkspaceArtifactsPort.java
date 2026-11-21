package com.aquilaoffice.application.port.out;

public interface LoadWorkspaceArtifactsPort {

    ArtifactTreeData loadTree(Long workspaceId);

    ArtifactFileData loadFile(Long workspaceId, String path);
}
