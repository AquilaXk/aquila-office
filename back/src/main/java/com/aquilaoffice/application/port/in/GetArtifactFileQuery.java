package com.aquilaoffice.application.port.in;

public record GetArtifactFileQuery(
        Long workspaceId,
        String path,
        String requesterGoogleSub) {}
