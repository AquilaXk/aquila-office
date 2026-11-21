package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetOrchestrationSessionQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedOrchestrationSession;
import java.util.Objects;

/**
 * 워크스페이스 멤버가 단건 Multi-Agent orchestration session 상세를 조회하는 application service다.
 */
public class GetOrchestrationSessionService implements GetOrchestrationSessionUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadOrchestrationSessionPort loadOrchestrationSessionPort;

    public GetOrchestrationSessionService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadOrchestrationSessionPort loadOrchestrationSessionPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadOrchestrationSessionPort = Objects.requireNonNull(
                loadOrchestrationSessionPort, "loadOrchestrationSessionPort must not be null");
    }

    @Override
    public GetOrchestrationSessionResult getSession(GetOrchestrationSessionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        loadMemberPort.findByGoogleSub(query.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), query.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        SavedOrchestrationSession session = loadOrchestrationSessionPort
                .findDetailByWorkspaceIdAndSessionId(query.workspaceId(), query.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("orchestration session not found"));
        return OrchestrationResultMapper.toGetResult(session);
    }
}
