package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.slack.SlackChannelMapping;
import java.util.List;
import java.util.Optional;

/**
 * Slack channel mapping active 조회 outbound port다.
 */
public interface LoadSlackChannelMappingPort {

    Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId);

    Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId);

    List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId);
}
