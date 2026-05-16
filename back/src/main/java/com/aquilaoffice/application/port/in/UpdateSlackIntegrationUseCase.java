package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 수정 use case다.
 */
public interface UpdateSlackIntegrationUseCase {

    UpdateSlackIntegrationResult updateSlackIntegration(UpdateSlackIntegrationCommand command);
}
