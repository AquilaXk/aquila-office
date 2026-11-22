package com.aquilaoffice.application.port.out;

/**
 * 등록된 AI 모델 공급자의 HTTP API로 태스크를 실행하는 outbound port다.
 */
public interface RunWorkspaceModelPort {

    WorkspaceModelRunResult run(WorkspaceModelRunCommand command);
}
