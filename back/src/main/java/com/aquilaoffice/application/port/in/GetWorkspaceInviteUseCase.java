package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 초대 token 조회 inbound port다.
 */
public interface GetWorkspaceInviteUseCase {

    GetWorkspaceInviteResult getWorkspaceInvite(GetWorkspaceInviteQuery query);
}
