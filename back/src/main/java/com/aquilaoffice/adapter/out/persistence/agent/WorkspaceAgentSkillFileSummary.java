package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;

/**
 * 목록·상세 조회 시 본문(TEXT) 적재 없이 skill 파일 요약 정보만 전달하는 JPQL 프로젝션이다.
 */
public record WorkspaceAgentSkillFileSummary(
        Long id,
        Long agentId,
        String fileName,
        WorkspaceAgentSkillFileSyncStatus syncStatus,
        String syncError) {}
