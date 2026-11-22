package com.aquilaoffice.application.port.out;

import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;

/**
 * 외부 AI 모델 공급자에 실제 연결 테스트(모델 조회 또는 최소 완료 호출)를 수행하는 outbound port다.
 */
public interface TestWorkspaceModelProviderPort {

    WorkspaceModelConnectionTestResult testConnection(WorkspaceModelConnectionTestRequest request);
}
