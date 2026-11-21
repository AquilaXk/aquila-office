package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 삭제 결과다.
 */
public record DeleteSlackIntegrationResult(
        Long id,
        Long workspaceId,
        boolean active) {}
