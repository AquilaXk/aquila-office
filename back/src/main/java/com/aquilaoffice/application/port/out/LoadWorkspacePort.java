package com.aquilaoffice.application.port.out;

import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import java.util.List;

/**
 * application 계층이 워크스페이스 조회 저장소 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface LoadWorkspacePort {

    List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub);
}
