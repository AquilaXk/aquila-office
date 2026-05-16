package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateSlackChannelMappingCommand;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Slack channel mapping application service가 멤버십과 중복 mapping을 검증하는지 확인한다.
 */
class SlackChannelMappingServiceTest {

    @Test
    void createSlackChannelMappingRequiresAdminAndSavesMapping() {
        FakeLoadSlackChannelMappingPort loadMappingPort =
                new FakeLoadSlackChannelMappingPort(Optional.empty(), List.of());
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        CreateSlackChannelMappingService service = new CreateSlackChannelMappingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                loadMappingPort,
                saveMappingPort);

        CreateSlackChannelMappingResult result = service.createSlackChannelMapping(
                new CreateSlackChannelMappingCommand(
                        101L, " google-1 ", " T123 ", " C456 ", " dev-alerts ", " xoxb-token-secret "));

        assertThat(loadMappingPort.lookupTeamId).isEqualTo("T123");
        assertThat(loadMappingPort.lookupChannelId).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveMappingPort.savedMapping.getTeamId()).isEqualTo("T123");
        assertThat(saveMappingPort.savedMapping.getChannelId()).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getBotToken()).isEqualTo("xoxb-token-secret");
        assertThat(result.mappingId()).isEqualTo(501L);
        assertThat(result.channelName()).isEqualTo("dev-alerts");
        assertThat(result.maskedBotToken()).isEqualTo("xoxb****cret");
    }

    @Test
    void createSlackChannelMappingRejectsDuplicateTeamChannel() {
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
        CreateSlackChannelMappingService service = new CreateSlackChannelMappingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadSlackChannelMappingPort(Optional.of(existing), List.of(existing)),
                saveMappingPort);

        assertThatThrownBy(() -> service.createSlackChannelMapping(
                        new CreateSlackChannelMappingCommand(
                                101L, "google-1", "T123", "C456", null, "xoxb-token-secret")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack channel mapping already exists");
        assertThat(saveMappingPort.called).isFalse();
    }

    @Test
    void listSlackChannelMappingsReturnsJoinedWorkspaceMappings() {
        SlackChannelMapping mapping = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "xoxb-token-secret",
                "google-1",
                true);
        ListSlackChannelMappingsService service = new ListSlackChannelMappingsService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadSlackChannelMappingPort(Optional.empty(), List.of(mapping)));

        List<ListSlackChannelMappingsResult> results = service.listSlackChannelMappings(
                new ListSlackChannelMappingsQuery(101L, " google-2 "));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.mappingId()).isEqualTo(501L);
            assertThat(result.teamId()).isEqualTo("T123");
            assertThat(result.channelId()).isEqualTo("C456");
            assertThat(result.maskedBotToken()).isEqualTo("xoxb****cret");
            assertThat(result.active()).isTrue();
        });
    }

    @Test
    void createSlackChannelMappingRejectsNonAdmin() {
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        CreateSlackChannelMappingService service = new CreateSlackChannelMappingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadSlackChannelMappingPort(Optional.empty(), List.of()),
                saveMappingPort);

        assertThatThrownBy(() -> service.createSlackChannelMapping(
                        new CreateSlackChannelMappingCommand(
                                101L, "google-2", "T123", "C456", null, "xoxb-token-secret")))
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
            throw new UnsupportedOperationException("not used in slack mapping service test");
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
        private final List<SlackChannelMapping> mappings;
        private String lookupTeamId;
        private String lookupChannelId;

        private FakeLoadSlackChannelMappingPort(
                Optional<SlackChannelMapping> mapping,
                List<SlackChannelMapping> mappings) {
            this.mapping = mapping;
            this.mappings = mappings;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId) {
            throw new UnsupportedOperationException("not used in slack mapping service test");
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            this.lookupTeamId = teamId;
            this.lookupChannelId = channelId;
            return mapping;
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            return mappings;
        }
    }

    private static class FakeSaveSlackChannelMappingPort implements SaveSlackChannelMappingPort {
        private SlackChannelMapping savedMapping;
        private boolean called;

        @Override
        public SlackChannelMapping save(SlackChannelMapping mapping) {
            this.called = true;
            this.savedMapping = mapping;
            return SlackChannelMapping.restore(
                    501L,
                    mapping.getWorkspaceId(),
                    mapping.getTeamId(),
                    mapping.getChannelId(),
                    mapping.getChannelName(),
                    mapping.getBotToken(),
                    mapping.getCreatedByGoogleSub(),
                    mapping.isActive());
        }
    }
}
