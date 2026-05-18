package com.aquilaoffice.application.port.out;

public interface SaveWorkspaceArtifactPort {

    ArtifactFileData saveFile(WorkspaceArtifactFileCommand command);
}
