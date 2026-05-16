package com.aquilaoffice.application.port.out;

public record OrchestrationPlanFileData(
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        boolean exists) {}
