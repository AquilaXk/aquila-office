package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 AI 모델 공급자 소프트 삭제 use case다.
 */
public interface DeleteWorkspaceModelProviderUseCase {

    void delete(Long workspaceId, Long providerId, String requesterGoogleSub);
}
