package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ReceiveSlackEventCommand;
import com.aquilaoffice.application.port.in.ReceiveSlackEventResult;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import java.util.Objects;

/**
 * Slack event payload를 기존 chat/task 실행 흐름으로 변환한다.
 */
public class ReceiveSlackEventService implements ReceiveSlackEventUseCase {

    private static final String URL_VERIFICATION_TYPE = "url_verification";
    private static final String APP_MENTION_EVENT_TYPE = "app_mention";
    private static final int TITLE_LIMIT = 64;

    private final SlackAgentCommandParser slackAgentCommandParser;
    private final SendChatMessageUseCase sendChatMessageUseCase;

    public ReceiveSlackEventService(
            SlackAgentCommandParser slackAgentCommandParser,
            SendChatMessageUseCase sendChatMessageUseCase) {
        this.slackAgentCommandParser =
                Objects.requireNonNull(slackAgentCommandParser, "slackAgentCommandParser must not be null");
        this.sendChatMessageUseCase =
                Objects.requireNonNull(sendChatMessageUseCase, "sendChatMessageUseCase must not be null");
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

        String requesterGoogleSub = normalizeRequester(command.requesterGoogleSub());
        SendChatMessageResult sendResult = sendChatMessageUseCase.sendChatMessage(new SendChatMessageCommand(
                command.workspaceId(),
                requesterGoogleSub,
                parsedCommand.message(),
                parsedCommand.agentId(),
                null,
                TaskType.OTHER,
                TaskPriority.MEDIUM,
                resolveTitle(parsedCommand.message()),
                true,
                null));

        return new ReceiveSlackEventResult(
                null,
                true,
                null,
                sendResult.taskId(),
                sendResult.chatSessionId(),
                sourceRef(command.teamId(), event.channel(), event.ts(), event.threadTs()));
    }

    private ReceiveSlackEventResult ignored(String challenge, String reason) {
        return new ReceiveSlackEventResult(challenge, false, reason, null, null, null);
    }

    private String normalizeRequester(String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = normalize(requesterGoogleSub);
        if (normalizedRequesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return normalizedRequesterGoogleSub;
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
}
