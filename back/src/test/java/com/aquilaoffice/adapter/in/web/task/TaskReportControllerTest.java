package com.aquilaoffice.adapter.in.web.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.ListTaskReportsQuery;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.application.port.in.TaskReportArtifactResult;
import com.aquilaoffice.application.port.in.TaskReportResult;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.TaskStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TaskReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListTaskReportsUseCase listTaskReportsUseCase;

    @Test
    void listTaskReportsReturnsOkEnvelope() throws Exception {
        given(listTaskReportsUseCase.listTaskReports(any(ListTaskReportsQuery.class)))
                .willReturn(List.of(reportResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/tasks/301/reports")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reportId").value(901))
                .andExpect(jsonPath("$.data[0].taskId").value(301))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].summary").value("테스트 생성 완료"))
                .andExpect(jsonPath("$.data[0].artifacts[0].artifactId").value(801))
                .andExpect(jsonPath("$.data[0].artifacts[0].artifactType").value("FILE"));

        ArgumentCaptor<ListTaskReportsQuery> queryCaptor = ArgumentCaptor.forClass(ListTaskReportsQuery.class);
        verify(listTaskReportsUseCase).listTaskReports(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().taskId()).isEqualTo(301L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/tasks/301/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(listTaskReportsUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(listTaskReportsUseCase.listTaskReports(any(ListTaskReportsQuery.class)))
                .willThrow(new IllegalArgumentException("task not found"));

        mockMvc.perform(get("/api/v1/workspaces/101/tasks/999/reports")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static TaskReportResult reportResult() {
        return new TaskReportResult(
                901L,
                301L,
                TaskStatus.COMPLETED,
                "테스트 생성 완료",
                "테스트 생성 완료",
                null,
                List.of(new TaskReportArtifactResult(
                        801L,
                        "FILE",
                        "result.md",
                        "/api/v1/workspaces/101/artifacts/files?path=result.md")),
                LocalDateTime.of(2026, 5, 16, 12, 0));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
