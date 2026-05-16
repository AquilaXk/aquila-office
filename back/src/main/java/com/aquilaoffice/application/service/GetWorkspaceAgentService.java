package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceAgentQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;

/**
 * 가입된 워크스페이스의 active Agent 상세만 조회한다.
 */
public class GetWorkspaceAgentService implements GetWorkspaceAgentUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;

    public GetWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.loadWorkspaceAgentPort = loadWorkspaceAgentPort;
    }

    @Override
    public GetWorkspaceAgentResult getWorkspaceAgent(GetWorkspaceAgentQuery query) {
        requireJoinedWorkspace(query.workspaceId(), query.requesterGoogleSub());

        return loadWorkspaceAgentPort.findActiveByWorkspaceIdAndAgentId(query.workspaceId(), query.agentId())
                .map(WorkspaceAgentResultMapper::toGetResult)
                .orElseThrow(() -> new IllegalArgumentException("workspace agent not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }
}
