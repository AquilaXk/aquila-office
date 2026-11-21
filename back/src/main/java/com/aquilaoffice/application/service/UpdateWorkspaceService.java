package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.port.out.UpdatedWorkspace;
import com.aquilaoffice.application.port.out.WorkspaceUpdate;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * 워크스페이스 정보 수정 유스케이스를 수행하는 application service다.
 */
public class UpdateWorkspaceService implements UpdateWorkspaceUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final UpdateWorkspacePort updateWorkspacePort;

    public UpdateWorkspaceService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            UpdateWorkspacePort updateWorkspacePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.updateWorkspacePort = Objects.requireNonNull(updateWorkspacePort, "updateWorkspacePort must not be null");
    }

    @Override
    public UpdateWorkspaceResult updateWorkspace(UpdateWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(command.requesterGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        GetWorkspaceDetailResult workspaceDetail = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(command.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }

        UpdatedWorkspace updatedWorkspace = updateWorkspacePort.update(new WorkspaceUpdate(
                command.workspaceId(),
                command.name(),
                command.description(),
                command.dailyTokenLimit()));
        return new UpdateWorkspaceResult(
                updatedWorkspace.workspaceId(),
                updatedWorkspace.name(),
                updatedWorkspace.description(),
                updatedWorkspace.creatorGoogleSub(),
                workspaceDetail.myRole(),
                workspaceDetail.joinedAt(),
                updatedWorkspace.dailyTokenLimit());
    }
}
