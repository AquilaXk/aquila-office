package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SendSlackReplyPort;
import com.aquilaoffice.application.port.out.SlackReplyRequest;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReceiveSlackEventServiceTest {

    @Test
    void receiveSlackEventReturnsChallengeForUrlVerification() {
        FakeSendChatMessageUseCase sendChatMessageUseCase = new FakeSendChatMessageUseCase();
        ReceiveSlackEventUseCase service = newService(sendChatMessageUseCase);

        ReceiveSlackEventResult result = service.receiveSlackEvent(new ReceiveSlackEventCommand(
                101L,
                "google-1",
                "url_verification",
                "challenge-token",
                "T123",
                "E123",
                null));

        assertThat(result.challenge()).isEqualTo("challenge-token");
        assertThat(result.processed()).isFalse();
        assertThat(result.ignoredReason()).isEqualTo("url verification");
        assertThat(sendChatMessageUseCase.called).isFalse();
    }

    @Test
    void receiveSlackEventDispatchesAppMentionToChatTaskFlow() {
        FakeSendChatMessageUseCase sendChatMessageUseCase =
                new FakeSendChatMessageUseCase("구현 완료", null);
        FakeSendSlackReplyPort sendSlackReplyPort = new FakeSendSlackReplyPort();
        ReceiveSlackEventUseCase service = newService(
                sendChatMessageUseCase,
                new FakeLoadSlackChannelMappingPort(Optional.empty()),
                sendSlackReplyPort);

        ReceiveSlackEventResult result = service.receiveSlackEvent(new ReceiveSlackEventCommand(
                101L,
                " google-1 ",
                "event_callback",
                null,
                "T123",
                "E123",
                new ReceiveSlackEventCommand.SlackEventPayload(
                        "app_mention",
                        "C456",
                        "U789",
                        "<@U123ABC> /agent 501 결제 오류를 수정해주세요",
                        "1715841000.000100",
                        null)));

        assertThat(sendChatMessageUseCase.called).isTrue();
        assertThat(sendChatMessageUseCase.command.workspaceId()).isEqualTo(101L);
        assertThat(sendChatMessageUseCase.command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(sendChatMessageUseCase.command.agentId()).isEqualTo(501L);
        assertThat(sendChatMessageUseCase.command.message()).isEqualTo("결제 오류를 수정해주세요");
        assertThat(sendChatMessageUseCase.command.taskType()).isEqualTo(TaskType.OTHER);
        assertThat(sendChatMessageUseCase.command.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(sendChatMessageUseCase.command.createPr()).isTrue();
        assertThat(sendChatMessageUseCase.command.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
        assertThat(result.processed()).isTrue();
        assertThat(result.taskId()).isEqualTo(301L);
        assertThat(result.chatSessionId()).isEqualTo(901L);
        assertThat(result.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
        assertThat(sendSlackReplyPort.request.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
        assertThat(sendSlackReplyPort.request.text()).isEqualTo("구현 완료");
        assertThat(sendSlackReplyPort.request.deduplicationKey()).isEqualTo("task-execution-701");
    }

    @Test
    void receiveSlackEventResolvesWorkspaceAndRequesterFromActiveChannelMapping() {
        FakeSendChatMessageUseCase sendChatMessageUseCase = new FakeSendChatMessageUseCase();
        ReceiveSlackEventUseCase service = newService(
                sendChatMessageUseCase,
                new FakeLoadSlackChannelMappingPort(Optional.of(SlackChannelMapping.restore(
                        801L,
                        202L,
                        "T123",
                        "C456",
                        "dev-alerts",
                        "xoxb-token-secret",
                        "google-mapped",
                        true))));

        ReceiveSlackEventResult result = service.receiveSlackEvent(new ReceiveSlackEventCommand(
                null,
                null,
                "event_callback",
                null,
                "T123",
                "E123",
                new ReceiveSlackEventCommand.SlackEventPayload(
                        "app_mention",
                        "C456",
                        "U789",
                        "<@U123ABC> /agent 501 장애를 확인해주세요",
                        "1715841000.000100",
                        null)));

        assertThat(result.processed()).isTrue();
        assertThat(sendChatMessageUseCase.called).isTrue();
        assertThat(sendChatMessageUseCase.command.workspaceId()).isEqualTo(202L);
        assertThat(sendChatMessageUseCase.command.requesterGoogleSub()).isEqualTo("google-mapped");
    }

    @Test
    void receiveSlackEventIgnoresMappedEventWhenChannelMappingIsMissing() {
        FakeSendChatMessageUseCase sendChatMessageUseCase = new FakeSendChatMessageUseCase();
        ReceiveSlackEventUseCase service = newService(
                sendChatMessageUseCase,
                new FakeLoadSlackChannelMappingPort(Optional.empty()));

        ReceiveSlackEventResult result = service.receiveSlackEvent(new ReceiveSlackEventCommand(
                null,
                null,
                "event_callback",
                null,
                "T123",
                "E123",
                new ReceiveSlackEventCommand.SlackEventPayload(
                        "app_mention",
                        "C456",
                        "U789",
                        "<@U123ABC> /agent 501 장애를 확인해주세요",
                        "1715841000.000100",
                        null)));

        assertThat(result.processed()).isFalse();
        assertThat(result.ignoredReason()).isEqualTo("slack channel mapping not found");
        assertThat(sendChatMessageUseCase.called).isFalse();
    }

    @Test
    void receiveSlackEventIgnoresUnsupportedEventWithoutDispatch() {
        FakeSendChatMessageUseCase sendChatMessageUseCase = new FakeSendChatMessageUseCase();
        ReceiveSlackEventUseCase service = newService(sendChatMessageUseCase);

        ReceiveSlackEventResult result = service.receiveSlackEvent(new ReceiveSlackEventCommand(
                101L,
                "google-1",
                "event_callback",
                null,
                "T123",
                "E123",
                new ReceiveSlackEventCommand.SlackEventPayload(
                        "message",
                        "C456",
                        "U789",
                        "hello",
                        "1715841000.000100",
                        null)));

        assertThat(result.processed()).isFalse();
        assertThat(result.ignoredReason()).isEqualTo("unsupported event type");
        assertThat(sendChatMessageUseCase.called).isFalse();
    }

    private static ReceiveSlackEventUseCase newService(SendChatMessageUseCase sendChatMessageUseCase) {
        return newService(
                sendChatMessageUseCase,
                new FakeLoadSlackChannelMappingPort(Optional.empty()),
                new FakeSendSlackReplyPort());
    }

    private static ReceiveSlackEventUseCase newService(
            SendChatMessageUseCase sendChatMessageUseCase,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort) {
        return newService(sendChatMessageUseCase, loadSlackChannelMappingPort, new FakeSendSlackReplyPort());
    }

    private static ReceiveSlackEventUseCase newService(
            SendChatMessageUseCase sendChatMessageUseCase,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SendSlackReplyPort sendSlackReplyPort) {
        return new ReceiveSlackEventService(
                new SlackAgentCommandParser(),
                sendChatMessageUseCase,
                loadSlackChannelMappingPort,
                sendSlackReplyPort);
    }

    private static class FakeLoadSlackChannelMappingPort implements LoadSlackChannelMappingPort {
        private final Optional<SlackChannelMapping> mapping;

        private FakeLoadSlackChannelMappingPort(Optional<SlackChannelMapping> mapping) {
            this.mapping = mapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            return mapping;
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in receive slack event service test");
        }
    }

    private static class FakeSendChatMessageUseCase implements SendChatMessageUseCase {
        private final String finalText;
        private final String failureReason;
        private boolean called;
        private SendChatMessageCommand command;

        private FakeSendChatMessageUseCase() {
            this(null, null);
        }

        private FakeSendChatMessageUseCase(String finalText, String failureReason) {
            this.finalText = finalText;
            this.failureReason = failureReason;
        }

        @Override
        public SendChatMessageResult sendChatMessage(SendChatMessageCommand command) {
            this.called = true;
            this.command = command;
            return new SendChatMessageResult(
                    901L,
                    301L,
                    101L,
                    command.agentId(),
                    TaskStatus.IN_PROGRESS,
                    701L,
                    TaskExecutionStatus.RUNNING,
                    null,
                    finalText,
                    failureReason,
                    LocalDateTime.of(2026, 5, 16, 15, 0),
                    List.of());
        }
    }

    private static class FakeSendSlackReplyPort implements SendSlackReplyPort {
        private SlackReplyRequest request;

        @Override
        public void sendReply(SlackReplyRequest request) {
            this.request = request;
        }
    }
}
