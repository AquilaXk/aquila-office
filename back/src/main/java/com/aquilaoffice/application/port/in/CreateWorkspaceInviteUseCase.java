package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 초대 생성 inbound port다.
 */
public interface CreateWorkspaceInviteUseCase {

    CreateWorkspaceInviteResult createWorkspaceInvite(CreateWorkspaceInviteCommand command);
}
