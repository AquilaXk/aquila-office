package com.aquilaoffice.application.port.out;

/**
 * 워크스페이스 초대 만료일 연장을 persistence 구현 밖으로 분리한 outbound port다.
 */
public interface ExtendWorkspaceInvitePort {

    WorkspaceInviteManagement extend(WorkspaceInviteExtension extension);
}

