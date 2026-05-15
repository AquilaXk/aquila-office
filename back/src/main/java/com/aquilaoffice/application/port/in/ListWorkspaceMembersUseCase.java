package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * 워크스페이스 멤버 목록 조회 inbound port다.
 */
public interface ListWorkspaceMembersUseCase {

    List<ListWorkspaceMembersResult> listWorkspaceMembers(ListWorkspaceMembersQuery query);
}
