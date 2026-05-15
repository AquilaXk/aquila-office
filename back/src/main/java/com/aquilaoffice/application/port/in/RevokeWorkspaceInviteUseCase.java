package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 초대 취소 inbound port다.
 */
public interface RevokeWorkspaceInviteUseCase {

    void revokeWorkspaceInvite(RevokeWorkspaceInviteCommand command);
}
