package com.aquilaoffice.application.port.in;

public interface DispatchOrchestrationSessionUseCase {

    DispatchOrchestrationSessionResult dispatchSession(DispatchOrchestrationSessionCommand command);
}
