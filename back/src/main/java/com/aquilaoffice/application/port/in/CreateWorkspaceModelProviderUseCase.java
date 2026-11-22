package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 AI 모델 공급자 등록 use case다.
 */
public interface CreateWorkspaceModelProviderUseCase {

    WorkspaceModelProviderResult create(CreateWorkspaceModelProviderCommand command);
}
