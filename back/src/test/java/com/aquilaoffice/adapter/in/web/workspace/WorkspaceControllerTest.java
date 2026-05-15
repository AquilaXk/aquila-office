package com.aquilaoffice.adapter.in.web.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import java.time.LocalDateTime;
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

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateWorkspaceUseCase createWorkspaceUseCase;

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
}
