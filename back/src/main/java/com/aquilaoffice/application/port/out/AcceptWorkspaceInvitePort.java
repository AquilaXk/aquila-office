package com.aquilaoffice.application.port.out;

/**
 * application 계층이 초대 수락 저장 방식에 의존하지 않도록 분리한 outbound port다.
 */
public interface AcceptWorkspaceInvitePort {

    AcceptedWorkspaceInvite accept(WorkspaceInviteAcceptance acceptance);
}
