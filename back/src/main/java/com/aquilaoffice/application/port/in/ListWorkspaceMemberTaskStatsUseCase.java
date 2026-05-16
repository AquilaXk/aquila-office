package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * 워크스페이스 멤버별 작업 통계 조회 inbound port다.
 */
public interface ListWorkspaceMemberTaskStatsUseCase {

    List<ListWorkspaceMemberTaskStatsResult> listMemberTaskStats(ListWorkspaceMemberTaskStatsQuery query);
}
