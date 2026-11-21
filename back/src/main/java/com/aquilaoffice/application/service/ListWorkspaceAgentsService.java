package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListWorkspaceAgentsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.List;

/**
 * 워크스페이스에 가입된 회원에게 active Agent 목록을 제공한다.
 */
public class ListWorkspaceAgentsService implements ListWorkspaceAgentsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;

    public ListWorkspaceAgentsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.loadWorkspaceAgentPort = loadWorkspaceAgentPort;
    }

    @Override
    public List<ListWorkspaceAgentsResult> listWorkspaceAgents(ListWorkspaceAgentsQuery query) {
        requireJoinedWorkspace(query.workspaceId(), query.requesterGoogleSub());

        return loadWorkspaceAgentPort.findActiveByWorkspaceId(query.workspaceId()).stream()
                .map(WorkspaceAgentResultMapper::toListResult)
                .toList();
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }
}
