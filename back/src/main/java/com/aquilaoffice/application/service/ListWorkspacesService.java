package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 목록 조회 유스케이스를 수행하는 application service다.
 */
public class ListWorkspacesService implements ListWorkspacesUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;

    public ListWorkspacesService(LoadMemberPort loadMemberPort, LoadWorkspacePort loadWorkspacePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
    }

    @Override
    public List<ListWorkspacesResult> listWorkspaces(ListWorkspacesQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String memberGoogleSub = query.memberGoogleSub();
        loadMemberPort
                .findByGoogleSub(memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));

        return loadWorkspacePort.findJoinedWorkspacesByMemberGoogleSub(memberGoogleSub);
    }
}
