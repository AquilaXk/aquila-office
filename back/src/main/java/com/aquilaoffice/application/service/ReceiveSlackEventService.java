package com.aquilaoffice.application.service;

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
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Slack event payload를 기존 chat/task 실행 흐름으로 변환한다.
 */
public class ReceiveSlackEventService implements ReceiveSlackEventUseCase {

    private static final String URL_VERIFICATION_TYPE = "url_verification";
    private static final String APP_MENTION_EVENT_TYPE = "app_mention";
    private static final int TITLE_LIMIT = 64;
    private static final SendSlackReplyPort NOOP_SLACK_REPLY_PORT = request -> {};

    private final SlackAgentCommandParser slackAgentCommandParser;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final SendSlackReplyPort sendSlackReplyPort;

    public ReceiveSlackEventService(
            SlackAgentCommandParser slackAgentCommandParser,
            SendChatMessageUseCase sendChatMessageUseCase) {
        this(slackAgentCommandParser, sendChatMessageUseCase, new EmptySlackChannelMappingPort());
    }

    public ReceiveSlackEventService(
            SlackAgentCommandParser slackAgentCommandParser,
            SendChatMessageUseCase sendChatMessageUseCase,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort) {
        this(slackAgentCommandParser, sendChatMessageUseCase, loadSlackChannelMappingPort, NOOP_SLACK_REPLY_PORT);
    }

    public ReceiveSlackEventService(
            SlackAgentCommandParser slackAgentCommandParser,
            SendChatMessageUseCase sendChatMessageUseCase,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SendSlackReplyPort sendSlackReplyPort) {
        this.slackAgentCommandParser =
                Objects.requireNonNull(slackAgentCommandParser, "slackAgentCommandParser must not be null");
        this.sendChatMessageUseCase =
                Objects.requireNonNull(sendChatMessageUseCase, "sendChatMessageUseCase must not be null");
        this.loadSlackChannelMappingPort =
                Objects.requireNonNull(loadSlackChannelMappingPort, "loadSlackChannelMappingPort must not be null");
        this.sendSlackReplyPort =
                Objects.requireNonNull(sendSlackReplyPort, "sendSlackReplyPort must not be null");
    }

    @Override
    public ReceiveSlackEventResult receiveSlackEvent(ReceiveSlackEventCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String type = normalize(command.type());
        if (URL_VERIFICATION_TYPE.equals(type)) {
            return ignored(command.challenge(), "url verification");
        }

        ReceiveSlackEventCommand.SlackEventPayload event = command.event();
        if (event == null || !APP_MENTION_EVENT_TYPE.equals(normalize(event.type()))) {
            return ignored(null, "unsupported event type");
        }

        SlackAgentCommandParser.ParsedCommand parsedCommand = slackAgentCommandParser.parse(event.text());
        if (!parsedCommand.matched()) {
            return ignored(null, "agent command not found");
        }

        ResolvedSlackWorkspace resolvedWorkspace = resolveWorkspace(command, event);
        if (resolvedWorkspace == null) {
            return ignored(null, "slack channel mapping not found");
        }
        String eventSourceRef = sourceRef(command.teamId(), event.channel(), event.ts(), event.threadTs());
        SendChatMessageResult sendResult = sendChatMessageUseCase.sendChatMessage(new SendChatMessageCommand(
                resolvedWorkspace.workspaceId(),
                resolvedWorkspace.requesterGoogleSub(),
                parsedCommand.message(),
                parsedCommand.agentId(),
                null,
                TaskType.OTHER,
                TaskPriority.MEDIUM,
                resolveTitle(parsedCommand.message()),
                true,
                null,
                eventSourceRef));
        sendReplyIfPresent(eventSourceRef, sendResult);

        return new ReceiveSlackEventResult(
                null,
                true,
                null,
                sendResult.taskId(),
                sendResult.chatSessionId(),
                eventSourceRef);
    }

    private void sendReplyIfPresent(String sourceRef, SendChatMessageResult sendResult) {
        String replyText = resolveReplyText(sendResult);
        if (replyText.isBlank()) {
            return;
        }
        sendSlackReplyPort.sendReply(SlackReplyRequest.fromSourceRef(
                sourceRef,
                replyText,
                replyDeduplicationKey(sendResult.taskExecutionId())));
    }

    private String resolveReplyText(SendChatMessageResult sendResult) {
        String finalText = normalize(sendResult.finalText());
        if (!finalText.isBlank()) {
            return finalText;
        }
        return normalize(sendResult.failureReason());
    }

    private String replyDeduplicationKey(Long taskExecutionId) {
        if (taskExecutionId == null) {
            return null;
        }
        return "task-execution-" + taskExecutionId;
    }

    private ResolvedSlackWorkspace resolveWorkspace(
            ReceiveSlackEventCommand command,
            ReceiveSlackEventCommand.SlackEventPayload event) {
        String requesterGoogleSub = normalize(command.requesterGoogleSub());
        if (command.workspaceId() != null && !requesterGoogleSub.isBlank()) {
            return new ResolvedSlackWorkspace(command.workspaceId(), requesterGoogleSub);
        }

        String teamId = normalize(command.teamId());
        String channelId = normalize(event.channel());
        if (teamId.isBlank() || channelId.isBlank()) {
            return null;
        }

        return loadSlackChannelMappingPort.findActiveByTeamIdAndChannelId(teamId, channelId)
                .map(mapping -> new ResolvedSlackWorkspace(
                        mapping.getWorkspaceId(),
                        mapping.getCreatedByGoogleSub()))
                .orElse(null);
    }

    private ReceiveSlackEventResult ignored(String challenge, String reason) {
        return new ReceiveSlackEventResult(challenge, false, reason, null, null, null);
    }

    private String resolveTitle(String message) {
        String normalizedMessage = normalize(message);
        String titleBody = normalizedMessage.length() <= TITLE_LIMIT
                ? normalizedMessage
                : normalizedMessage.substring(0, TITLE_LIMIT);
        return "Slack 요청: " + titleBody;
    }

    private String sourceRef(String teamId, String channelId, String ts, String threadTs) {
        String targetTs = normalize(threadTs).isBlank() ? normalize(ts) : normalize(threadTs);
        return normalize(teamId) + ":" + normalize(channelId) + ":" + targetTs;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record ResolvedSlackWorkspace(Long workspaceId, String requesterGoogleSub) {}

    private static class EmptySlackChannelMappingPort implements LoadSlackChannelMappingPort {

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            return Optional.empty();
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            return List.of();
        }
    }
}
