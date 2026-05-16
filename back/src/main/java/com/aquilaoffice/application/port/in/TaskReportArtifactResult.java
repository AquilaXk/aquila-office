package com.aquilaoffice.application.port.in;

public record TaskReportArtifactResult(
        Long artifactId,
        String artifactType,
        String name,
        String url) {}
