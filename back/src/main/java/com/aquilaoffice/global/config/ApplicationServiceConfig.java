package com.aquilaoffice.global.config;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.in.GetArtifactTreeUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.SaveChatMessagePort;
import com.aquilaoffice.application.port.out.SaveChatSessionPort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SendSlackReplyPort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.ChangeWorkspaceMemberRoleService;
import com.aquilaoffice.application.service.CreateGithubCredentialService;
import com.aquilaoffice.application.service.CreateSlackChannelMappingService;
import com.aquilaoffice.application.service.CreateTaskService;
import com.aquilaoffice.application.service.CreateWorkspaceAgentService;
import com.aquilaoffice.application.service.CreateWorkspaceGatewayBindingService;
import com.aquilaoffice.application.service.CreateWorkspaceService;
import com.aquilaoffice.application.service.CreateWorkspaceInviteService;
import com.aquilaoffice.application.service.DeleteGithubCredentialService;
import com.aquilaoffice.application.service.DeleteSlackIntegrationService;
import com.aquilaoffice.application.service.DeleteWorkspaceAgentService;
import com.aquilaoffice.application.service.DeleteWorkspaceService;
import com.aquilaoffice.application.service.GetArtifactFileService;
import com.aquilaoffice.application.service.GetArtifactTreeService;
import com.aquilaoffice.application.service.GetOrchestrationPlanArtifactService;
import com.aquilaoffice.application.service.GetWorkspaceAgentService;
import com.aquilaoffice.application.service.GetWorkspaceGatewayStatusService;
import com.aquilaoffice.application.service.GetWorkspaceDetailService;
import com.aquilaoffice.application.service.GetWorkspaceInviteService;
import com.aquilaoffice.application.service.GetTaskDetailService;
import com.aquilaoffice.application.service.ListTaskReportsService;
import com.aquilaoffice.application.service.ListGithubCredentialsService;
import com.aquilaoffice.application.service.ListChatSessionMessagesService;
import com.aquilaoffice.application.service.ListChatTaskMessagesService;
import com.aquilaoffice.application.service.ListSlackChannelMappingsService;
import com.aquilaoffice.application.service.ListTasksService;
import com.aquilaoffice.application.service.ListWorkspaceAgentsService;
import com.aquilaoffice.application.service.ListWorkspaceMembersService;
import com.aquilaoffice.application.service.ListWorkspaceInvitesService;
import com.aquilaoffice.application.service.ListWorkspacesService;
import com.aquilaoffice.application.service.ReceiveSlackEventService;
import com.aquilaoffice.application.service.RemoveWorkspaceMemberService;
import com.aquilaoffice.application.service.RevokeWorkspaceInviteService;
import com.aquilaoffice.application.service.RunTaskService;
import com.aquilaoffice.application.service.SendChatMessageService;
import com.aquilaoffice.application.service.SlackAgentCommandParser;
import com.aquilaoffice.application.service.TestWorkspaceGatewayConnectionService;
import com.aquilaoffice.application.service.UpdateGithubCredentialService;
import com.aquilaoffice.application.service.UpdateSlackIntegrationService;
import com.aquilaoffice.application.service.UpdateTaskStatusService;
import com.aquilaoffice.application.service.UpdateWorkspaceService;
import com.aquilaoffice.adapter.in.web.slack.SlackSignatureVerifier;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring 밖에 둔 application service를 런타임 bean으로 조립하는 설정이다.
 */
