package com.aquilaoffice.adapter.out.persistence.slack;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slack channel mapping outbound port를 JPA repository로 구현하는 persistence adapter다.
 */
@Repository
public class SlackChannelMappingPersistenceAdapter
        implements SaveSlackChannelMappingPort, LoadSlackChannelMappingPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final SlackChannelMappingJpaRepository slackChannelMappingJpaRepository;

    public SlackChannelMappingPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            SlackChannelMappingJpaRepository slackChannelMappingJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.slackChannelMappingJpaRepository = slackChannelMappingJpaRepository;
    }

    @Override
    @Transactional
    public SlackChannelMapping save(SlackChannelMapping mapping) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(mapping.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        SlackChannelMappingJpaEntity savedEntity = slackChannelMappingJpaRepository.save(
                SlackChannelMappingJpaEntity.from(mapping, workspaceEntity));
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
        return slackChannelMappingJpaRepository.findActiveByTeamIdAndChannelId(
                        normalizeRequired(teamId, "teamId"),
                        normalizeRequired(channelId, "channelId"))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return slackChannelMappingJpaRepository.findActiveByWorkspaceId(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    private SlackChannelMapping toDomain(SlackChannelMappingJpaEntity entity) {
        return SlackChannelMapping.restore(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getTeamId(),
                entity.getChannelId(),
                entity.getChannelName(),
                null,
                entity.getCreatedByGoogleSub(),
                entity.isActive());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
