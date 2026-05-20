package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackCommand;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackResult;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.ExchangeSlackOAuthCodePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeRequest;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeResult;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.slack.SlackOAuthState;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Slack OAuth callback 결과를 workspace Slack channel mapping으로 저장한다.
 */
public class CompleteSlackOAuthCallbackService implements CompleteSlackOAuthCallbackUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final SaveSlackChannelMappingPort saveSlackChannelMappingPort;
    private final ExchangeSlackOAuthCodePort exchangeSlackOAuthCodePort;
    private final String stateSecret;
    private final Clock clock;

    public CompleteSlackOAuthCallbackService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort,
            ExchangeSlackOAuthCodePort exchangeSlackOAuthCodePort,
            String stateSecret,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadSlackChannelMappingPort =
                Objects.requireNonNull(loadSlackChannelMappingPort, "loadSlackChannelMappingPort must not be null");
        this.saveSlackChannelMappingPort =
                Objects.requireNonNull(saveSlackChannelMappingPort, "saveSlackChannelMappingPort must not be null");
        this.exchangeSlackOAuthCodePort =
                Objects.requireNonNull(exchangeSlackOAuthCodePort, "exchangeSlackOAuthCodePort must not be null");
        this.stateSecret = requireNotBlank(stateSecret, "stateSecret");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CompleteSlackOAuthCallbackService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort,
            ExchangeSlackOAuthCodePort exchangeSlackOAuthCodePort,
            String stateSecret,
            Instant now) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadSlackChannelMappingPort,
                saveSlackChannelMappingPort,
                exchangeSlackOAuthCodePort,
                stateSecret,
                Clock.fixed(Objects.requireNonNull(now, "now must not be null"), ZoneOffset.UTC));
    }

    @Override
    public CompleteSlackOAuthCallbackResult completeSlackOAuthCallback(CompleteSlackOAuthCallbackCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SlackOAuthState state = SlackOAuthState.parse(command.state(), stateSecret, clock.instant());
        if (!state.requesterGoogleSub().equals(command.requesterGoogleSub())) {
            throw new IllegalArgumentException("slack oauth state requester mismatch");
        }
        Member requester = loadRequester(state.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(
                state.workspaceId(),
                requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        SlackOAuthCodeExchangeResult exchangeResult = exchangeSlackOAuthCodePort.exchangeCode(
                new SlackOAuthCodeExchangeRequest(command.code(), command.redirectUri()));
        requireNewChannelMapping(exchangeResult.teamId(), exchangeResult.channelId());

        SlackChannelMapping mapping = SlackChannelMapping.create(
                state.workspaceId(),
                exchangeResult.teamId(),
                exchangeResult.channelId(),
                exchangeResult.channelName(),
                exchangeResult.botToken(),
                requester.getGoogleSub());
        return toResult(saveSlackChannelMappingPort.save(mapping));
    }

    private String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
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

    private void requireAdmin(GetWorkspaceDetailResult workspaceDetail) {
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private void requireNewChannelMapping(String teamId, String channelId) {
        if (loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId(teamId, channelId).isPresent()) {
            throw new IllegalStateException("slack channel mapping already exists");
        }
    }

    private CompleteSlackOAuthCallbackResult toResult(SlackChannelMapping mapping) {
        return new CompleteSlackOAuthCallbackResult(
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
