package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * 워크스페이스 Multi-Agent orchestration session 목록을 조회하는 use case다.
 */
public interface ListOrchestrationSessionsUseCase {

    List<ListOrchestrationSessionsResult> listSessions(ListOrchestrationSessionsQuery query);
}
