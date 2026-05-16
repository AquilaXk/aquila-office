package com.aquilaoffice.adapter.in.web.agent;

import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * Agent skill 파일 동기화 상태 응답이다.
 */
public record WorkspaceAgentSkillFileResponse(
        Long skillFileId,
        String fileName,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {

    public static WorkspaceAgentSkillFileResponse from(WorkspaceAgentSkillFileResult result) {
        return new WorkspaceAgentSkillFileResponse(
                result.skillFileId(),
                result.fileName(),
                result.syncStatus(),
                result.syncError());
    }
}
