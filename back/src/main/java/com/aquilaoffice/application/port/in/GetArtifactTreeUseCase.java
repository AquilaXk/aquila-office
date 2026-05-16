package com.aquilaoffice.application.port.in;

public interface GetArtifactTreeUseCase {

    ArtifactTreeResult getArtifactTree(GetArtifactTreeQuery query);
}
