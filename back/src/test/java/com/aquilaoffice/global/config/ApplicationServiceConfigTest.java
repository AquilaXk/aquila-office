package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.ChangeWorkspaceMemberRoleService;
import com.aquilaoffice.application.service.CreateGithubCredentialService;
import com.aquilaoffice.application.service.CreateTaskService;
import com.aquilaoffice.application.service.CreateWorkspaceAgentService;
import com.aquilaoffice.application.service.CreateWorkspaceGatewayBindingService;
import com.aquilaoffice.application.service.DeleteWorkspaceAgentService;
import com.aquilaoffice.application.service.DeleteWorkspaceService;
import com.aquilaoffice.application.service.DeleteGithubCredentialService;
import com.aquilaoffice.application.service.GetWorkspaceAgentService;
import com.aquilaoffice.application.service.GetWorkspaceGatewayStatusService;
import com.aquilaoffice.application.service.GetTaskDetailService;
import com.aquilaoffice.application.service.ListWorkspaceAgentsService;
import com.aquilaoffice.application.service.ListGithubCredentialsService;
import com.aquilaoffice.application.service.ListTasksService;
import com.aquilaoffice.application.service.ListWorkspaceInvitesService;
import com.aquilaoffice.application.service.ListWorkspaceMembersService;
import com.aquilaoffice.application.service.RemoveWorkspaceMemberService;
import com.aquilaoffice.application.service.RevokeWorkspaceInviteService;
import com.aquilaoffice.application.service.TestWorkspaceGatewayConnectionService;
import com.aquilaoffice.application.service.UpdateGithubCredentialService;
import com.aquilaoffice.application.service.UpdateTaskStatusService;
import com.aquilaoffice.application.service.UpdateWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * application service 설정이 Spring 런타임에서 필요한 use case bean을 조립하는지 검증한다.
 */
@SpringBootTest(classes = ApplicationServiceConfig.class)
class ApplicationServiceConfigTest {

    @MockitoBean
    private LoadMemberPort loadMemberPort;

    @MockitoBean
    private LoadWorkspacePort loadWorkspacePort;

    @MockitoBean
    private LoadWorkspaceMemberPort loadWorkspaceMemberPort;

    @MockitoBean
    private SaveWorkspacePort saveWorkspacePort;

    @MockitoBean
    private SaveWorkspaceInvitePort saveWorkspaceInvitePort;

    @MockitoBean
    private LoadWorkspaceInvitePort loadWorkspaceInvitePort;

    @MockitoBean
    private AcceptWorkspaceInvitePort acceptWorkspaceInvitePort;

    @MockitoBean
    private LoadWorkspaceInvitesPort loadWorkspaceInvitesPort;

    @MockitoBean
    private RevokeWorkspaceInvitePort revokeWorkspaceInvitePort;

    @MockitoBean
    private UpdateWorkspacePort updateWorkspacePort;

    @MockitoBean
    private DeleteWorkspacePort deleteWorkspacePort;

    @MockitoBean
    private ChangeWorkspaceMemberRolePort changeWorkspaceMemberRolePort;

    @MockitoBean
    private RemoveWorkspaceMemberPort removeWorkspaceMemberPort;

    @MockitoBean
    private GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort;

    @MockitoBean
    private ExistsGithubCredentialPort existsGithubCredentialPort;

    @MockitoBean
    private SaveGithubCredentialPort saveGithubCredentialPort;

    @MockitoBean
    private LoadGithubCredentialsPort loadGithubCredentialsPort;

    @MockitoBean
    private LoadGithubCredentialPort loadGithubCredentialPort;

    @MockitoBean
    private UpdateGithubCredentialPort updateGithubCredentialPort;

    @MockitoBean
    private DeleteGithubCredentialPort deleteGithubCredentialPort;

    @MockitoBean
    private LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;

    @MockitoBean
    private SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort;

    @MockitoBean
    private SaveWorkspaceAgentPort saveWorkspaceAgentPort;

    @MockitoBean
    private LoadWorkspaceAgentPort loadWorkspaceAgentPort;

    @MockitoBean
    private DeleteWorkspaceAgentPort deleteWorkspaceAgentPort;

    @MockitoBean
    private SaveTaskPort saveTaskPort;

    @MockitoBean
    private LoadTasksPort loadTasksPort;

    @MockitoBean
    private LoadTaskPort loadTaskPort;

    @MockitoBean
    private UpdateTaskStatusPort updateTaskStatusPort;

    @Autowired
    private AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase;

    @Autowired
    private ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;

    @Autowired
    private ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;

    @Autowired
    private RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;

    @Autowired
    private UpdateWorkspaceUseCase updateWorkspaceUseCase;

    @Autowired
    private DeleteWorkspaceUseCase deleteWorkspaceUseCase;

    @Autowired
    private ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase;

    @Autowired
    private RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;

    @Autowired
    private CreateGithubCredentialUseCase createGithubCredentialUseCase;

    @Autowired
    private ListGithubCredentialsUseCase listGithubCredentialsUseCase;

    @Autowired
    private UpdateGithubCredentialUseCase updateGithubCredentialUseCase;

    @Autowired
    private DeleteGithubCredentialUseCase deleteGithubCredentialUseCase;

    @Autowired
    private GetWorkspaceGatewayStatusUseCase getWorkspaceGatewayStatusUseCase;

