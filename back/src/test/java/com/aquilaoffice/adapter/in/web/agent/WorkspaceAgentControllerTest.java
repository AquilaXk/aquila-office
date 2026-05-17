package com.aquilaoffice.adapter.in.web.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
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
 * Agent web adapter가 HTTP 요청을 application command/query로 변환하는지 검증한다.
 */
@WebMvcTest(WorkspaceAgentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWorkspaceAgentUseCase createWorkspaceAgentUseCase;

    @MockitoBean
    private ListWorkspaceAgentsUseCase listWorkspaceAgentsUseCase;

    @MockitoBean
    private GetWorkspaceAgentUseCase getWorkspaceAgentUseCase;

    @MockitoBean
    private DeleteWorkspaceAgentUseCase deleteWorkspaceAgentUseCase;

    @Test
    void createWorkspaceAgentReturnsCreatedEnvelope() throws Exception {
        given(createWorkspaceAgentUseCase.createWorkspaceAgent(any(CreateWorkspaceAgentCommand.class)))
                .willReturn(createResult());

        mockMvc.perform(post("/api/v1/workspaces/101/agents")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Backend Agent ",
                                  "category": "BACKEND",
                                  "workspacePath": " ",
                                  "skillFiles": [
                                    { "fileName": " AGENTS.md ", "content": "skill" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101/agents/501"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.agentId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.name").value("Backend Agent"))
                .andExpect(jsonPath("$.data.category").value("BACKEND"))
                .andExpect(jsonPath("$.data.openClawAgentId").value("gateway-agent-1"))
                .andExpect(jsonPath("$.data.workspacePath").value(".aquila/workspaces/101"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.skillFiles[0].fileName").value("AGENTS.md"))
                .andExpect(jsonPath("$.data.skillFiles[0].syncStatus").value("SYNCED"));

        ArgumentCaptor<CreateWorkspaceAgentCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateWorkspaceAgentCommand.class);
        verify(createWorkspaceAgentUseCase).createWorkspaceAgent(commandCaptor.capture());
        CreateWorkspaceAgentCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.name()).isEqualTo("Backend Agent");
        assertThat(command.category()).isEqualTo(WorkspaceAgentCategory.BACKEND);
        assertThat(command.workspacePath()).isEmpty();
        assertThat(command.skillFiles()).singleElement().satisfies(skillFile -> {
            assertThat(skillFile.fileName()).isEqualTo("AGENTS.md");
            assertThat(skillFile.content()).isEqualTo("skill");
        });
    }

    @Test
    void listWorkspaceAgentsReturnsOkEnvelope() throws Exception {
        given(listWorkspaceAgentsUseCase.listWorkspaceAgents(any(ListWorkspaceAgentsQuery.class)))
                .willReturn(List.of(listResult()));

        mockMvc.perform(get("/api/v1/workspaces/101/agents")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].agentId").value(501))
                .andExpect(jsonPath("$.data[0].name").value("Backend Agent"))
                .andExpect(jsonPath("$.data[0].skillFiles[0].syncStatus").value("SYNCED"));

        ArgumentCaptor<ListWorkspaceAgentsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListWorkspaceAgentsQuery.class);
        verify(listWorkspaceAgentsUseCase).listWorkspaceAgents(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getWorkspaceAgentReturnsOkEnvelope() throws Exception {
        given(getWorkspaceAgentUseCase.getWorkspaceAgent(any(GetWorkspaceAgentQuery.class)))
                .willReturn(getResult());

        mockMvc.perform(get("/api/v1/workspaces/101/agents/501")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agentId").value(501))
                .andExpect(jsonPath("$.data.category").value("BACKEND"));

        ArgumentCaptor<GetWorkspaceAgentQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWorkspaceAgentQuery.class);
        verify(getWorkspaceAgentUseCase).getWorkspaceAgent(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().agentId()).isEqualTo(501L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void deleteWorkspaceAgentReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/agents/501")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<DeleteWorkspaceAgentCommand> commandCaptor =
                ArgumentCaptor.forClass(DeleteWorkspaceAgentCommand.class);
        verify(deleteWorkspaceAgentUseCase).deleteWorkspaceAgent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().agentId()).isEqualTo(501L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/agents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(
                createWorkspaceAgentUseCase,
                listWorkspaceAgentsUseCase,
                getWorkspaceAgentUseCase,
                deleteWorkspaceAgentUseCase);
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/agents")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceAgentUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceAgentsUseCase.listWorkspaceAgents(any(ListWorkspaceAgentsQuery.class)))
                .willThrow(new IllegalArgumentException("workspace membership not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/agents")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }

    private static CreateWorkspaceAgentResult createResult() {
        return new CreateWorkspaceAgentResult(
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
                skillFiles());
    }

    private static ListWorkspaceAgentsResult listResult() {
        return new ListWorkspaceAgentsResult(
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
                skillFiles());
    }

    private static GetWorkspaceAgentResult getResult() {
        return new GetWorkspaceAgentResult(
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
                skillFiles());
    }

    private static List<WorkspaceAgentSkillFileResult> skillFiles() {
        return List.of(new WorkspaceAgentSkillFileResult(
                601L,
                "AGENTS.md",
                WorkspaceAgentSkillFileSyncStatus.SYNCED,
                null));
    }
}
