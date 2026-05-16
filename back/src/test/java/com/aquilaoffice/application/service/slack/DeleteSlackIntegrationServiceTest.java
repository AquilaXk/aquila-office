package com.aquilaoffice.application.service.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.DeleteSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.service.DeleteSlackIntegrationService;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteSlackIntegrationServiceTest {

    @Test
    void deleteSlackIntegrationRequiresAdminAndDeactivatesMapping() {
        SlackChannelMapping existing = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-token-secret",
                "google-1",
                true);
        FakeLoadSlackChannelMappingPort loadMappingPort = new FakeLoadSlackChannelMappingPort(Optional.of(existing));
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        DeleteSlackIntegrationService service = new DeleteSlackIntegrationService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                loadMappingPort,
                saveMappingPort);

        DeleteSlackIntegrationResult result = service.deleteSlackIntegration(
                new DeleteSlackIntegrationCommand(101L, " google-1 ", 501L));

        assertThat(loadMappingPort.lookupMappingId).isEqualTo(501L);
        assertThat(loadMappingPort.lookupWorkspaceId).isEqualTo(101L);
        assertThat(saveMappingPort.savedMapping.getId()).isEqualTo(501L);
        assertThat(saveMappingPort.savedMapping.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveMappingPort.savedMapping.isActive()).isFalse();
        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.active()).isFalse();
    }

    @Test
    void deleteSlackIntegrationRejectsNonAdmin() {
        SlackChannelMapping existing = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-token-secret",
                "google-1",
                true);
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        DeleteSlackIntegrationService service = new DeleteSlackIntegrationService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadSlackChannelMappingPort(Optional.of(existing)),
                saveMappingPort);

        assertThatThrownBy(() -> service.deleteSlackIntegration(
                        new DeleteSlackIntegrationCommand(101L, "google-2", 501L)))
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
            throw new UnsupportedOperationException("not used in delete slack integration service test");
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
        private Long lookupMappingId;
        private Long lookupWorkspaceId;

        private FakeLoadSlackChannelMappingPort(Optional<SlackChannelMapping> mapping) {
            this.mapping = mapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId) {
            this.lookupMappingId = mappingId;
            this.lookupWorkspaceId = workspaceId;
            return mapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            throw new UnsupportedOperationException("not used in delete slack integration service test");
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in delete slack integration service test");
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
