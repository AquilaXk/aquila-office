package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.Objects;

/**
 * 워크스페이스 상세 조회 유스케이스를 수행하는 application service다.
 */
public class GetWorkspaceDetailService implements GetWorkspaceDetailUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;

    public GetWorkspaceDetailService(LoadMemberPort loadMemberPort, LoadWorkspacePort loadWorkspacePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
    }

    @Override
    public GetWorkspaceDetailResult getWorkspaceDetail(GetWorkspaceDetailQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String memberGoogleSub = query.memberGoogleSub();
        loadMemberPort
                .findByGoogleSub(memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));

        return loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(query.workspaceId(), memberGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
    }
}
