package com.aquilaoffice.adapter.in.web.task;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusCommand;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Test
    void createTaskReturnsCreatedEnvelope() throws Exception {
        given(createTaskUseCase.createTask(any(CreateTaskCommand.class))).willReturn(createResult());

        mockMvc.perform(post("/api/v1/workspaces/101/tasks")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
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
    void missingHeaderReturnsBadRequestAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createTaskUseCase, listTasksUseCase, getTaskDetailUseCase, updateTaskStatusUseCase);
    }

    @Test
    void blankTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/tasks")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
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
}
