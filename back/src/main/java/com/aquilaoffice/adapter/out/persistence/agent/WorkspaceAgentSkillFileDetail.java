package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * Agent skill 파일 본문(content)을 포함해 조회하는 JPQL 프로젝션이다.
 */
public record WorkspaceAgentSkillFileDetail(
        Long id,
        Long agentId,
        String fileName,
        String content,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {}
