package com.aquilaoffice.application.service.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackCommand;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.ExchangeSlackOAuthCodePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeRequest;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeResult;
import com.aquilaoffice.application.service.CompleteSlackOAuthCallbackService;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.slack.SlackOAuthState;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompleteSlackOAuthCallbackServiceTest {

    @Test
    void completeSlackOAuthCallbackExchangesCodeAndSavesMapping() {
        FakeExchangeSlackOAuthCodePort exchangePort = new FakeExchangeSlackOAuthCodePort(
                new SlackOAuthCodeExchangeResult("T123", "C456", "dev-alerts", "xoxb-token-secret"));
        FakeLoadSlackChannelMappingPort loadMappingPort =
                new FakeLoadSlackChannelMappingPort(Optional.empty());
        FakeSaveSlackChannelMappingPort saveMappingPort = new FakeSaveSlackChannelMappingPort();
        CompleteSlackOAuthCallbackService service = new CompleteSlackOAuthCallbackService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                loadMappingPort,
                saveMappingPort,
                exchangePort);

        CompleteSlackOAuthCallbackResult result = service.completeSlackOAuthCallback(
                new CompleteSlackOAuthCallbackCommand(
                        " code-789 ",
                        SlackOAuthState.serialize(101L, "google-1"),
                        " http://localhost:8080/api/v1/slack/oauth/callback "));

        assertThat(exchangePort.request.code()).isEqualTo("code-789");
        assertThat(exchangePort.request.redirectUri())
                .isEqualTo("http://localhost:8080/api/v1/slack/oauth/callback");
        assertThat(loadMappingPort.lookupTeamId).isEqualTo("T123");
        assertThat(loadMappingPort.lookupChannelId).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveMappingPort.savedMapping.getTeamId()).isEqualTo("T123");
        assertThat(saveMappingPort.savedMapping.getChannelId()).isEqualTo("C456");
        assertThat(saveMappingPort.savedMapping.getChannelName()).isEqualTo("dev-alerts");
        assertThat(saveMappingPort.savedMapping.getBotToken()).isEqualTo("xoxb-token-secret");
        assertThat(saveMappingPort.savedMapping.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.slackTeamId()).isEqualTo("T123");
        assertThat(result.slackChannelId()).isEqualTo("C456");
        assertThat(result.maskedBotToken()).isEqualTo("xoxb****cret");
    }

    @Test
    void completeSlackOAuthCallbackRejectsInvalidStateBeforeExchange() {
        FakeExchangeSlackOAuthCodePort exchangePort = new FakeExchangeSlackOAuthCodePort(
                new SlackOAuthCodeExchangeResult("T123", "C456", "dev-alerts", "xoxb-token-secret"));
        CompleteSlackOAuthCallbackService service = new CompleteSlackOAuthCallbackService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                new FakeLoadSlackChannelMappingPort(Optional.empty()),
                new FakeSaveSlackChannelMappingPort(),
                exchangePort);

        assertThatThrownBy(() -> service.completeSlackOAuthCallback(
                        new CompleteSlackOAuthCallbackCommand(
                                "code-789",
                                "workspace:101:google-1",
                                "http://localhost:8080/api/v1/slack/oauth/callback")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state is invalid");
        assertThat(exchangePort.called).isFalse();
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
            throw new UnsupportedOperationException("not used in complete slack oauth callback service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadSlackChannelMappingPort implements LoadSlackChannelMappingPort {
        private final Optional<SlackChannelMapping> duplicateMapping;
        private String lookupTeamId;
        private String lookupChannelId;

        private FakeLoadSlackChannelMappingPort(Optional<SlackChannelMapping> duplicateMapping) {
            this.duplicateMapping = duplicateMapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId) {
            throw new UnsupportedOperationException("not used in complete slack oauth callback service test");
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            this.lookupTeamId = teamId;
            this.lookupChannelId = channelId;
            return duplicateMapping;
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in complete slack oauth callback service test");
        }
    }

    private static class FakeSaveSlackChannelMappingPort implements SaveSlackChannelMappingPort {
        private SlackChannelMapping savedMapping;

        @Override
        public SlackChannelMapping save(SlackChannelMapping mapping) {
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

    private static class FakeExchangeSlackOAuthCodePort implements ExchangeSlackOAuthCodePort {
        private final SlackOAuthCodeExchangeResult result;
        private SlackOAuthCodeExchangeRequest request;
        private boolean called;

        private FakeExchangeSlackOAuthCodePort(SlackOAuthCodeExchangeResult result) {
            this.result = result;
        }

        @Override
        public SlackOAuthCodeExchangeResult exchangeCode(SlackOAuthCodeExchangeRequest request) {
            this.called = true;
            this.request = request;
            return result;
        }
    }
}
