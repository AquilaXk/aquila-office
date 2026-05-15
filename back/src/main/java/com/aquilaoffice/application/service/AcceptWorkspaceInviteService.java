package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.AcceptedWorkspaceInvite;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteAcceptance;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 초대 수락 유스케이스를 수행하는 application service다.
 */
public class AcceptWorkspaceInviteService implements AcceptWorkspaceInviteUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspaceInvitePort loadWorkspaceInvitePort;
    private final AcceptWorkspaceInvitePort acceptWorkspaceInvitePort;
    private final Clock clock;

    public AcceptWorkspaceInviteService(
            LoadMemberPort loadMemberPort,
            LoadWorkspaceInvitePort loadWorkspaceInvitePort,
            AcceptWorkspaceInvitePort acceptWorkspaceInvitePort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspaceInvitePort =
                Objects.requireNonNull(loadWorkspaceInvitePort, "loadWorkspaceInvitePort must not be null");
        this.acceptWorkspaceInvitePort =
                Objects.requireNonNull(acceptWorkspaceInvitePort, "acceptWorkspaceInvitePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AcceptWorkspaceInviteResult acceptWorkspaceInvite(AcceptWorkspaceInviteCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member member = loadMemberPort
                .findByGoogleSub(command.memberGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        WorkspaceInviteLookup invite = loadWorkspaceInvitePort
                .findByToken(command.token())
                .orElseThrow(() -> new IllegalArgumentException("workspace invite not found"));
        LocalDateTime acceptedAt = LocalDateTime.now(clock);

        validateInvite(member, invite, acceptedAt);

        AcceptedWorkspaceInvite acceptedInvite = acceptWorkspaceInvitePort.accept(new WorkspaceInviteAcceptance(
                invite.inviteId(),
                invite.workspaceId(),
                member.getGoogleSub(),
                WorkspaceMemberRole.MEMBER,
                acceptedAt));

        return new AcceptWorkspaceInviteResult(
                acceptedInvite.inviteId(),
                acceptedInvite.workspaceId(),
                acceptedInvite.memberGoogleSub(),
                acceptedInvite.role(),
                acceptedInvite.acceptedAt());
    }

    private void validateInvite(Member member, WorkspaceInviteLookup invite, LocalDateTime acceptedAt) {
        if (!member.getEmail().equalsIgnoreCase(invite.email())) {
            throw new IllegalArgumentException("invite email mismatch");
        }
        if (invite.status() != WorkspaceInviteStatus.PENDING) {
            throw new IllegalArgumentException("workspace invite is not pending");
        }
        // 도메인 초대와 동일하게 만료 시각과 같거나 이후인 수락 요청은 만료로 본다.
        if (!acceptedAt.isBefore(invite.expiresAt())) {
            throw new IllegalArgumentException("workspace invite is expired");
        }
    }
}
