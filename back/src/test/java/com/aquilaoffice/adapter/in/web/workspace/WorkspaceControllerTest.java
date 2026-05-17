package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleCommand;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceCommand;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryUseCase;
import com.aquilaoffice.application.port.in.LeaveWorkspaceCommand;
import com.aquilaoffice.application.port.in.LeaveWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberCommand;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentLogResult;
import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentReportResult;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.exception.ServiceException;
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
 * 워크스페이스 web adapter가 HTTP 요청을 command로 변환하고 공통 응답 계약을 지키는지 검증한다.
 */
@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWorkspaceUseCase createWorkspaceUseCase;

    @MockitoBean
    private CreateWorkspaceInviteUseCase createWorkspaceInviteUseCase;

    @MockitoBean
    private ListWorkspacesUseCase listWorkspacesUseCase;

    @MockitoBean
    private GetWorkspaceDetailUseCase getWorkspaceDetailUseCase;

    @MockitoBean
    private GetWorkspaceDashboardSummaryUseCase getWorkspaceDashboardSummaryUseCase;

    @MockitoBean
    private ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;

    @MockitoBean
    private ListWorkspaceMemberTaskStatsUseCase listWorkspaceMemberTaskStatsUseCase;

    @MockitoBean
    private ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;

    @MockitoBean
    private RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;

    @MockitoBean
    private ExtendWorkspaceInviteUseCase extendWorkspaceInviteUseCase;

    @MockitoBean
    private UpdateWorkspaceUseCase updateWorkspaceUseCase;

    @MockitoBean
    private DeleteWorkspaceUseCase deleteWorkspaceUseCase;

    @MockitoBean
    private ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase;

    @MockitoBean
    private RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;

    @MockitoBean
    private LeaveWorkspaceUseCase leaveWorkspaceUseCase;

    @Test
    void createWorkspaceReturnsCreatedEnvelope() throws Exception {
        given(createWorkspaceUseCase.createWorkspace(any(CreateWorkspaceCommand.class)))
                .willReturn(new CreateWorkspaceResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)));

        mockMvc.perform(post("/api/v1/workspaces")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Team \",\"description\":\" Description \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.name").value("Team"))
                .andExpect(jsonPath("$.data.description").value("Description"))
                .andExpect(jsonPath("$.data.creatorGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data.myRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.joinedAt", notNullValue()));

        ArgumentCaptor<CreateWorkspaceCommand> commandCaptor = ArgumentCaptor.forClass(CreateWorkspaceCommand.class);
        verify(createWorkspaceUseCase).createWorkspace(commandCaptor.capture());
        CreateWorkspaceCommand command = commandCaptor.getValue();
        // web adapter는 creator 식별자만 정규화하고, name/description 정규화는 domain 계층에 남긴다.
        assertThat(command.name()).isEqualTo(" Team ");
        assertThat(command.description()).isEqualTo(" Description ");
        assertThat(command.creatorGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void createWorkspaceRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"Description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void createWorkspaceRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Team\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void createWorkspaceReturnsNotFoundEnvelopeWhenCreatorMemberIsMissing() throws Exception {
        given(createWorkspaceUseCase.createWorkspace(any(CreateWorkspaceCommand.class)))
                .willThrow(new IllegalArgumentException("creator member not found"));

        mockMvc.perform(post("/api/v1/workspaces")
                        .principal(authenticatedPrincipal("missing-google"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Team\",\"description\":\"Description\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void createWorkspaceInviteReturnsCreatedEnvelope() throws Exception {
        given(createWorkspaceInviteUseCase.createWorkspaceInvite(any(CreateWorkspaceInviteCommand.class)))
                .willReturn(new CreateWorkspaceInviteResult(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 22, 10, 0)));

        mockMvc.perform(post("/api/v1/workspaces/101/invites")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invitee@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/101/invites/501"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.inviteId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.data.token").value("invite-token"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.expiresAt", notNullValue()));

        ArgumentCaptor<CreateWorkspaceInviteCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateWorkspaceInviteCommand.class);
        verify(createWorkspaceInviteUseCase).createWorkspaceInvite(commandCaptor.capture());
        CreateWorkspaceInviteCommand command = commandCaptor.getValue();
        assertThat(command.workspaceId()).isEqualTo(101L);
        assertThat(command.requesterGoogleSub()).isEqualTo("google-1");
        assertThat(command.email()).isEqualTo("invitee@example.com");
    }

    @Test
    void createWorkspaceInviteRejectsBlankEmail() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/invites")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceInviteUseCase);
    }

    @Test
    void createWorkspaceInviteRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/101/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invitee@example.com\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(createWorkspaceInviteUseCase);
    }

    @Test
    void createWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        given(createWorkspaceInviteUseCase.createWorkspaceInvite(any(CreateWorkspaceInviteCommand.class)))
                .willThrow(new IllegalArgumentException("workspace admin membership required"));

        mockMvc.perform(post("/api/v1/workspaces/101/invites")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invitee@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void listWorkspacesReturnsOkEnvelope() throws Exception {
        given(listWorkspacesUseCase.listWorkspaces(any(ListWorkspacesQuery.class)))
                .willReturn(List.of(new ListWorkspacesResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0))));

        mockMvc.perform(get("/api/v1/workspaces")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].workspaceId").value(101))
                .andExpect(jsonPath("$.data[0].name").value("Team"))
                .andExpect(jsonPath("$.data[0].description").value("Description"))
                .andExpect(jsonPath("$.data[0].creatorGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data[0].myRole").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].joinedAt", notNullValue()));

        ArgumentCaptor<ListWorkspacesQuery> queryCaptor = ArgumentCaptor.forClass(ListWorkspacesQuery.class);
        verify(listWorkspacesUseCase).listWorkspaces(queryCaptor.capture());
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void listWorkspacesRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(listWorkspacesUseCase);
    }

    @Test
    void getWorkspaceDetailReturnsOkEnvelope() throws Exception {
        given(getWorkspaceDetailUseCase.getWorkspaceDetail(any(GetWorkspaceDetailQuery.class)))
                .willReturn(new GetWorkspaceDetailResult(
                        101L,
                        "Team",
                        "Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)));

        mockMvc.perform(get("/api/v1/workspaces/101")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.name").value("Team"))
                .andExpect(jsonPath("$.data.description").value("Description"))
                .andExpect(jsonPath("$.data.creatorGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data.myRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.joinedAt", notNullValue()));

        ArgumentCaptor<GetWorkspaceDetailQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWorkspaceDetailQuery.class);
        verify(getWorkspaceDetailUseCase).getWorkspaceDetail(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getWorkspaceDetailRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(getWorkspaceDetailUseCase);
    }

    @Test
    void getWorkspaceDetailReturnsNotFoundEnvelope() throws Exception {
        given(getWorkspaceDetailUseCase.getWorkspaceDetail(any(GetWorkspaceDetailQuery.class)))
                .willThrow(new ServiceException(CommonErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/workspaces/999")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void getWorkspaceDashboardSummaryReturnsOkEnvelope() throws Exception {
        given(getWorkspaceDashboardSummaryUseCase.getSummary(any(GetWorkspaceDashboardSummaryQuery.class)))
                .willReturn(new GetWorkspaceDashboardSummaryResult(
                        4L,
                        1L,
                        2L,
                        1L,
                        10L,
                        3L,
                        6L,
                        1L,
                        List.of(new WorkspaceDashboardRecentReportResult(
                                900L,
                                301L,
                                501L,
                                TaskExecutionStatus.SUCCEEDED,
                                "작업 완료",
                                null,
                                LocalDateTime.of(2026, 5, 16, 12, 0))),
                        List.<WorkspaceDashboardRecentLogResult>of()));

        mockMvc.perform(get("/api/v1/workspaces/101/dashboard/summary")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.agentCount").value(4))
                .andExpect(jsonPath("$.data.runningAgentCount").value(1))
                .andExpect(jsonPath("$.data.idleAgentCount").value(2))
                .andExpect(jsonPath("$.data.errorAgentCount").value(1))
                .andExpect(jsonPath("$.data.taskCount").value(10))
                .andExpect(jsonPath("$.data.runningTaskCount").value(3))
                .andExpect(jsonPath("$.data.completedTaskCount").value(6))
                .andExpect(jsonPath("$.data.failedTaskCount").value(1))
                .andExpect(jsonPath("$.data.recentReports[0].executionId").value(900))
                .andExpect(jsonPath("$.data.recentReports[0].taskId").value(301))
                .andExpect(jsonPath("$.data.recentReports[0].agentId").value(501))
                .andExpect(jsonPath("$.data.recentReports[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.recentReports[0].summary").value("작업 완료"))
                .andExpect(jsonPath("$.data.recentReports[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.recentLogs.length()").value(0));

        ArgumentCaptor<GetWorkspaceDashboardSummaryQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWorkspaceDashboardSummaryQuery.class);
        verify(getWorkspaceDashboardSummaryUseCase).getSummary(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void getWorkspaceDashboardSummaryRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(getWorkspaceDashboardSummaryUseCase);
    }

    @Test
    void getWorkspaceDashboardSummaryReturnsNotFoundEnvelope() throws Exception {
        given(getWorkspaceDashboardSummaryUseCase.getSummary(any(GetWorkspaceDashboardSummaryQuery.class)))
                .willThrow(new IllegalArgumentException("workspace membership required"));

        mockMvc.perform(get("/api/v1/workspaces/999/dashboard/summary")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void updateWorkspaceReturnsOkEnvelope() throws Exception {
        given(updateWorkspaceUseCase.updateWorkspace(any(UpdateWorkspaceCommand.class)))
                .willReturn(new UpdateWorkspaceResult(
                        101L,
                        "Updated Team",
                        "Updated Description",
                        "google-1",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/workspaces/101")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Updated Team \",\"description\":\" Updated Description \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.name").value("Updated Team"))
                .andExpect(jsonPath("$.data.description").value("Updated Description"))
                .andExpect(jsonPath("$.data.myRole").value("ADMIN"));

        ArgumentCaptor<UpdateWorkspaceCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateWorkspaceCommand.class);
        verify(updateWorkspaceUseCase).updateWorkspace(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().name()).isEqualTo("Updated Team");
        assertThat(commandCaptor.getValue().description()).isEqualTo(" Updated Description ");
    }

    @Test
    void updateWorkspaceRejectsBlankName() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/workspaces/101")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"Description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(updateWorkspaceUseCase);
    }

    @Test
    void updateWorkspaceRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/workspaces/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Team\",\"description\":\"Description\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(updateWorkspaceUseCase);
    }

    @Test
    void deleteWorkspaceReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<DeleteWorkspaceCommand> commandCaptor =
                ArgumentCaptor.forClass(DeleteWorkspaceCommand.class);
        verify(deleteWorkspaceUseCase).deleteWorkspace(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void deleteWorkspaceRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(deleteWorkspaceUseCase);
    }

    @Test
    void listWorkspaceMembersReturnsOkEnvelope() throws Exception {
        given(listWorkspaceMembersUseCase.listWorkspaceMembers(any(ListWorkspaceMembersQuery.class)))
                .willReturn(List.of(
                        new ListWorkspaceMembersResult(
                                1L,
                                "google-1",
                                "admin@example.com",
                                "Admin",
                                WorkspaceMemberRole.ADMIN,
                                LocalDateTime.of(2026, 5, 15, 10, 0)),
                        new ListWorkspaceMembersResult(
                                2L,
                                "google-2",
                                "member@example.com",
                                "Member",
                                WorkspaceMemberRole.MEMBER,
                                LocalDateTime.of(2026, 5, 15, 11, 0))));

        mockMvc.perform(get("/api/v1/workspaces/101/members")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].memberId").value(1))
                .andExpect(jsonPath("$.data[0].memberGoogleSub").value("google-1"))
                .andExpect(jsonPath("$.data[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$.data[0].name").value("Admin"))
                .andExpect(jsonPath("$.data[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].joinedAt", notNullValue()))
                .andExpect(jsonPath("$.data[1].memberId").value(2))
                .andExpect(jsonPath("$.data[1].memberGoogleSub").value("google-2"))
                .andExpect(jsonPath("$.data[1].role").value("MEMBER"));

        ArgumentCaptor<ListWorkspaceMembersQuery> queryCaptor =
                ArgumentCaptor.forClass(ListWorkspaceMembersQuery.class);
        verify(listWorkspaceMembersUseCase).listWorkspaceMembers(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void listWorkspaceMembersRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(listWorkspaceMembersUseCase);
    }

    @Test
    void listWorkspaceMembersReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceMembersUseCase.listWorkspaceMembers(any(ListWorkspaceMembersQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/members")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void listWorkspaceMemberTaskStatsReturnsOkEnvelope() throws Exception {
        given(listWorkspaceMemberTaskStatsUseCase.listMemberTaskStats(
                        any(ListWorkspaceMemberTaskStatsQuery.class)))
                .willReturn(List.of(new ListWorkspaceMemberTaskStatsResult(
                        2L,
                        "Member",
                        1,
                        4,
                        2,
                        1,
                        0,
                        1,
                        87,
                        36,
                        50,
                        60)));

        mockMvc.perform(get("/api/v1/workspaces/101/members/task-stats")
                        .principal(authenticatedPrincipal("google-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].memberId").value(2))
                .andExpect(jsonPath("$.data[0].memberName").value("Member"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].taskCount").value(4))
                .andExpect(jsonPath("$.data[0].completedTaskCount").value(2))
                .andExpect(jsonPath("$.data[0].runningTaskCount").value(1))
                .andExpect(jsonPath("$.data[0].failedTaskCount").value(0))
                .andExpect(jsonPath("$.data[0].waitingUserTaskCount").value(1))
                .andExpect(jsonPath("$.data[0].healthScore").value(87))
                .andExpect(jsonPath("$.data[0].flowScore").value(36))
                .andExpect(jsonPath("$.data[0].impactScore").value(50))
                .andExpect(jsonPath("$.data[0].totalScore").value(60));

        ArgumentCaptor<ListWorkspaceMemberTaskStatsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListWorkspaceMemberTaskStatsQuery.class);
        verify(listWorkspaceMemberTaskStatsUseCase).listMemberTaskStats(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().requesterGoogleSub()).isEqualTo("google-2");
    }

    @Test
    void listWorkspaceMemberTaskStatsRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/members/task-stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(listWorkspaceMemberTaskStatsUseCase);
    }

    @Test
    void listWorkspaceMemberTaskStatsReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceMemberTaskStatsUseCase.listMemberTaskStats(
                        any(ListWorkspaceMemberTaskStatsQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/members/task-stats")
                        .principal(authenticatedPrincipal("google-2")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void changeWorkspaceMemberRoleReturnsOkEnvelope() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/workspaces/101/members/2")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<ChangeWorkspaceMemberRoleCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeWorkspaceMemberRoleCommand.class);
        verify(changeWorkspaceMemberRoleUseCase).changeWorkspaceMemberRole(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().memberId()).isEqualTo(2L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().role()).isEqualTo(WorkspaceMemberRole.ADMIN);
    }

    @Test
    void changeWorkspaceMemberRoleReturnsBadRequestForLastAdminRule() throws Exception {
        org.mockito.BDDMockito.willThrow(new IllegalStateException("last admin cannot be changed"))
                .given(changeWorkspaceMemberRoleUseCase)
                .changeWorkspaceMemberRole(any(ChangeWorkspaceMemberRoleCommand.class));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/workspaces/101/members/1")
                        .principal(authenticatedPrincipal("google-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void changeWorkspaceMemberRoleRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/workspaces/101/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(changeWorkspaceMemberRoleUseCase);
    }

    @Test
    void removeWorkspaceMemberReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/members/2")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<RemoveWorkspaceMemberCommand> commandCaptor =
                ArgumentCaptor.forClass(RemoveWorkspaceMemberCommand.class);
        verify(removeWorkspaceMemberUseCase).removeWorkspaceMember(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().memberId()).isEqualTo(2L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void removeWorkspaceMemberRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/members/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(removeWorkspaceMemberUseCase);
    }

    @Test
    void leaveWorkspaceReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/members/me")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-2 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<LeaveWorkspaceCommand> commandCaptor =
                ArgumentCaptor.forClass(LeaveWorkspaceCommand.class);
        verify(leaveWorkspaceUseCase).leaveWorkspace(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-2");
    }

    @Test
    void leaveWorkspaceRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/members/me")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(leaveWorkspaceUseCase);
    }

    @Test
    void leaveWorkspaceReturnsBadRequestForLastAdminRule() throws Exception {
        org.mockito.BDDMockito.willThrow(new IllegalStateException("last admin cannot leave workspace"))
                .given(leaveWorkspaceUseCase)
                .leaveWorkspace(any(LeaveWorkspaceCommand.class));

        mockMvc.perform(delete("/api/v1/workspaces/101/members/me")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void leaveWorkspaceReturnsNotFoundEnvelope() throws Exception {
        org.mockito.BDDMockito.willThrow(new IllegalArgumentException("workspace member not found"))
                .given(leaveWorkspaceUseCase)
                .leaveWorkspace(any(LeaveWorkspaceCommand.class));

        mockMvc.perform(delete("/api/v1/workspaces/999/members/me")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void listWorkspaceInvitesReturnsOkEnvelope() throws Exception {
        given(listWorkspaceInvitesUseCase.listWorkspaceInvites(any(ListWorkspaceInvitesQuery.class)))
                .willReturn(List.of(new ListWorkspaceInvitesResult(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 22, 10, 0),
                        null,
                        null)));

        mockMvc.perform(get("/api/v1/workspaces/101/invites")
                        .queryParam("status", " PENDING ")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].inviteId").value(501))
                .andExpect(jsonPath("$.data[0].workspaceId").value(101))
                .andExpect(jsonPath("$.data[0].email").value("invitee@example.com"))
                .andExpect(jsonPath("$.data[0].token").value("invite-token"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.data[0].expiresAt", notNullValue()));

        ArgumentCaptor<ListWorkspaceInvitesQuery> queryCaptor =
                ArgumentCaptor.forClass(ListWorkspaceInvitesQuery.class);
        verify(listWorkspaceInvitesUseCase).listWorkspaceInvites(queryCaptor.capture());
        assertThat(queryCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
        assertThat(queryCaptor.getValue().status()).isEqualTo(WorkspaceInviteStatus.PENDING);
    }

    @Test
    void listWorkspaceInvitesRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/invites")
                        .queryParam("status", "unknown")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(listWorkspaceInvitesUseCase);
    }

    @Test
    void listWorkspaceInvitesRejectsMissingPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/invites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(listWorkspaceInvitesUseCase);
    }

    @Test
    void listWorkspaceInvitesReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceInvitesUseCase.listWorkspaceInvites(any(ListWorkspaceInvitesQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/invites")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void revokeWorkspaceInviteReturnsOkEnvelope() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/101/invites/501")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"));

        ArgumentCaptor<RevokeWorkspaceInviteCommand> commandCaptor =
                ArgumentCaptor.forClass(RevokeWorkspaceInviteCommand.class);
        verify(revokeWorkspaceInviteUseCase).revokeWorkspaceInvite(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().inviteId()).isEqualTo(501L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void revokeWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        org.mockito.BDDMockito.willThrow(new IllegalArgumentException("workspace invite entity not found"))
                .given(revokeWorkspaceInviteUseCase)
                .revokeWorkspaceInvite(any(RevokeWorkspaceInviteCommand.class));

        mockMvc.perform(delete("/api/v1/workspaces/101/invites/404")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void extendWorkspaceInviteReturnsOkEnvelope() throws Exception {
        given(extendWorkspaceInviteUseCase.extendWorkspaceInvite(any(ExtendWorkspaceInviteCommand.class)))
                .willReturn(new ExtendWorkspaceInviteResult(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 25, 10, 0),
                        null,
                        null));

        mockMvc.perform(patch("/api/v1/workspaces/101/invites/501/extend")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalDays\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.inviteId").value(501))
                .andExpect(jsonPath("$.data.workspaceId").value(101))
                .andExpect(jsonPath("$.data.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.data.token").value("invite-token"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.expiresAt", notNullValue()));

        ArgumentCaptor<ExtendWorkspaceInviteCommand> commandCaptor =
                ArgumentCaptor.forClass(ExtendWorkspaceInviteCommand.class);
        verify(extendWorkspaceInviteUseCase).extendWorkspaceInvite(commandCaptor.capture());
        assertThat(commandCaptor.getValue().workspaceId()).isEqualTo(101L);
        assertThat(commandCaptor.getValue().inviteId()).isEqualTo(501L);
        assertThat(commandCaptor.getValue().requesterGoogleSub()).isEqualTo("google-1");
        assertThat(commandCaptor.getValue().additionalDays()).isEqualTo(3);
    }

    @Test
    void extendWorkspaceInviteAcceptsEmptyBodyAsDefaultDays() throws Exception {
        given(extendWorkspaceInviteUseCase.extendWorkspaceInvite(any(ExtendWorkspaceInviteCommand.class)))
                .willReturn(new ExtendWorkspaceInviteResult(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 29, 10, 0),
                        null,
                        null));

        mockMvc.perform(patch("/api/v1/workspaces/101/invites/501/extend")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<ExtendWorkspaceInviteCommand> commandCaptor =
                ArgumentCaptor.forClass(ExtendWorkspaceInviteCommand.class);
        verify(extendWorkspaceInviteUseCase).extendWorkspaceInvite(commandCaptor.capture());
        assertThat(commandCaptor.getValue().additionalDays()).isNull();
    }

    @Test
    void extendWorkspaceInviteRejectsInvalidAdditionalDays() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/101/invites/501/extend")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalDays\":31}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(extendWorkspaceInviteUseCase);
    }

    @Test
    void extendWorkspaceInviteReturnsBadRequestWhenInviteCannotBeExtended() throws Exception {
        given(extendWorkspaceInviteUseCase.extendWorkspaceInvite(any(ExtendWorkspaceInviteCommand.class)))
                .willThrow(new IllegalStateException("invite is not extendable"));

        mockMvc.perform(patch("/api/v1/workspaces/101/invites/501/extend")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalDays\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void extendWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        given(extendWorkspaceInviteUseCase.extendWorkspaceInvite(any(ExtendWorkspaceInviteCommand.class)))
                .willThrow(new IllegalArgumentException("workspace invite entity not found"));

        mockMvc.perform(patch("/api/v1/workspaces/101/invites/404/extend")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalDays\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
