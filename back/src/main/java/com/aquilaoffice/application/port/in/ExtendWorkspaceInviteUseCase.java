package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 초대 만료일을 연장하는 inbound port다.
 */
public interface ExtendWorkspaceInviteUseCase {

    ExtendWorkspaceInviteResult extendWorkspaceInvite(ExtendWorkspaceInviteCommand command);
}