    @Autowired
    private TestWorkspaceGatewayConnectionUseCase testWorkspaceGatewayConnectionUseCase;

    @Autowired
    private CreateWorkspaceGatewayBindingUseCase createWorkspaceGatewayBindingUseCase;

    @Autowired
    private CreateWorkspaceAgentUseCase createWorkspaceAgentUseCase;

    @Autowired
    private ListWorkspaceAgentsUseCase listWorkspaceAgentsUseCase;

    @Autowired
    private GetWorkspaceAgentUseCase getWorkspaceAgentUseCase;

    @Autowired
    private DeleteWorkspaceAgentUseCase deleteWorkspaceAgentUseCase;

    @Autowired
    private CreateTaskUseCase createTaskUseCase;

    @Autowired
    private ListTasksUseCase listTasksUseCase;

    @Autowired
    private GetTaskDetailUseCase getTaskDetailUseCase;

    @Autowired
    private UpdateTaskStatusUseCase updateTaskStatusUseCase;

    @Test
    void registersAcceptWorkspaceInviteUseCase() {
        assertThat(acceptWorkspaceInviteUseCase).isInstanceOf(AcceptWorkspaceInviteService.class);
    }

    @Test
    void registersListWorkspaceMembersUseCase() {
        assertThat(listWorkspaceMembersUseCase).isInstanceOf(ListWorkspaceMembersService.class);
    }

    @Test
    void registersListWorkspaceInvitesUseCase() {
        assertThat(listWorkspaceInvitesUseCase).isInstanceOf(ListWorkspaceInvitesService.class);
    }

    @Test
    void registersRevokeWorkspaceInviteUseCase() {
        assertThat(revokeWorkspaceInviteUseCase).isInstanceOf(RevokeWorkspaceInviteService.class);
    }

    @Test
    void registersUpdateWorkspaceUseCase() {
        assertThat(updateWorkspaceUseCase).isInstanceOf(UpdateWorkspaceService.class);
    }

    @Test
    void registersDeleteWorkspaceUseCase() {
        assertThat(deleteWorkspaceUseCase).isInstanceOf(DeleteWorkspaceService.class);
    }

    @Test
    void registersChangeWorkspaceMemberRoleUseCase() {
        assertThat(changeWorkspaceMemberRoleUseCase).isInstanceOf(ChangeWorkspaceMemberRoleService.class);
    }

    @Test
    void registersRemoveWorkspaceMemberUseCase() {
        assertThat(removeWorkspaceMemberUseCase).isInstanceOf(RemoveWorkspaceMemberService.class);
    }

    @Test
    void registersCreateGithubCredentialUseCase() {
        assertThat(createGithubCredentialUseCase).isInstanceOf(CreateGithubCredentialService.class);
    }

    @Test
    void registersListGithubCredentialsUseCase() {
        assertThat(listGithubCredentialsUseCase).isInstanceOf(ListGithubCredentialsService.class);
    }

    @Test
    void registersUpdateGithubCredentialUseCase() {
        assertThat(updateGithubCredentialUseCase).isInstanceOf(UpdateGithubCredentialService.class);
    }

    @Test
    void registersDeleteGithubCredentialUseCase() {
        assertThat(deleteGithubCredentialUseCase).isInstanceOf(DeleteGithubCredentialService.class);
    }

    @Test
    void registersGetWorkspaceGatewayStatusUseCase() {
        assertThat(getWorkspaceGatewayStatusUseCase).isInstanceOf(GetWorkspaceGatewayStatusService.class);
    }

    @Test
    void registersTestWorkspaceGatewayConnectionUseCase() {
        assertThat(testWorkspaceGatewayConnectionUseCase).isInstanceOf(TestWorkspaceGatewayConnectionService.class);
    }

    @Test
    void registersCreateWorkspaceGatewayBindingUseCase() {
        assertThat(createWorkspaceGatewayBindingUseCase).isInstanceOf(CreateWorkspaceGatewayBindingService.class);
    }

    @Test
    void registersCreateWorkspaceAgentUseCase() {
        assertThat(createWorkspaceAgentUseCase).isInstanceOf(CreateWorkspaceAgentService.class);
    }

    @Test
    void registersListWorkspaceAgentsUseCase() {
        assertThat(listWorkspaceAgentsUseCase).isInstanceOf(ListWorkspaceAgentsService.class);
    }

    @Test
    void registersGetWorkspaceAgentUseCase() {
        assertThat(getWorkspaceAgentUseCase).isInstanceOf(GetWorkspaceAgentService.class);
    }

    @Test
    void registersDeleteWorkspaceAgentUseCase() {
        assertThat(deleteWorkspaceAgentUseCase).isInstanceOf(DeleteWorkspaceAgentService.class);
    }

    @Test
    void registersCreateTaskUseCase() {
        assertThat(createTaskUseCase).isInstanceOf(CreateTaskService.class);
    }

    @Test
    void registersListTasksUseCase() {
        assertThat(listTasksUseCase).isInstanceOf(ListTasksService.class);
    }

    @Test
    void registersGetTaskDetailUseCase() {
        assertThat(getTaskDetailUseCase).isInstanceOf(GetTaskDetailService.class);
    }

    @Test
    void registersUpdateTaskStatusUseCase() {
        assertThat(updateTaskStatusUseCase).isInstanceOf(UpdateTaskStatusService.class);
    }
}
