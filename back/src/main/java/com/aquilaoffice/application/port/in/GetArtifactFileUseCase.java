package com.aquilaoffice.application.port.in;

public interface GetArtifactFileUseCase {

    ArtifactFileResult getArtifactFile(GetArtifactFileQuery query);
}
