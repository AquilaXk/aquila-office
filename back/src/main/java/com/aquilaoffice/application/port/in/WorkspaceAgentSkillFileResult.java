package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * Agent skill 파일 동기화 상태와 본문 응답 모델이다.
 */
public record WorkspaceAgentSkillFileResult(
        Long skillFileId,
        String fileName,
        String content,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {

    public WorkspaceAgentSkillFileResult(
            Long skillFileId,
            String fileName,
            WorkspaceAgentSkillFileSyncStatus syncStatus,
            String syncError) {
        this(skillFileId, fileName, null, syncStatus, syncError);
    }
}
