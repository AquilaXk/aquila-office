package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.time.LocalDateTime;
import java.util.List;
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
        FakeSendChatMessageUseCase sendChatMessageUseCase = new FakeSendChatMessageUseCase();
        ReceiveSlackEventUseCase service = newService(sendChatMessageUseCase);

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
        assertThat(result.processed()).isTrue();
        assertThat(result.taskId()).isEqualTo(301L);
        assertThat(result.chatSessionId()).isEqualTo(901L);
        assertThat(result.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
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
        return new ReceiveSlackEventService(
                new SlackAgentCommandParser(),
                sendChatMessageUseCase);
    }

    private static class FakeSendChatMessageUseCase implements SendChatMessageUseCase {
        private boolean called;
        private SendChatMessageCommand command;

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
                    null,
                    null,
                    LocalDateTime.of(2026, 5, 16, 15, 0),
                    List.of());
        }
    }
}