@Configuration
public class ApplicationServiceConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }

    // application service는 Spring 애노테이션 없이 유지하고, 조립만 설정 클래스에서 담당한다.
    @Bean
    public CreateWorkspaceUseCase createWorkspaceUseCase(
            LoadMemberPort loadMemberPort,
            SaveWorkspacePort saveWorkspacePort,
            Clock applicationClock) {
        return new CreateWorkspaceService(loadMemberPort, saveWorkspacePort, applicationClock);
    }

    @Bean
    public CreateWorkspaceInviteUseCase createWorkspaceInviteUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceInvitePort saveWorkspaceInvitePort,
            GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort,
            Clock applicationClock) {
        return new CreateWorkspaceInviteService(
                loadMemberPort,
                loadWorkspacePort,
                saveWorkspaceInvitePort,
                generateWorkspaceInviteTokenPort,
                applicationClock);
    }

    @Bean
    public ListWorkspacesUseCase listWorkspacesUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort) {
        return new ListWorkspacesService(loadMemberPort, loadWorkspacePort);
    }

    @Bean
    public GetWorkspaceDetailUseCase getWorkspaceDetailUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort) {
        return new GetWorkspaceDetailService(loadMemberPort, loadWorkspacePort);
    }

    @Bean
    public ListWorkspaceMembersUseCase listWorkspaceMembersUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort) {
        return new ListWorkspaceMembersService(loadMemberPort, loadWorkspacePort, loadWorkspaceMemberPort);
    }

    @Bean
    public ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceInvitesPort loadWorkspaceInvitesPort) {
        return new ListWorkspaceInvitesService(loadMemberPort, loadWorkspacePort, loadWorkspaceInvitesPort);
    }

    @Bean
    public GetWorkspaceInviteUseCase getWorkspaceInviteUseCase(LoadWorkspaceInvitePort loadWorkspaceInvitePort) {
        return new GetWorkspaceInviteService(loadWorkspaceInvitePort);
    }

    @Bean
    public AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspaceInvitePort loadWorkspaceInvitePort,
            AcceptWorkspaceInvitePort acceptWorkspaceInvitePort,
            Clock applicationClock) {
        return new AcceptWorkspaceInviteService(
                loadMemberPort, loadWorkspaceInvitePort, acceptWorkspaceInvitePort, applicationClock);
    }

    @Bean
    public RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            RevokeWorkspaceInvitePort revokeWorkspaceInvitePort,
            Clock applicationClock) {
        return new RevokeWorkspaceInviteService(
                loadMemberPort, loadWorkspacePort, revokeWorkspaceInvitePort, applicationClock);
    }

    @Bean
    public UpdateWorkspaceUseCase updateWorkspaceUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            UpdateWorkspacePort updateWorkspacePort) {
        return new UpdateWorkspaceService(loadMemberPort, loadWorkspacePort, updateWorkspacePort);
    }

    @Bean
    public DeleteWorkspaceUseCase deleteWorkspaceUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            DeleteWorkspacePort deleteWorkspacePort,
            Clock applicationClock) {
        return new DeleteWorkspaceService(loadMemberPort, loadWorkspacePort, deleteWorkspacePort, applicationClock);
    }

    @Bean
    public ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            ChangeWorkspaceMemberRolePort changeWorkspaceMemberRolePort) {
        return new ChangeWorkspaceMemberRoleService(
                loadMemberPort, loadWorkspacePort, loadWorkspaceMemberPort, changeWorkspaceMemberRolePort);
    }

    @Bean
    public RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            RemoveWorkspaceMemberPort removeWorkspaceMemberPort) {
        return new RemoveWorkspaceMemberService(
                loadMemberPort, loadWorkspacePort, loadWorkspaceMemberPort, removeWorkspaceMemberPort);
    }

    @Bean
    public CreateGithubCredentialUseCase createGithubCredentialUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            ExistsGithubCredentialPort existsGithubCredentialPort,
            SaveGithubCredentialPort saveGithubCredentialPort) {
        return new CreateGithubCredentialService(
                loadMemberPort, loadWorkspacePort, existsGithubCredentialPort, saveGithubCredentialPort);
    }

    @Bean
    public ListGithubCredentialsUseCase listGithubCredentialsUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialsPort loadGithubCredentialsPort) {
        return new ListGithubCredentialsService(loadMemberPort, loadWorkspacePort, loadGithubCredentialsPort);
    }

    @Bean
    public UpdateGithubCredentialUseCase updateGithubCredentialUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialPort loadGithubCredentialPort,
            UpdateGithubCredentialPort updateGithubCredentialPort) {
        return new UpdateGithubCredentialService(
                loadMemberPort, loadWorkspacePort, loadGithubCredentialPort, updateGithubCredentialPort);
    }

    @Bean
    public DeleteGithubCredentialUseCase deleteGithubCredentialUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadGithubCredentialPort loadGithubCredentialPort,
            DeleteGithubCredentialPort deleteGithubCredentialPort) {
        return new DeleteGithubCredentialService(
                loadMemberPort, loadWorkspacePort, loadGithubCredentialPort, deleteGithubCredentialPort);
    }

    @Bean
    public GetWorkspaceGatewayStatusUseCase getWorkspaceGatewayStatusUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort) {
        return new GetWorkspaceGatewayStatusService(
                loadMemberPort, loadWorkspacePort, loadWorkspaceGatewayBindingPort);
    }

    @Bean
    public TestWorkspaceGatewayConnectionUseCase testWorkspaceGatewayConnectionUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort) {
        return new TestWorkspaceGatewayConnectionService(loadMemberPort, loadWorkspacePort);
    }

    @Bean
    public CreateWorkspaceGatewayBindingUseCase createWorkspaceGatewayBindingUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort) {
        return new CreateWorkspaceGatewayBindingService(
                loadMemberPort, loadWorkspacePort, saveWorkspaceGatewayBindingPort);
    }

    @Bean
    public CreateWorkspaceAgentUseCase createWorkspaceAgentUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceAgentPort saveWorkspaceAgentPort) {
        return new CreateWorkspaceAgentService(loadMemberPort, loadWorkspacePort, saveWorkspaceAgentPort);
    }

    @Bean
    public ListWorkspaceAgentsUseCase listWorkspaceAgentsUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort) {
        return new ListWorkspaceAgentsService(loadMemberPort, loadWorkspacePort, loadWorkspaceAgentPort);
    }

    @Bean
    public GetWorkspaceAgentUseCase getWorkspaceAgentUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort) {
        return new GetWorkspaceAgentService(loadMemberPort, loadWorkspacePort, loadWorkspaceAgentPort);
    }

    @Bean
    public DeleteWorkspaceAgentUseCase deleteWorkspaceAgentUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            DeleteWorkspaceAgentPort deleteWorkspaceAgentPort) {
        return new DeleteWorkspaceAgentService(loadMemberPort, loadWorkspacePort, deleteWorkspaceAgentPort);
    }

    @Bean
    public CreateTaskUseCase createTaskUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            SaveTaskPort saveTaskPort) {
        return new CreateTaskService(loadMemberPort, loadWorkspacePort, loadWorkspaceAgentPort, saveTaskPort);
    }

    @Bean
    public ListTasksUseCase listTasksUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTasksPort loadTasksPort) {
        return new ListTasksService(loadMemberPort, loadWorkspacePort, loadTasksPort);
    }

    @Bean
    public GetTaskDetailUseCase getTaskDetailUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort) {
        return new GetTaskDetailService(loadMemberPort, loadWorkspacePort, loadTaskPort);
    }

    @Bean
    public UpdateTaskStatusUseCase updateTaskStatusUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            UpdateTaskStatusPort updateTaskStatusPort) {
        return new UpdateTaskStatusService(
                loadMemberPort, loadWorkspacePort, loadTaskPort, updateTaskStatusPort);
    }

    @Bean
    public RunTaskUseCase runTaskUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort) {
        return new RunTaskService(
                loadMemberPort,
                loadWorkspacePort,
                loadTaskPort,
                loadWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                saveTaskExecutionPort,
                updateTaskExecutionPort,
                updateTaskStatusPort,
                runWorkspaceAgentPort);
    }

    @Bean
    public ListTaskReportsUseCase listTaskReportsUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadTaskExecutionReportsPort loadTaskExecutionReportsPort) {
        return new ListTaskReportsService(
                loadMemberPort, loadWorkspacePort, loadTaskPort, loadTaskExecutionReportsPort);
    }

    @Bean
    public GetArtifactTreeUseCase getArtifactTreeUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort) {
        return new GetArtifactTreeService(loadMemberPort, loadWorkspacePort, loadWorkspaceArtifactsPort);
    }

    @Bean
    public GetArtifactFileUseCase getArtifactFileUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort) {
        return new GetArtifactFileService(loadMemberPort, loadWorkspacePort, loadWorkspaceArtifactsPort);
    }

    @Bean
    public GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadOrchestrationPlanArtifactPort loadOrchestrationPlanArtifactPort) {
        return new GetOrchestrationPlanArtifactService(
                loadMemberPort,
                loadWorkspacePort,
                loadOrchestrationPlanArtifactPort);
    }

    @Bean
    public SendChatMessageUseCase sendChatMessageUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            SaveChatSessionPort saveChatSessionPort,
            LoadChatSessionPort loadChatSessionPort,
            SaveTaskPort saveTaskPort,
            SaveChatMessagePort saveChatMessagePort,
            RunTaskUseCase runTaskUseCase) {
        return new SendChatMessageService(
                loadMemberPort,
                loadWorkspacePort,
                loadWorkspaceAgentPort,
                saveChatSessionPort,
                loadChatSessionPort,
                saveTaskPort,
                saveChatMessagePort,
                runTaskUseCase);
    }

    @Bean
    public ListChatTaskMessagesUseCase listChatTaskMessagesUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadChatMessagesPort loadChatMessagesPort) {
        return new ListChatTaskMessagesService(loadMemberPort, loadWorkspacePort, loadChatMessagesPort);
    }

    @Bean
    public ListChatSessionMessagesUseCase listChatSessionMessagesUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadChatMessagesPort loadChatMessagesPort,
            LoadChatSessionPort loadChatSessionPort) {
        return new ListChatSessionMessagesService(
                loadMemberPort,
                loadWorkspacePort,
                loadChatMessagesPort,
                loadChatSessionPort);
    }

    @Bean
    public SlackAgentCommandParser slackAgentCommandParser() {
        return new SlackAgentCommandParser();
    }

    @Bean
    public ReceiveSlackEventUseCase receiveSlackEventUseCase(
            SlackAgentCommandParser slackAgentCommandParser,
            SendChatMessageUseCase sendChatMessageUseCase,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SendSlackReplyPort sendSlackReplyPort) {
        return new ReceiveSlackEventService(
                slackAgentCommandParser,
                sendChatMessageUseCase,
                loadSlackChannelMappingPort,
                sendSlackReplyPort);
    }

    @Bean
    public CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort) {
        return new CreateSlackChannelMappingService(
                loadMemberPort,
                loadWorkspacePort,
                loadSlackChannelMappingPort,
                saveSlackChannelMappingPort);
    }

    @Bean
    public ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort) {
        return new ListSlackChannelMappingsService(
                loadMemberPort,
                loadWorkspacePort,
                loadSlackChannelMappingPort);
    }

    @Bean
    public UpdateSlackIntegrationUseCase updateSlackIntegrationUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort) {
        return new UpdateSlackIntegrationService(
                loadMemberPort,
                loadWorkspacePort,
                loadSlackChannelMappingPort,
                saveSlackChannelMappingPort);
    }

    @Bean
    public DeleteSlackIntegrationUseCase deleteSlackIntegrationUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            SaveSlackChannelMappingPort saveSlackChannelMappingPort) {
        return new DeleteSlackIntegrationService(
                loadMemberPort,
                loadWorkspacePort,
                loadSlackChannelMappingPort,
                saveSlackChannelMappingPort);
    }

    @Bean
    public SlackSignatureVerifier slackSignatureVerifier(
            @Value("${aquila.slack.signing-secret:}") String signingSecret,
            Clock applicationClock) {
        return new SlackSignatureVerifier(signingSecret, applicationClock, Duration.ofMinutes(5));
    }
}
