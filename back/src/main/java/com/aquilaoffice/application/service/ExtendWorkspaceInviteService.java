package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.ExtendWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteExtension;
import com.aquilaoffice.application.port.out.WorkspaceInviteManagement;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 초대 연장 유스케이스를 수행하는 application service다.
 */
public class ExtendWorkspaceInviteService implements ExtendWorkspaceInviteUseCase {

    private static final int DEFAULT_ADDITIONAL_DAYS = 7;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final ExtendWorkspaceInvitePort extendWorkspaceInvitePort;
    private final Clock clock;

    public ExtendWorkspaceInviteService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            ExtendWorkspaceInvitePort extendWorkspaceInvitePort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.extendWorkspaceInvitePort =
                Objects.requireNonNull(extendWorkspaceInvitePort, "extendWorkspaceInvitePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ExtendWorkspaceInviteResult extendWorkspaceInvite(ExtendWorkspaceInviteCommand command) {
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

        int additionalDays = command.additionalDays() == null
                ? DEFAULT_ADDITIONAL_DAYS
                : command.additionalDays();
        WorkspaceInviteManagement management = extendWorkspaceInvitePort.extend(new WorkspaceInviteExtension(
                command.workspaceId(),
                command.inviteId(),
                additionalDays,
                LocalDateTime.now(clock)));
        return toResult(management);
    }

    private ExtendWorkspaceInviteResult toResult(WorkspaceInviteManagement management) {
        return new ExtendWorkspaceInviteResult(
                management.inviteId(),
                management.workspaceId(),
                management.email(),
                management.token(),
                management.status(),
                management.createdAt(),
                management.expiresAt(),
                management.acceptedAt(),
                management.revokedAt());
    }
}

