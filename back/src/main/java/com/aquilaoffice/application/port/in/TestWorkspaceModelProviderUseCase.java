package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 AI 모델 공급자 연결 테스트 use case다.
 */
public interface TestWorkspaceModelProviderUseCase {

    WorkspaceModelConnectionTestResult testConnection(TestWorkspaceModelProviderCommand command);
}
