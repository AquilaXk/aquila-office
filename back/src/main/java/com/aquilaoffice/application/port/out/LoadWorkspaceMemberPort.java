package com.aquilaoffice.application.port.out;

import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import java.util.List;

/**
 * application 계층이 워크스페이스 멤버 조회 저장소 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface LoadWorkspaceMemberPort {

    List<ListWorkspaceMembersResult> findActiveWorkspaceMembersByWorkspaceId(Long workspaceId);
}
