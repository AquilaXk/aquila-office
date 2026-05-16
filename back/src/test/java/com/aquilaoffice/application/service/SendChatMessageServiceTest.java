package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
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
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.chat.ChatMessage;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.chat.ChatSession;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SendChatMessageServiceTest {

    @Test
    void sendNewChatMessageCreatesSessionTaskMessagesAndRunsTask() {
        FakeSaveChatSessionPort saveChatSessionPort = new FakeSaveChatSessionPort(savedSession(901L));
        FakeSaveTaskPort saveTaskPort = new FakeSaveTaskPort(savedTask(301L));
        FakeSaveChatMessagePort saveChatMessagePort = new FakeSaveChatMessagePort();
        FakeRunTaskUseCase runTaskUseCase = new FakeRunTaskUseCase(new RunTaskResult(
                101L,
                301L,
                701L,
                501L,
                TaskStatus.COMPLETED,
                TaskExecutionStatus.SUCCEEDED,
                "구현 완료",
                null));
        SendChatMessageService service = newService(
                saveChatSessionPort,
                new FakeLoadChatSessionPort(Optional.empty()),
                saveTaskPort,
                saveChatMessagePort,
                runTaskUseCase);

        SendChatMessageResult result = service.sendChatMessage(new SendChatMessageCommand(
                101L,
                " google-1 ",
                "  기능을 구현해 주세요.  ",
                501L,
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                " 채팅 요청 ",
                true,
                null));

        assertThat(saveChatSessionPort.savedSession.getWorkspaceId()).isEqualTo(101L);
        assertThat(saveTaskPort.savedTask.getTitle()).isEqualTo("채팅 요청");
        assertThat(saveTaskPort.savedTask.getDescription()).isEqualTo("기능을 구현해 주세요.");
        assertThat(saveTaskPort.savedTask.getAssignedAgentId()).isEqualTo(501L);
        assertThat(runTaskUseCase.command.taskId()).isEqualTo(301L);
        assertThat(runTaskUseCase.command.createPr()).isTrue();
        assertThat(saveChatMessagePort.savedMessages).hasSize(2);
        assertThat(saveChatMessagePort.savedMessages.get(0).getRole()).isEqualTo(ChatMessageRole.USER);
        assertThat(saveChatMessagePort.savedMessages.get(1).getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(saveChatMessagePort.savedMessages.get(1).getTaskExecutionId()).isEqualTo(701L);
        assertThat(result.chatSessionId()).isEqualTo(901L);
        assertThat(result.taskId()).isEqualTo(301L);
        assertThat(result.taskExecutionId()).isEqualTo(701L);
        assertThat(result.messages()).hasSize(2);
        assertThat(result.finalText()).isEqualTo("구현 완료");
    }

    @Test
    void sendMessageCanAppendToExistingSessionAndUsesMessageAsDefaultTitle() {
        FakeSaveChatSessionPort saveChatSessionPort = new FakeSaveChatSessionPort(savedSession(902L));
        FakeSaveTaskPort saveTaskPort = new FakeSaveTaskPort(savedTask(302L));
        FakeSaveChatMessagePort saveChatMessagePort = new FakeSaveChatMessagePort();
        SendChatMessageService service = newService(
                saveChatSessionPort,
                new FakeLoadChatSessionPort(Optional.of(savedSession(902L))),
                saveTaskPort,
                saveChatMessagePort,
                new FakeRunTaskUseCase(new RunTaskResult(
                        101L,
                        302L,
                        702L,
                        501L,
                        TaskStatus.FAILED,
                        TaskExecutionStatus.FAILED,
                        null,
                        "gateway failed")));

        SendChatMessageResult result = service.sendChatMessage(new SendChatMessageCommand(
                101L,
                "google-1",
                "버그를 수정하고 테스트를 추가해 주세요.",
                501L,
                null,
                null,
                null,
                null,
                false,
                902L));

        assertThat(saveChatSessionPort.savedSession).isNull();
        assertThat(saveTaskPort.savedTask.getTitle()).isEqualTo("버그를 수정하고 테스트를 추가해 주세요.");
        assertThat(saveTaskPort.savedTask.getType()).isEqualTo(TaskType.OTHER);
        assertThat(saveTaskPort.savedTask.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(saveChatMessagePort.savedMessages.get(1).getContent()).isEqualTo("gateway failed");
        assertThat(result.chatSessionId()).isEqualTo(902L);
        assertThat(result.failureReason()).isEqualTo("gateway failed");
    }

    @Test
    void sendMessageRejectsBlankMessage() {
        SendChatMessageService service = newService(
                new FakeSaveChatSessionPort(savedSession(901L)),
                new FakeLoadChatSessionPort(Optional.empty()),
                new FakeSaveTaskPort(savedTask(301L)),
                new FakeSaveChatMessagePort(),
                new FakeRunTaskUseCase(null));

        assertThatThrownBy(() -> service.sendChatMessage(new SendChatMessageCommand(
                        101L,
                        "google-1",
                        " ",
                        501L,
                        null,
                        TaskType.FEATURE_IMPLEMENTATION,
                        TaskPriority.HIGH,
                        null,
                        false,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message must not be blank");
    }

    @Test
    void sendMessageRejectsMissingSession() {
        SendChatMessageService service = newService(
                new FakeSaveChatSessionPort(savedSession(901L)),
                new FakeLoadChatSessionPort(Optional.empty()),
                new FakeSaveTaskPort(savedTask(301L)),
                new FakeSaveChatMessagePort(),
                new FakeRunTaskUseCase(null));

        assertThatThrownBy(() -> service.sendChatMessage(new SendChatMessageCommand(
                        101L,
                        "google-1",
                        "작업해 주세요.",
                        501L,
                        null,
                        TaskType.FEATURE_IMPLEMENTATION,
                        TaskPriority.HIGH,
                        null,
                        false,
                        999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chat session not found");
    }

    private static SendChatMessageService newService(
            SaveChatSessionPort saveChatSessionPort,
            LoadChatSessionPort loadChatSessionPort,
            SaveTaskPort saveTaskPort,
            SaveChatMessagePort saveChatMessagePort,
            RunTaskUseCase runTaskUseCase) {
        return new SendChatMessageService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                new FakeLoadWorkspaceAgentPort(Optional.of(savedAgent())),
                saveChatSessionPort,
                loadChatSessionPort,
                saveTaskPort,
                saveChatMessagePort,
                runTaskUseCase);
    }

    private static GetWorkspaceDetailResult workspaceDetail() {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedWorkspaceAgent savedAgent() {
        return new SavedWorkspaceAgent(
                501L,
                101L,
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                "gateway-agent-1",
                ".aquila/workspaces/101",
                WorkspaceAgentStatus.READY,
                null,
                "google-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0),
                List.of());
    }

    private static SavedTask savedTask(Long taskId) {
        return new SavedTask(
                taskId,
                101L,
                "google-1",
                "채팅 요청",
                "기능을 구현해 주세요.",
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 5, 16, 10, 1),
                LocalDateTime.of(2026, 5, 16, 10, 1));
    }

    private static SavedChatSession savedSession(Long chatSessionId) {
        return new SavedChatSession(
                chatSessionId,
                101L,
                "google-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
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
            throw new UnsupportedOperationException("not used in chat service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadWorkspaceAgentPort implements LoadWorkspaceAgentPort {
        private final Optional<SavedWorkspaceAgent> agent;

        private FakeLoadWorkspaceAgentPort(Optional<SavedWorkspaceAgent> agent) {
            this.agent = agent;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in chat service test");
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            return agent;
        }
    }

    private static class FakeSaveChatSessionPort implements SaveChatSessionPort {
        private final SavedChatSession result;
        private ChatSession savedSession;

        private FakeSaveChatSessionPort(SavedChatSession result) {
            this.result = result;
        }

        @Override
        public SavedChatSession save(ChatSession session) {
            this.savedSession = session;
            return result;
        }
    }

    private static class FakeLoadChatSessionPort implements LoadChatSessionPort {
        private final Optional<SavedChatSession> session;

        private FakeLoadChatSessionPort(Optional<SavedChatSession> session) {
            this.session = session;
        }

        @Override
        public Optional<SavedChatSession> findByWorkspaceIdAndChatSessionId(Long workspaceId, Long chatSessionId) {
            return session;
        }
    }

    private static class FakeSaveTaskPort implements SaveTaskPort {
        private final SavedTask result;
        private Task savedTask;

        private FakeSaveTaskPort(SavedTask result) {
            this.result = result;
        }

        @Override
        public SavedTask save(Task task) {
            this.savedTask = task;
            return result;
        }
    }

    private static class FakeSaveChatMessagePort implements SaveChatMessagePort {
        private final List<ChatMessage> savedMessages = new ArrayList<>();

        @Override
        public SavedChatMessage save(ChatMessage message) {
            savedMessages.add(message);
            return new SavedChatMessage(
                    1000L + savedMessages.size(),
                    message.getWorkspaceId(),
                    message.getChatSessionId(),
                    message.getTaskId(),
                    message.getTaskExecutionId(),
                    message.getRole(),
                    message.getContent(),
                    LocalDateTime.of(2026, 5, 16, 10, savedMessages.size()),
                    LocalDateTime.of(2026, 5, 16, 10, savedMessages.size()));
        }
    }

    private static class FakeRunTaskUseCase implements RunTaskUseCase {
        private final RunTaskResult result;
        private RunTaskCommand command;

        private FakeRunTaskUseCase(RunTaskResult result) {
            this.result = result;
        }

        @Override
        public RunTaskResult runTask(RunTaskCommand command) {
            this.command = command;
            return result;
        }
    }
}
