package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 멤버 제거 inbound port다.
 */
public interface RemoveWorkspaceMemberUseCase {

    void removeWorkspaceMember(RemoveWorkspaceMemberCommand command);
}
