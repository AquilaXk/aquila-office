package com.aquilaoffice.application.port.in;

/**
 * 단건 Multi-Agent orchestration session을 조회하는 use case다.
 */
public interface GetOrchestrationSessionUseCase {

    GetOrchestrationSessionResult getSession(GetOrchestrationSessionQuery query);
}
