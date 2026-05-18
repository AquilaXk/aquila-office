package com.aquilaoffice.application.port.out;

public interface UpdateOrchestrationSessionStatusPort {

    SavedOrchestrationSession updateStatus(OrchestrationSessionStatusUpdate update);
}
