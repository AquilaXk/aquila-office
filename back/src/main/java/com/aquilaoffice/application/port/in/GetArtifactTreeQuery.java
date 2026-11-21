package com.aquilaoffice.application.port.in;

public record GetArtifactTreeQuery(
        Long workspaceId,
        String requesterGoogleSub) {}
