package com.aquilaoffice.application.port.in;

public interface CreateOrchestrationSessionUseCase {

    CreateOrchestrationSessionResult createSession(CreateOrchestrationSessionCommand command);
}
