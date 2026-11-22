package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import java.util.List;
import java.util.Optional;

/**
 * 워크스페이스의 활성 AI 모델 공급자 설정을 조회하는 outbound port다.
 */
public interface LoadWorkspaceModelProviderPort {

    List<WorkspaceModelProvider> findActiveByWorkspaceId(Long workspaceId);

    Optional<WorkspaceModelProvider> findActiveByWorkspaceIdAndProviderId(Long workspaceId, Long providerId);
}
