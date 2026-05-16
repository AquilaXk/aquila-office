package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * persistence adapter가 조회한 Agent skill 파일 상태다.
 */
public record SavedWorkspaceAgentSkillFile(
        Long skillFileId,
        String fileName,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {}
