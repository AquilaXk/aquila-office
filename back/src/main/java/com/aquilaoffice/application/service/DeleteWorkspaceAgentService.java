package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;

/**
 * 워크스페이스 관리자만 Agent를 비활성화할 수 있도록 검증한다.
 */
public class DeleteWorkspaceAgentService implements DeleteWorkspaceAgentUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final DeleteWorkspaceAgentPort deleteWorkspaceAgentPort;

    public DeleteWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            DeleteWorkspaceAgentPort deleteWorkspaceAgentPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.deleteWorkspaceAgentPort = deleteWorkspaceAgentPort;
    }

    @Override
    public void deleteWorkspaceAgent(DeleteWorkspaceAgentCommand command) {
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        deleteWorkspaceAgentPort.disable(command.workspaceId(), command.agentId());
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireAdminMembership(Long workspaceId, String googleSub) {
        var workspace = loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }
}
