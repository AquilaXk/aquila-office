package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 Agent 메타데이터와 skill 파일을 수정하는 use case다.
 */
public interface UpdateWorkspaceAgentUseCase {

    UpdateWorkspaceAgentResult updateWorkspaceAgent(UpdateWorkspaceAgentCommand command);
}
