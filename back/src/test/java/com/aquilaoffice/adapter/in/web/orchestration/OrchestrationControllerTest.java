package com.aquilaoffice.adapter.in.web.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsQuery;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsResult;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsUseCase;
import com.aquilaoffice.application.port.in.OrchestrationStepResult;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
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
 * Multi-Agent orchestration web adapter의 생성·목록·상세·디스패치 HTTP 계약을 검증한다.
 */
@WebMvcTest(OrchestrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrchestrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrchestrationSessionUseCase createOrchestrationSessionUseCase;

    @MockitoBean
    private ListOrchestrationSessionsUseCase listOrchestrationSessionsUseCase;

    @MockitoBean
    private GetOrchestrationSessionUseCase getOrchestrationSessionUseCase;

    @MockitoBean
    private DispatchOrchestrationSessionUseCase dispatchOrchestrationSessionUseCase;

    @Test
    void createSessionReturnsCreatedEnvelopeAndLocationHeader() throws Exception {
        given(createOrchestrationSessionUseCase.createSession(any(CreateOrchestrationSessionCommand.class)))
                .willReturn(new CreateOrchestrationSessionResult(
                        901L,
                        101L,
                        301L,
                        OrchestrationSessionStatus.PLANNED,
                        LocalDateTime.of(2026, 5, 18, 10, 0),
                        LocalDateTime.of(2026, 5, 18, 10, 0),
                        List.of(
                                new OrchestrationStepResult(
                                        "plan",
                                        "설계 정리",
                                        501L,
                                        null,
                                        List.of(),
                                        OrchestrationStepStatus.PENDING),
                                new OrchestrationStepResult(
                                        "impl",
                                        "코드 작성",
                                        502L,
                                        null,
                                        List.of("plan"),
                                        OrchestrationStepStatus.PENDING))));

        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceTaskId": 301,
                                  "steps": [
                                    { "stepKey": "plan", "instruction": "설계 정리", "agentId": 501, "dependsOn": [] },
                                    { "stepKey": "impl", "instruction": "코드 작성", "agentId": 502, "dependsOn": ["plan"] }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101/orchestrations/901"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(901))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.sourceTaskId").value(301))
                .andExpect(jsonPath("$.data.status").value("PLANNED"))
                .andExpect(jsonPath("$.data.steps[1].dependsOn[0]").value("plan"));

        ArgumentCaptor<CreateOrchestrationSessionCommand> captor =
                ArgumentCaptor.forClass(CreateOrchestrationSessionCommand.class);
        verify(createOrchestrationSessionUseCase).createSession(captor.capture());
        assertThat(captor.getValue().steps()).hasSize(2);
    }

    @Test
    void createSessionRejectsExceedingStepCountOversizedInstructionAndDependencyCycles() throws Exception {
        StringBuilder tooManySteps = new StringBuilder();
        for (int i = 1; i <= 21; i++) {
            if (i > 1) {
                tooManySteps.append(",");
            }
            tooManySteps.append("{\"stepKey\":\"s")
                    .append(i)
                    .append("\",\"instruction\":\"ok\",\"agentId\":501}");
        }
        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTaskId\":301,\"steps\":[" + tooManySteps + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceTaskId": 301,
                                  "steps": [
                                    { "stepKey": "plan", "instruction": "%s", "agentId": 501 }
                                  ]
                                }
                                """.formatted("a".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceTaskId": 301,
                                  "steps": [
                                    { "stepKey": "a\\nb", "instruction": "ok", "agentId": 501 }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTaskId\":301,\"steps\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createOrchestrationSessionUseCase);

        given(createOrchestrationSessionUseCase.createSession(any(CreateOrchestrationSessionCommand.class)))
                .willThrow(new IllegalArgumentException("orchestration step dependencies must not contain cycles"));
        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceTaskId": 301,
                                  "steps": [
                                    { "stepKey": "a", "instruction": "A", "agentId": 501, "dependsOn": ["b"] },
                                    { "stepKey": "b", "instruction": "B", "agentId": 502, "dependsOn": ["a"] }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void listGetAndDispatchOrchestrationsReturnExpectedEnvelopesAndHideNotFound() throws Exception {
        given(listOrchestrationSessionsUseCase.listSessions(any(ListOrchestrationSessionsQuery.class)))
                .willReturn(List.of(new ListOrchestrationSessionsResult(
                        901L,
                        101L,
                        301L,
                        OrchestrationSessionStatus.RUNNING,
                        LocalDateTime.of(2026, 5, 18, 10, 0),
                        LocalDateTime.of(2026, 5, 18, 10, 1),
                        List.of())));

        mockMvc.perform(get("/api/v1/workspaces/101/orchestrations")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value(901))
                .andExpect(jsonPath("$.data[0].status").value("RUNNING"));

        given(getOrchestrationSessionUseCase.getSession(any(GetOrchestrationSessionQuery.class)))
                .willReturn(new GetOrchestrationSessionResult(
                        901L,
                        101L,
                        301L,
                        OrchestrationSessionStatus.RUNNING,
                        LocalDateTime.of(2026, 5, 18, 10, 0),
                        LocalDateTime.of(2026, 5, 18, 10, 1),
                        List.of(new OrchestrationStepResult(
                                "plan",
                                "설계 정리",
                                501L,
                                401L,
                                List.of(),
                                OrchestrationStepStatus.SUCCEEDED))));

        mockMvc.perform(get("/api/v1/workspaces/101/orchestrations/901")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(901))
                .andExpect(jsonPath("$.data.steps[0].taskId").value(401));

        given(dispatchOrchestrationSessionUseCase.dispatchSession(any(DispatchOrchestrationSessionCommand.class)))
                .willReturn(new DispatchOrchestrationSessionResult(
                        901L,
                        101L,
                        301L,
                        OrchestrationSessionStatus.SUCCEEDED,
                        1,
                        List.of(new OrchestrationStepResult(
                                "plan",
                                "설계 정리",
                                501L,
                                401L,
                                List.of(),
                                OrchestrationStepStatus.SUCCEEDED))));

        mockMvc.perform(post("/api/v1/workspaces/101/orchestrations/901/dispatch")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"createPr\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.dispatchedCount").value(1));

        ArgumentCaptor<DispatchOrchestrationSessionCommand> dispatchCaptor =
                ArgumentCaptor.forClass(DispatchOrchestrationSessionCommand.class);
        verify(dispatchOrchestrationSessionUseCase).dispatchSession(dispatchCaptor.capture());
        assertThat(dispatchCaptor.getValue().createPr()).isTrue();

        given(getOrchestrationSessionUseCase.getSession(any(GetOrchestrationSessionQuery.class)))
                .willThrow(new IllegalArgumentException("orchestration session not found"));
        mockMvc.perform(get("/api/v1/workspaces/101/orchestrations/404")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));

        mockMvc.perform(get("/api/v1/workspaces/101/orchestrations"))
                .andExpect(status().isUnauthorized());
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
