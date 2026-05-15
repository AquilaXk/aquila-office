package com.aquilaoffice.application.port.out;

import java.util.Optional;

/**
 * application 계층이 워크스페이스 초대 조회 저장소 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface LoadWorkspaceInvitePort {

    Optional<WorkspaceInviteLookup> findByToken(String token);
}
