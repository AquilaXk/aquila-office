package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 초대 수락 inbound port다.
 */
public interface AcceptWorkspaceInviteUseCase {

    AcceptWorkspaceInviteResult acceptWorkspaceInvite(AcceptWorkspaceInviteCommand command);
}
