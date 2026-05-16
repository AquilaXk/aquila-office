package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * Agent skill 파일 동기화 상태 응답 모델이다.
 */
public record WorkspaceAgentSkillFileResult(
        Long skillFileId,
        String fileName,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {}
