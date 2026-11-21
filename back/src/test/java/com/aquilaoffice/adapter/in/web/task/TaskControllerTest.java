package com.aquilaoffice.adapter.in.web.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CancelTaskCommand;
import com.aquilaoffice.application.port.in.CancelTaskUseCase;
import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsQuery;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsUseCase;
import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.RecoverStaleTasksCommand;
import com.aquilaoffice.application.port.in.RecoverStaleTasksResult;
import com.aquilaoffice.application.port.in.RecoverStaleTasksUseCase;
import com.aquilaoffice.application.port.in.RequestTaskApprovalCommand;
import com.aquilaoffice.application.port.in.RequestTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalCommand;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusCommand;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.security.AuthenticatedMember;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Task web adapter가 HTTP 요청을 application command/query로 변환하는지 검증한다.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTaskUseCase createTaskUseCase;

    @MockitoBean
    private ListTasksUseCase listTasksUseCase;

    @MockitoBean
    private GetTaskDetailUseCase getTaskDetailUseCase;

    @MockitoBean
    private UpdateTaskStatusUseCase updateTaskStatusUseCase;

    @MockitoBean
    private RunTaskUseCase runTaskUseCase;

    @MockitoBean
    private ListTaskExecutionLogsUseCase listTaskExecutionLogsUseCase;

    @MockitoBean
    private ListChatTaskMessagesUseCase listChatTaskMessagesUseCase;

    @MockitoBean
    private CancelTaskUseCase cancelTaskUseCase;

    @MockitoBean
    private RequestTaskApprovalUseCase requestTaskApprovalUseCase;

    @MockitoBean
    private ReviewTaskApprovalUseCase reviewTaskApprovalUseCase;

    @MockitoBean
    private RecoverStaleTasksUseCase recoverStaleTasksUseCase;

    @Test
    void createTaskReturnsCreatedEnvelope() throws Exception {
        given(createTaskUseCase.createTask(any(CreateTaskCommand.class))).willReturn(createResult());

        mockMvc.perform(post("/api/v1/workspaces/101/tasks")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " 테스트 작성 ",
                                  "description": " 핵심 서비스 테스트를 추가해주세요. ",
                                  "type": "TEST_CREATION",
                                  "priority": "HIGH",
                                  "assignedAgentId": 501
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101/tasks/301"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.title").value("테스트 작성"))
                .andExpect(jsonPath("$.data.type").value("TEST_CREATION"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(501))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        ArgumentCaptor<CreateTaskCommand> commandCaptor = ArgumentCaptor.forClass(CreateTaskCommand.class);
        verify(createTaskUseCase).createTask(commandCaptor.capture());
        CreateTaskCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.title()).isEqualTo("테스트 작성");
        assertThat(command.description()).isEqualTo("핵심 서비스 테스트를 추가해주세요.");
        assertThat(command.type()).isEqualTo(TaskType.TEST_CREATION);
        assertThat(command.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(command.assignedAgentId()).isEqualTo(501L);
    }

    @Test
    void listTasksReturnsOkEnvelope() throws Exception {
        given(listTasksUseCase.listTasks(any(ListTasksQuery.class))).willReturn(List.of(listResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/tasks")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskId").value(301))
                .andExpect(jsonPath("$.data[0].status").value("ASSIGNED"));

        ArgumentCaptor<ListTasksQuery> queryCaptor = ArgumentCaptor.forClass(ListTasksQuery.class);
        verify(listTasksUseCase).listTasks(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getTaskDetailReturnsOkEnvelope() throws Exception {
        given(getTaskDetailUseCase.getTaskDetail(any(GetTaskDetailQuery.class))).willReturn(detailResult());

        mockMvc.perform(get("/api/v1/workspaces/101/tasks/301")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.description").value("핵심 서비스 테스트를 추가해주세요."));

        ArgumentCaptor<GetTaskDetailQuery> queryCaptor = ArgumentCaptor.forClass(GetTaskDetailQuery.class);
        verify(getTaskDetailUseCase).getTaskDetail(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void updateTaskStatusReturnsOkEnvelope() throws Exception {
        given(updateTaskStatusUseCase.updateTaskStatus(any(UpdateTaskStatusCommand.class)))
                .willReturn(updateResult());

        mockMvc.perform(patch("/api/v1/workspaces/101/tasks/301/status")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        ArgumentCaptor<UpdateTaskStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateTaskStatusCommand.class);
        verify(updateTaskStatusUseCase).updateTaskStatus(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void runTaskReturnsOkEnvelope() throws Exception {
        given(runTaskUseCase.runTask(any(RunTaskCommand.class))).willReturn(runResult());

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/run")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"createPr\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.executionId").value(701))
                .andExpect(jsonPath("$.data.agentId").value(501))
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.executionStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.finalText").value("완료 요약"));

        ArgumentCaptor<RunTaskCommand> commandCaptor = ArgumentCaptor.forClass(RunTaskCommand.class);
        verify(runTaskUseCase).runTask(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().createPr()).isTrue();
    }

    @Test
    void createAndRunTaskReturnsOkEnvelope() throws Exception {
        given(createTaskUseCase.createTask(any(CreateTaskCommand.class))).willReturn(createResult());
        given(runTaskUseCase.runTask(any(RunTaskCommand.class))).willReturn(runResult());

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/run")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " 작업 실행 ",
                                  "description": " 생성 직후 실행해 주세요. ",
                                  "type": "FEATURE_IMPLEMENTATION",
                                  "priority": "URGENT",
                                  "assignedAgentId": 501,
                                  "createPr": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.taskId").value(301))
                .andExpect(jsonPath("$.data.executionId").value(701))
                .andExpect(jsonPath("$.data.agentId").value(501))
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.executionStatus").value("SUCCEEDED"));

        ArgumentCaptor<CreateTaskCommand> createCommandCaptor =
                ArgumentCaptor.forClass(CreateTaskCommand.class);
        verify(createTaskUseCase).createTask(createCommandCaptor.capture());
        CreateTaskCommand createCommand = createCommandCaptor.getValue();
        assertThat(createCommand.workspaceId()).isEqualTo(101L);
        assertThat(createCommand.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(createCommand.title()).isEqualTo("작업 실행");
        assertThat(createCommand.description()).isEqualTo("생성 직후 실행해 주세요.");
        assertThat(createCommand.type()).isEqualTo(TaskType.FEATURE_IMPLEMENTATION);
        assertThat(createCommand.priority()).isEqualTo(TaskPriority.URGENT);
        assertThat(createCommand.assignedAgentId()).isEqualTo(501L);

        ArgumentCaptor<RunTaskCommand> runCommandCaptor = ArgumentCaptor.forClass(RunTaskCommand.class);
        verify(runTaskUseCase).runTask(runCommandCaptor.capture());
        RunTaskCommand runCommand = runCommandCaptor.getValue();
        assertThat(runCommand.workspaceId()).isEqualTo(101L);
        assertThat(runCommand.taskId()).isEqualTo(301L);
        assertThat(runCommand.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(runCommand.createPr()).isTrue();
    }

    @Test
    void listTaskLogsReturnsOkEnvelope() throws Exception {
        given(listTaskExecutionLogsUseCase.listTaskExecutionLogs(any(ListTaskExecutionLogsQuery.class)))
                .willReturn(List.of(taskLogResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/tasks/301/logs")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].logId").value(801))
                .andExpect(jsonPath("$.data[0].taskExecutionId").value(701))
                .andExpect(jsonPath("$.data[0].level").value("INFO"))
                .andExpect(jsonPath("$.data[0].message").value("작업 실행 시작"));

        ArgumentCaptor<ListTaskExecutionLogsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListTaskExecutionLogsQuery.class);
        verify(listTaskExecutionLogsUseCase).listTaskExecutionLogs(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void listTaskMessagesAliasReturnsOkEnvelope() throws Exception {
        given(listChatTaskMessagesUseCase.listChatTaskMessages(any(ListChatTaskMessagesQuery.class)))
                .willReturn(List.of(taskMessageResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/tasks/301/messages")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageId").value(901))
                .andExpect(jsonPath("$.data[0].taskId").value(301))
                .andExpect(jsonPath("$.data[0].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[0].content").value("작업 결과를 정리했습니다."));

        ArgumentCaptor<ListChatTaskMessagesQuery> queryCaptor =
                ArgumentCaptor.forClass(ListChatTaskMessagesQuery.class);
        verify(listChatTaskMessagesUseCase).listChatTaskMessages(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                createTaskUseCase,
                listTasksUseCase,
                getTaskDetailUseCase,
                updateTaskStatusUseCase,
                runTaskUseCase);
    }

    @Test
    void blankTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/tasks")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createTaskUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(listTasksUseCase.listTasks(any(ListTasksQuery.class)))
                .willThrow(new IllegalArgumentException("workspace membership not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/tasks")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void taskControllerDoesNotExposeLegacyRequesterHeaderHelper() {
        assertThatThrownBy(
                        () -> TaskController.class.getDeclaredMethod("requireRequesterGoogleSub", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void cancelTaskReturnsUpdatedStatusAndMapsStateConflict() throws Exception {
        given(cancelTaskUseCase.cancelTask(any(CancelTaskCommand.class)))
                .willReturn(statusResult(TaskStatus.CANCELED));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/cancel")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        given(cancelTaskUseCase.cancelTask(any(CancelTaskCommand.class)))
                .willThrow(new IllegalStateException("terminal task status cannot be changed"));
        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/cancel")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestAndReviewApprovalValidatePayloadsAndReturnUpdatedTask() throws Exception {
        given(requestTaskApprovalUseCase.requestApproval(any(RequestTaskApprovalCommand.class)))
                .willReturn(statusResult(TaskStatus.WAITING_USER));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/request-approval")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"배포 승인 필요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_USER"));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/request-approval")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());

        given(reviewTaskApprovalUseCase.reviewApproval(any(ReviewTaskApprovalCommand.class)))
                .willReturn(statusResult(TaskStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/review-approval")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"comment\":\"승인합니다\",\"runAfterApprove\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        ArgumentCaptor<ReviewTaskApprovalCommand> reviewCaptor =
                ArgumentCaptor.forClass(ReviewTaskApprovalCommand.class);
        verify(reviewTaskApprovalUseCase).reviewApproval(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().decision()).isEqualTo("APPROVE");
        assertThat(reviewCaptor.getValue().comment()).isEqualTo("승인합니다");
        assertThat(reviewCaptor.getValue().runAfterApprove()).isTrue();

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/301/review-approval")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"INVALID\",\"comment\":\"bad\",\"runAfterApprove\":false}"))
                .andExpect(status().isBadRequest());

        verify(requestTaskApprovalUseCase).requestApproval(any(RequestTaskApprovalCommand.class));
        verify(reviewTaskApprovalUseCase).reviewApproval(any(ReviewTaskApprovalCommand.class));
    }

    @Test
    void recoverStaleTasksValidatesRangeAndHidesNonAdmin() throws Exception {
        given(recoverStaleTasksUseCase.recoverStaleTasks(any(RecoverStaleTasksCommand.class)))
                .willReturn(new RecoverStaleTasksResult(2, 3));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/recover-stale")
                        .principal(authenticatedPrincipal("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staleMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recoveredTaskCount").value(2))
                .andExpect(jsonPath("$.data.recoveredExecutionCount").value(3));

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/recover-stale")
                        .principal(authenticatedPrincipal("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staleMinutes\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/workspaces/101/tasks/recover-stale")
                        .principal(authenticatedPrincipal("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staleMinutes\":1441}"))
                .andExpect(status().isBadRequest());

        given(recoverStaleTasksUseCase.recoverStaleTasks(any(RecoverStaleTasksCommand.class)))
                .willThrow(new IllegalArgumentException("workspace admin membership required"));
        mockMvc.perform(post("/api/v1/workspaces/101/tasks/recover-stale")
                        .principal(authenticatedPrincipal("member-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static UpdateTaskStatusResult statusResult(TaskStatus status) {
        return new UpdateTaskStatusResult(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                status,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 5));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }

    private static CreateTaskResult createResult() {
        return new CreateTaskResult(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static ListTasksResult listResult() {
        return new ListTasksResult(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static GetTaskDetailResult detailResult() {
        return new GetTaskDetailResult(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static UpdateTaskStatusResult updateResult() {
        return new UpdateTaskStatusResult(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                "핵심 서비스 테스트를 추가해주세요.",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 1));
    }

    private static RunTaskResult runResult() {
        return new RunTaskResult(
                101L,
                301L,
                701L,
                501L,
                TaskStatus.COMPLETED,
                TaskExecutionStatus.SUCCEEDED,
                "완료 요약",
                null);
    }

    private static com.aquilaoffice.application.port.in.TaskExecutionLogResult taskLogResult() {
        return new com.aquilaoffice.application.port.in.TaskExecutionLogResult(
                801L,
                101L,
                301L,
                701L,
                501L,
                "INFO",
                "작업 실행 시작",
                LocalDateTime.of(2026, 5, 16, 10, 2));
    }

    private static com.aquilaoffice.application.port.in.ChatMessageResult taskMessageResult() {
        return new com.aquilaoffice.application.port.in.ChatMessageResult(
                901L,
                9010L,
                301L,
                701L,
                ChatMessageRole.ASSISTANT,
                "작업 결과를 정리했습니다.",
                LocalDateTime.of(2026, 5, 16, 10, 3));
    }
}
