package com.aquilaoffice.application.port.in;

/**
 * Slack 수동 연동 삭제 use case다.
 */
public interface DeleteSlackIntegrationUseCase {

    DeleteSlackIntegrationResult deleteSlackIntegration(DeleteSlackIntegrationCommand command);
}
