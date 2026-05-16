package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveChatMessagePort;
import com.aquilaoffice.application.port.out.SaveChatSessionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SavedChatMessage;
import com.aquilaoffice.application.port.out.SavedChatSession;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.chat.ChatMessage;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.chat.ChatSession;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chat message 요청을 Task 생성과 실행 흐름으로 연결한다.
 */
public class SendChatMessageService implements SendChatMessageUseCase {

    private static final int DEFAULT_TITLE_LIMIT = 80;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final SaveChatSessionPort saveChatSessionPort;
    private final LoadChatSessionPort loadChatSessionPort;
    private final SaveTaskPort saveTaskPort;
    private final SaveChatMessagePort saveChatMessagePort;
    private final RunTaskUseCase runTaskUseCase;

    public SendChatMessageService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            SaveChatSessionPort saveChatSessionPort,
            LoadChatSessionPort loadChatSessionPort,
            SaveTaskPort saveTaskPort,
            SaveChatMessagePort saveChatMessagePort,
            RunTaskUseCase runTaskUseCase) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.saveChatSessionPort =
                Objects.requireNonNull(saveChatSessionPort, "saveChatSessionPort must not be null");
        this.loadChatSessionPort =
                Objects.requireNonNull(loadChatSessionPort, "loadChatSessionPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "saveTaskPort must not be null");
        this.saveChatMessagePort =
                Objects.requireNonNull(saveChatMessagePort, "saveChatMessagePort must not be null");
        this.runTaskUseCase = Objects.requireNonNull(runTaskUseCase, "runTaskUseCase must not be null");
    }

    @Override
    public SendChatMessageResult sendChatMessage(SendChatMessageCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeRequester(command.requesterGoogleSub());
        String message = normalizeMessage(command.message());
        Long agentId = requireAgentId(command.agentId());
        Member requester = requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(command.workspaceId(), requester.getGoogleSub());
        requireReadyAgent(command.workspaceId(), agentId);

        SavedChatSession session = resolveSession(command.workspaceId(), requester.getGoogleSub(), command.chatSessionId());
        SavedTask task = saveTaskPort.save(Task.create(
                command.workspaceId(),
                requester.getGoogleSub(),
                resolveTitle(command.title(), message),
                message,
                command.taskType(),
                command.priority(),
                agentId,
                command.sourceRef()));
        List<SavedChatMessage> savedMessages = new ArrayList<>();
        savedMessages.add(saveChatMessagePort.save(ChatMessage.create(
                command.workspaceId(),
                session.chatSessionId(),
                task.taskId(),
                null,
                ChatMessageRole.USER,
                message)));

        RunTaskResult runResult = runTaskUseCase.runTask(new RunTaskCommand(
                command.workspaceId(),
                task.taskId(),
                requester.getGoogleSub(),
                command.createPr()));
        saveAssistantMessageIfPresent(command.workspaceId(), session.chatSessionId(), task.taskId(), runResult)
                .ifPresent(savedMessages::add);

        return new SendChatMessageResult(
                session.chatSessionId(),
                task.taskId(),
                task.workspaceId(),
                agentId,
                runResult.taskStatus(),
                runResult.executionId(),
                runResult.executionStatus(),
                null,
                runResult.finalText(),
                runResult.failureReason(),
                savedMessages.get(0).createdAt(),
                savedMessages.stream().map(ChatMessageResultMapper::toResult).toList());
    }

    private java.util.Optional<SavedChatMessage> saveAssistantMessageIfPresent(
            Long workspaceId,
            Long chatSessionId,
            Long taskId,
            RunTaskResult runResult) {
        String content = runResult.finalText() != null ? runResult.finalText() : runResult.failureReason();
        if (content == null || content.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(saveChatMessagePort.save(ChatMessage.create(
                workspaceId,
                chatSessionId,
                taskId,
                runResult.executionId(),
                ChatMessageRole.ASSISTANT,
                content)));
    }

    private SavedChatSession resolveSession(Long workspaceId, String requesterGoogleSub, Long chatSessionId) {
        if (chatSessionId == null) {
            return saveChatSessionPort.save(ChatSession.create(workspaceId, requesterGoogleSub));
        }
        return loadChatSessionPort
                .findByWorkspaceIdAndChatSessionId(workspaceId, chatSessionId)
                .orElseThrow(() -> new IllegalArgumentException("chat session not found"));
    }

    private Member requireExistingMember(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private void requireReadyAgent(Long workspaceId, Long agentId) {
        loadWorkspaceAgentPort
                .findActiveByWorkspaceIdAndAgentId(workspaceId, agentId)
                .filter(agent -> agent.status() == WorkspaceAgentStatus.READY)
                .orElseThrow(() -> new IllegalArgumentException("ready workspace agent not found"));
    }

    private String normalizeRequester(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return requesterGoogleSub.trim();
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message.trim();
    }

    private Long requireAgentId(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("agentId must not be null");
        }
        return agentId;
    }

    private String resolveTitle(String title, String message) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return message.length() <= DEFAULT_TITLE_LIMIT ? message : message.substring(0, DEFAULT_TITLE_LIMIT);
    }
}
