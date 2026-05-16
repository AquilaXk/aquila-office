package com.aquilaoffice.adapter.out.persistence.slack;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Slack channel mapping JPA entity 저장과 active mapping 조회를 담당하는 repository다.
 */
public interface SlackChannelMappingJpaRepository extends JpaRepository<SlackChannelMappingJpaEntity, Long> {

    @Query("""
            select sm
            from SlackChannelMappingJpaEntity sm
            join fetch sm.workspace w
            where sm.teamId = :teamId
              and sm.channelId = :channelId
              and sm.active = true
            """)
    Optional<SlackChannelMappingJpaEntity> findActiveByTeamIdAndChannelId(
            @Param("teamId") String teamId,
            @Param("channelId") String channelId);

    @Query("""
            select sm
            from SlackChannelMappingJpaEntity sm
            join fetch sm.workspace w
            where w.id = :workspaceId
              and sm.active = true
            order by sm.id asc
            """)
    List<SlackChannelMappingJpaEntity> findActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
