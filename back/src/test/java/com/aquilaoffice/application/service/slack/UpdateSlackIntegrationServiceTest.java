package com.aquilaoffice.application.service.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.service.UpdateSlackIntegrationService;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateSlackIntegrationServiceTest {

    @Test
    void updateSlackIntegrationRequiresAdminAndUpdatesOnlyNonBlankFields() {
        SlackChannelMapping existing = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-old-secret",
                "google-1",
                true);
        FakeLoadSlackChannelMappingPort loadMappingPort =
                new FakeLoadSlackChannelMappingPort(Optional.of(existing), Optional.empty());
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        UpdateSlackIntegrationService service = new UpdateSlackIntegrationService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                loadMappingPort,
                saveMappingPort);

        UpdateSlackIntegrationResult result = service.updateSlackIntegration(
                new UpdateSlackIntegrationCommand(
                        101L,
                        " google-1 ",
                        501L,
                        " T999 ",
                        " ",
                        " prod-alerts ",
                        " xoxb-new-secret "));

        assertThat(loadMappingPort.lookupMappingId).isEqualTo(501L);
        assertThat(loadMappingPort.lookupWorkspaceId).isEqualTo(101L);
        assertThat(loadMappingPort.duplicateLookupTeamId).isEqualTo("T999");
        assertThat(loadMappingPort.duplicateLookupChannelId).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getId()).isEqualTo(501L);
        assertThat(saveMappingPort.savedMapping.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveMappingPort.savedMapping.getTeamId()).isEqualTo("T999");
        assertThat(saveMappingPort.savedMapping.getChannelId()).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getChannelName()).isEqualTo("prod-alerts");
        assertThat(saveMappingPort.savedMapping.getBotToken()).isEqualTo("xoxb-new-secret");
        assertThat(saveMappingPort.savedMapping.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(saveMappingPort.savedMapping.isActive()).isTrue();
        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.slackTeamId()).isEqualTo("T999");
        assertThat(result.slackChannelId()).isEqualTo("C456");
        assertThat(result.maskedBotToken()).isEqualTo("xox****cret");
    }

    @Test
    void updateSlackIntegrationRejectsNonAdmin() {
        SlackChannelMapping existing = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-old-secret",
                "google-1",
                true);
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        UpdateSlackIntegrationService service = new UpdateSlackIntegrationService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadSlackChannelMappingPort(Optional.of(existing), Optional.empty()),
                saveMappingPort);

        assertThatThrownBy(() -> service.updateSlackIntegration(
                        new UpdateSlackIntegrationCommand(
                                101L,
                                "google-2",
                                501L,
                                "T999",
                                null,
                                null,
                                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(saveMappingPort.called).isFalse();
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detail;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detail) {
            this.detail = detail;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in update slack integration service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadSlackChannelMappingPort implements LoadSlackChannelMappingPort {
        private final Optional<SlackChannelMapping> mapping;
        private final Optional<SlackChannelMapping> duplicateMapping;
        private Long lookupMappingId;
        private Long lookupWorkspaceId;
        private String duplicateLookupTeamId;
        private String duplicateLookupChannelId;

        private FakeLoadSlackChannelMappingPort(
                Optional<SlackChannelMapping> mapping,
                Optional<SlackChannelMapping> duplicateMapping) {
            this.mapping = mapping;
            this.duplicateMapping = duplicateMapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId) {
            this.lookupMappingId = mappingId;
            this.lookupWorkspaceId = workspaceId;
            return mapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            this.duplicateLookupTeamId = teamId;
            this.duplicateLookupChannelId = channelId;
            return duplicateMapping;
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in update slack integration service test");
        }
    }

    private static class FakeSaveSlackChannelMappingPort implements SaveSlackChannelMappingPort {
        private SlackChannelMapping savedMapping;
        private boolean called;

        @Override
        public SlackChannelMapping save(SlackChannelMapping mapping) {
            this.called = true;
            this.savedMapping = mapping;
            return mapping;
        }
    }
}
