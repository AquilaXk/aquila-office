package com.aquilaoffice.application.port.out;

/**
 * application 계층이 워크스페이스 초대 취소 저장 방식에 의존하지 않도록 분리한 outbound port다.
 */
public interface RevokeWorkspaceInvitePort {

    void revoke(WorkspaceInviteRevocation revocation);
}
