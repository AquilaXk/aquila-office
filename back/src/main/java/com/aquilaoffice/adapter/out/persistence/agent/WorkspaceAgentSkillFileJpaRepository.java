package com.aquilaoffice.adapter.out.persistence.agent;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Agent skill 파일 JPA entity 저장과 요약/상세 프로젝션 조회를 담당한다.
 */
public interface WorkspaceAgentSkillFileJpaRepository extends JpaRepository<WorkspaceAgentSkillFileJpaEntity, Long> {

    @Query("""
            select new com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentSkillFileSummary(
                    s.id,
                    s.agent.id,
                    s.fileName,
                    s.syncStatus,
                    s.syncError)
            from WorkspaceAgentSkillFileJpaEntity s
            where s.agent.id = :agentId
            order by s.id asc
            """)
    List<WorkspaceAgentSkillFileSummary> findSummariesByAgentIdOrderByIdAsc(@Param("agentId") Long agentId);

    @Query("""
            select new com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentSkillFileSummary(
                    s.id,
                    s.agent.id,
                    s.fileName,
                    s.syncStatus,
                    s.syncError)
            from WorkspaceAgentSkillFileJpaEntity s
            where s.agent.id in :agentIds
            order by s.agent.id asc, s.id asc
            """)
    List<WorkspaceAgentSkillFileSummary> findSummariesByAgentIdInOrderByAgentIdAscIdAsc(
            @Param("agentIds") Collection<Long> agentIds);

    @Query("""
            select new com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentSkillFileDetail(
                    s.id,
                    s.agent.id,
                    s.fileName,
                    s.content,
                    s.syncStatus,
                    s.syncError)
            from WorkspaceAgentSkillFileJpaEntity s
            where s.agent.id = :agentId
            order by s.id asc
            """)
    List<WorkspaceAgentSkillFileDetail> findDetailsByAgentIdOrderByIdAsc(@Param("agentId") Long agentId);

    @Query("""
            select new com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentSkillFileDetail(
                    s.id,
                    s.agent.id,
                    s.fileName,
                    s.content,
                    s.syncStatus,
                    s.syncError)
            from WorkspaceAgentSkillFileJpaEntity s
            where s.agent.id in :agentIds
            order by s.agent.id asc, s.id asc
            """)
    List<WorkspaceAgentSkillFileDetail> findDetailsByAgentIdInOrderByAgentIdAscIdAsc(
            @Param("agentIds") Collection<Long> agentIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from WorkspaceAgentSkillFileJpaEntity s where s.agent.id = :agentId")
    void deleteAllByAgentId(@Param("agentId") Long agentId);
}
