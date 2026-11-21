package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListOrchestrationSessionsQuery;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsResult;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버가 Multi-Agent orchestration session 목록을 조회하는 application service다.
 */
public class ListOrchestrationSessionsService implements ListOrchestrationSessionsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadOrchestrationSessionPort loadOrchestrationSessionPort;

    public ListOrchestrationSessionsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadOrchestrationSessionPort loadOrchestrationSessionPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadOrchestrationSessionPort = Objects.requireNonNull(
                loadOrchestrationSessionPort, "loadOrchestrationSessionPort must not be null");
    }

    @Override
    public List<ListOrchestrationSessionsResult> listSessions(ListOrchestrationSessionsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        loadMemberPort.findByGoogleSub(query.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), query.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        return loadOrchestrationSessionPort.findByWorkspaceIdOrderByIdDesc(query.workspaceId())
                .stream()
                .map(OrchestrationResultMapper::toListResult)
                .toList();
    }
}
