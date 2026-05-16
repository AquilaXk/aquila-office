package com.aquilaoffice.adapter.in.web.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.ChatMessageResult;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesResult;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendChatMessageUseCase sendChatMessageUseCase;

    @MockitoBean
    private ListChatTaskMessagesUseCase listChatTaskMessagesUseCase;

    @MockitoBean
    private ListChatSessionMessagesUseCase listChatSessionMessagesUseCase;

    @Test
    void sendChatMessageReturnsOkEnvelope() throws Exception {
        given(sendChatMessageUseCase.sendChatMessage(any(SendChatMessageCommand.class)))
                .willReturn(sendResult());

        mockMvc.perform(post("/api/v1/workspaces/101/chat/messages")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": " 기능을 구현해 주세요. ",
                                  "agentId": 501,
                                  "repositoryId": 801,
                                  "taskType": "FEATURE_IMPLEMENTATION",
                                  "priority": "HIGH",
                                  "title": " 기능 구현 ",
                                  "createPr": true,
                                  "chatSessionId": 901
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatSessionId").value(901))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.assignedAgentId").value(501))
                .andExpect(jsonPath("$.data.executionStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"));

        ArgumentCaptor<SendChatMessageCommand> commandCaptor =
                ArgumentCaptor.forClass(SendChatMessageCommand.class);
        verify(sendChatMessageUseCase).sendChatMessage(commandCaptor.capture());
        SendChatMessageCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.message()).isEqualTo(" 기능을 구현해 주세요. ");
        assertThat(command.agentId()).isEqualTo(501L);
        assertThat(command.repositoryId()).isEqualTo(801L);
        assertThat(command.taskType()).isEqualTo(TaskType.FEATURE_IMPLEMENTATION);
        assertThat(command.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(command.title()).isEqualTo(" 기능 구현 ");
        assertThat(command.createPr()).isTrue();
        assertThat(command.chatSessionId()).isEqualTo(901L);
    }

    @Test
    void listTaskMessagesReturnsOkEnvelope() throws Exception {
        given(listChatTaskMessagesUseCase.listChatTaskMessages(any(ListChatTaskMessagesQuery.class)))
                .willReturn(List.of(userMessage()));

        mockMvc.perform(get("/api/v1/workspaces/101/chat/tasks/301/messages")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageId").value(1001))
                .andExpect(jsonPath("$.data[0].role").value("USER"));

        ArgumentCaptor<ListChatTaskMessagesQuery> queryCaptor =
                ArgumentCaptor.forClass(ListChatTaskMessagesQuery.class);
        verify(listChatTaskMessagesUseCase).listChatTaskMessages(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void listSessionMessagesReturnsOkEnvelope() throws Exception {
        given(listChatSessionMessagesUseCase.listChatSessionMessages(any(ListChatSessionMessagesQuery.class)))
                .willReturn(new ListChatSessionMessagesResult(
                        901L,
                        List.of(userMessage(), assistantMessage()),
                        1002L,
                        true));

        mockMvc.perform(get("/api/v1/workspaces/101/chat/sessions/901/messages")
                        .queryParam("afterMessageId", "1000")
                        .queryParam("limit", "2")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatSessionId").value(901))
                .andExpect(jsonPath("$.data.nextCursor").value(1002))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"));

        ArgumentCaptor<ListChatSessionMessagesQuery> queryCaptor =
                ArgumentCaptor.forClass(ListChatSessionMessagesQuery.class);
        verify(listChatSessionMessagesUseCase).listChatSessionMessages(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().chatSessionId()).isEqualTo(901L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(queryCaptor.getValue().afterMessageId()).isEqualTo(1000L);
        assertThat(queryCaptor.getValue().limit()).isEqualTo(2);
    }

    @Test
    void missingHeaderReturnsBadRequestAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/chat/tasks/301/messages"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(sendChatMessageUseCase, listChatTaskMessagesUseCase, listChatSessionMessagesUseCase);
    }

    @Test
    void blankMessageReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/chat/messages")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \",\"agentId\":501}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(sendChatMessageUseCase);
    }

    private static SendChatMessageResult sendResult() {
        return new SendChatMessageResult(
                901L,
                301L,
                101L,
                501L,
                TaskStatus.COMPLETED,
                701L,
                TaskExecutionStatus.SUCCEEDED,
                null,
                "구현 완료",
                null,
                LocalDateTime.of(2026, 5, 16, 10, 1),
                List.of(userMessage(), assistantMessage()));
    }

    private static ChatMessageResult userMessage() {
        return new ChatMessageResult(
                1001L,
                901L,
                301L,
                null,
                ChatMessageRole.USER,
                "기능을 구현해 주세요.",
                LocalDateTime.of(2026, 5, 16, 10, 1));
    }

    private static ChatMessageResult assistantMessage() {
        return new ChatMessageResult(
                1002L,
                901L,
                301L,
                701L,
                ChatMessageRole.ASSISTANT,
                "구현 완료",
                LocalDateTime.of(2026, 5, 16, 10, 2));
    }
}
