package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteRevocation;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 초대 취소 유스케이스를 수행하는 application service다.
 */
public class RevokeWorkspaceInviteService implements RevokeWorkspaceInviteUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final RevokeWorkspaceInvitePort revokeWorkspaceInvitePort;
    private final Clock clock;

    public RevokeWorkspaceInviteService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            RevokeWorkspaceInvitePort revokeWorkspaceInvitePort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.revokeWorkspaceInvitePort =
                Objects.requireNonNull(revokeWorkspaceInvitePort, "revokeWorkspaceInvitePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void revokeWorkspaceInvite(RevokeWorkspaceInviteCommand command) {
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

        revokeWorkspaceInvitePort.revoke(new WorkspaceInviteRevocation(
                command.workspaceId(), command.inviteId(), LocalDateTime.now(clock)));
    }
}
