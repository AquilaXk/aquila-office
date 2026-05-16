package com.aquilaoffice.application.port.in;

public record OrchestrationPlanFileResult(
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        boolean exists) {}
