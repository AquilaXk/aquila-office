package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
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
 * Slack team/channel을 workspace에 연결하는 mapping 생성 유스케이스를 수행한다.
 */
public class CreateSlackChannelMappingService implements CreateSlackChannelMappingUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final SaveSlackChannelMappingPort saveSlackChannelMappingPort;

    public CreateSlackChannelMappingService(
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
    public CreateSlackChannelMappingResult createSlackChannelMapping(CreateSlackChannelMappingCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);
        requireNewChannelMapping(command.teamId(), command.channelId());

        SlackChannelMapping mapping = SlackChannelMapping.create(
                command.workspaceId(),
                command.teamId(),
                command.channelId(),
                command.channelName(),
                command.botToken(),
                requester.getGoogleSub());
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

    private void requireNewChannelMapping(String teamId, String channelId) {
        if (loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId(teamId, channelId).isPresent()) {
            throw new IllegalStateException("slack channel mapping already exists");
        }
    }

    private CreateSlackChannelMappingResult toResult(SlackChannelMapping mapping) {
        return new CreateSlackChannelMappingResult(
                mapping.getId(),
                mapping.getWorkspaceId(),
                mapping.getTeamId(),
                mapping.getChannelId(),
                mapping.getChannelName(),
                mapping.getCreatedByGoogleSub(),
                mapping.isActive());
    }
}
