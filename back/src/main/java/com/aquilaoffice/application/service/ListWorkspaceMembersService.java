package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListWorkspaceMembersQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버 목록 조회 유스케이스를 수행하는 application service다.
 */
public class ListWorkspaceMembersService implements ListWorkspaceMembersUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;

    public ListWorkspaceMembersService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
    }

    @Override
    public List<ListWorkspaceMembersResult> listWorkspaceMembers(ListWorkspaceMembersQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(query.memberGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));

        return loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(query.workspaceId());
    }
}
