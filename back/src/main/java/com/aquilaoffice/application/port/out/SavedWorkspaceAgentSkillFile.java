package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * persistence adapter가 조회한 Agent skill 파일 상태와 본문이다.
 */
public record SavedWorkspaceAgentSkillFile(
        Long skillFileId,
        String fileName,
        String content,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {

    public SavedWorkspaceAgentSkillFile(
            Long skillFileId,
            String fileName,
            WorkspaceAgentSkillFileSyncStatus syncStatus,
            String syncError) {
        this(skillFileId, fileName, null, syncStatus, syncError);
    }
}
