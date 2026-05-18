package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationSession;

public interface SaveOrchestrationSessionPort {

    SavedOrchestrationSession save(OrchestrationSession session);
}
