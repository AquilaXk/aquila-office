package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
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

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.exception.ServiceException;
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
    private ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;

    @MockitoBean
    private ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;

    @MockitoBean
    private RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;

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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"Description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void createWorkspaceRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Team\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceUseCase);
    }

    @Test
    void createWorkspaceReturnsNotFoundEnvelopeWhenCreatorMemberIsMissing() throws Exception {
        given(createWorkspaceUseCase.createWorkspace(any(CreateWorkspaceCommand.class)))
                .willThrow(new IllegalArgumentException("creator member not found"));

        mockMvc.perform(post("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "missing-google")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(createWorkspaceInviteUseCase);
    }

    @Test
    void createWorkspaceInviteReturnsNotFoundEnvelope() throws Exception {
        given(createWorkspaceInviteUseCase.createWorkspaceInvite(any(CreateWorkspaceInviteCommand.class)))
                .willThrow(new IllegalArgumentException("workspace admin membership required"));

        mockMvc.perform(post("/api/v1/workspaces/101/invites")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
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
    void listWorkspacesRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
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
    void getWorkspaceDetailRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(getWorkspaceDetailUseCase);
    }

    @Test
    void getWorkspaceDetailReturnsNotFoundEnvelope() throws Exception {
        given(getWorkspaceDetailUseCase.getWorkspaceDetail(any(GetWorkspaceDetailQuery.class)))
                .willThrow(new ServiceException(CommonErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/workspaces/999")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
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
    void listWorkspaceMembersRejectsBlankCreatorHeader() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/101/members")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(listWorkspaceMembersUseCase);
    }

    @Test
    void listWorkspaceMembersReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceMembersUseCase.listWorkspaceMembers(any(ListWorkspaceMembersQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/members")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 "))
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
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(listWorkspaceInvitesUseCase);
    }

    @Test
    void listWorkspaceInvitesReturnsNotFoundEnvelope() throws Exception {
        given(listWorkspaceInvitesUseCase.listWorkspaceInvites(any(ListWorkspaceInvitesQuery.class)))
                .willThrow(new IllegalArgumentException("workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/999/invites")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1"))
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
}
