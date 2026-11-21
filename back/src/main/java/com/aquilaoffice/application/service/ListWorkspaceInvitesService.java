package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteManagement;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 초대 목록 조회 유스케이스를 수행하는 application service다.
 */
public class ListWorkspaceInvitesService implements ListWorkspaceInvitesUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceInvitesPort loadWorkspaceInvitesPort;

    public ListWorkspaceInvitesService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceInvitesPort loadWorkspaceInvitesPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceInvitesPort =
                Objects.requireNonNull(loadWorkspaceInvitesPort, "loadWorkspaceInvitesPort must not be null");
    }

    @Override
    public List<ListWorkspaceInvitesResult> listWorkspaceInvites(ListWorkspaceInvitesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(query.memberGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        GetWorkspaceDetailResult workspaceDetail = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }

        return loadWorkspaceInvitesPort.findWorkspaceInvites(query.workspaceId(), query.status()).stream()
                .map(this::toResult)
                .toList();
    }

    private ListWorkspaceInvitesResult toResult(WorkspaceInviteManagement invite) {
        return new ListWorkspaceInvitesResult(
                invite.inviteId(),
                invite.workspaceId(),
                invite.email(),
                invite.token(),
                invite.status(),
                invite.createdAt(),
                invite.expiresAt(),
                invite.acceptedAt(),
                invite.revokedAt());
    }
}
