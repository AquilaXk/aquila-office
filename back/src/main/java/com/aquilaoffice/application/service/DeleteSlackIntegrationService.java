package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.DeleteSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationResult;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * Slack 수동 연동을 비활성화하는 application service다.
 */
public class DeleteSlackIntegrationService implements DeleteSlackIntegrationUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final SaveSlackChannelMappingPort saveSlackChannelMappingPort;

    public DeleteSlackIntegrationService(
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
    public DeleteSlackIntegrationResult deleteSlackIntegration(DeleteSlackIntegrationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        SlackChannelMapping mapping = loadSlackChannelMappingPort
                .findActiveByIdAndWorkspaceId(command.integrationId(), command.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("slack integration not found"));
        mapping.deactivate();
        return toResult(saveSlackChannelMappingPort.save(mapping));
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

    private DeleteSlackIntegrationResult toResult(SlackChannelMapping mapping) {
        return new DeleteSlackIntegrationResult(
                mapping.getId(),
                mapping.getWorkspaceId(),
                mapping.isActive());
    }
}
