package com.aquilaoffice.application.port.out;

public interface UpdateOrchestrationStepPort {

    SavedOrchestrationStep updateStep(OrchestrationStepTaskUpdate update);
}
