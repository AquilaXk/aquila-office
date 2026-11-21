package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationResult;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * Slack 수동 연동 정보를 부분 수정하는 application service다.
 */
public class UpdateSlackIntegrationService implements UpdateSlackIntegrationUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final SaveSlackChannelMappingPort saveSlackChannelMappingPort;

    public UpdateSlackIntegrationService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadSlackChannelMappingPort =
                Objects.requireNonNull(loadSlackChannelMappingPort, "loadSlackChannelMappingPort must not be null");
        this.saveSlackChannelMappingPort =
                Objects.requireNonNull(saveSlackChannelMappingPort, "saveSlackChannelMappingPort must not be null");
    }

    @Override
    public UpdateSlackIntegrationResult updateSlackIntegration(UpdateSlackIntegrationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        SlackChannelMapping existing = loadSlackChannelMappingPort
                .findActiveByIdAndWorkspaceId(command.integrationId(), command.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("slack integration not found"));
        SlackChannelMapping updated = existing.update(
                command.slackTeamId(),
                command.slackChannelId(),
                command.channelName(),
                command.botToken());
        requireNoDuplicateChannel(existing, updated);
        return toResult(saveSlackChannelMappingPort.save(updated));
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

    private void requireNoDuplicateChannel(SlackChannelMapping existing, SlackChannelMapping updated) {
        if (existing.hasSameChannel(updated.getTeamId(), updated.getChannelId())) {
            return;
        }
        loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId(updated.getTeamId(), updated.getChannelId())
                .filter(mapping -> !Objects.equals(mapping.getId(), existing.getId()))
                .ifPresent(mapping -> {
                    throw new IllegalStateException("slack channel mapping already exists");
                });
    }

    private UpdateSlackIntegrationResult toResult(SlackChannelMapping mapping) {
        return new UpdateSlackIntegrationResult(
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
