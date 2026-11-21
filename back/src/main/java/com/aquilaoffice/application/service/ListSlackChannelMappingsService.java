package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스의 active Slack channel mapping 목록 조회 유스케이스를 수행한다.
 */
public class ListSlackChannelMappingsService implements ListSlackChannelMappingsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;

    public ListSlackChannelMappingsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadSlackChannelMappingPort =
                Objects.requireNonNull(loadSlackChannelMappingPort, "loadSlackChannelMappingPort must not be null");
    }

    @Override
    public List<ListSlackChannelMappingsResult> listSlackChannelMappings(ListSlackChannelMappingsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Member requester = loadRequester(query.requesterGoogleSub());
        loadWorkspaceDetail(query.workspaceId(), requester.getGoogleSub());

        return loadSlackChannelMappingPort.findActiveByWorkspaceId(query.workspaceId()).stream()
                .map(this::toResult)
                .toList();
    }

    private Member loadRequester(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
    }

    private GetWorkspaceDetailResult loadWorkspaceDetail(Long workspaceId, String googleSub) {
        return loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
    }

    private ListSlackChannelMappingsResult toResult(SlackChannelMapping mapping) {
        return new ListSlackChannelMappingsResult(
                mapping.getId(),
                mapping.getWorkspaceId(),
                mapping.getTeamId(),
                mapping.getChannelId(),
                mapping.getChannelName(),
                mapping.maskedBotToken(),
                mapping.getCreatedByGoogleSub(),
                mapping.isActive());
    }
}
