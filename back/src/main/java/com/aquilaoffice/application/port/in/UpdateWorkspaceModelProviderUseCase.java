package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 AI 모델 공급자 수정 use case다.
 */
public interface UpdateWorkspaceModelProviderUseCase {

    WorkspaceModelProviderResult update(UpdateWorkspaceModelProviderCommand command);
}
