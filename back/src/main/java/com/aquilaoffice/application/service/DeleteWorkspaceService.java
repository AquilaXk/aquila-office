package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.DeleteWorkspaceCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceDeletion;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 삭제 유스케이스를 수행하는 application service다.
 */
public class DeleteWorkspaceService implements DeleteWorkspaceUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final DeleteWorkspacePort deleteWorkspacePort;
    private final Clock clock;

    public DeleteWorkspaceService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            DeleteWorkspacePort deleteWorkspacePort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.deleteWorkspacePort = Objects.requireNonNull(deleteWorkspacePort, "deleteWorkspacePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void deleteWorkspace(DeleteWorkspaceCommand command) {
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

        deleteWorkspacePort.delete(new WorkspaceDeletion(command.workspaceId(), LocalDateTime.now(clock)));
    }
}
