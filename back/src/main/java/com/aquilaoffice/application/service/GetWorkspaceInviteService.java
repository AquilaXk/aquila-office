package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteLookup;
import java.util.Objects;

/**
 * 워크스페이스 초대 token 조회 유스케이스를 수행하는 application service다.
 */
public class GetWorkspaceInviteService implements GetWorkspaceInviteUseCase {

    private final LoadWorkspaceInvitePort loadWorkspaceInvitePort;

    public GetWorkspaceInviteService(LoadWorkspaceInvitePort loadWorkspaceInvitePort) {
        this.loadWorkspaceInvitePort =
                Objects.requireNonNull(loadWorkspaceInvitePort, "loadWorkspaceInvitePort must not be null");
    }

    @Override
    public GetWorkspaceInviteResult getWorkspaceInvite(GetWorkspaceInviteQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        return loadWorkspaceInvitePort
                .findByToken(query.token())
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException("workspace invite not found"));
    }

    private GetWorkspaceInviteResult toResult(WorkspaceInviteLookup lookup) {
        return new GetWorkspaceInviteResult(
                lookup.inviteId(),
                lookup.workspaceId(),
                lookup.email(),
                lookup.token(),
                lookup.status(),
                lookup.createdAt(),
                lookup.expiresAt(),
                lookup.acceptedAt());
    }
}
