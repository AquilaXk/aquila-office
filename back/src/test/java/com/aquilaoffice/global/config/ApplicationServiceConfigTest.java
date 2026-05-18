package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.in.web.slack.SlackSignatureVerifier;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.CompleteSlackOAuthCallbackUseCase;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.in.GetArtifactTreeUseCase;
import com.aquilaoffice.application.port.in.GetMemberProfileUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDashboardSummaryUseCase;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.in.GoogleLoginUseCase;
import com.aquilaoffice.application.port.in.LeaveWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsUseCase;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesUseCase;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.LogoutAuthUseCase;
import com.aquilaoffice.application.port.in.ReceiveSlackEventUseCase;
import com.aquilaoffice.application.port.in.RefreshAuthTokenUseCase;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.in.SendChatMessageUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionUseCase;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.UpdateMemberProfileUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionUseCase;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.AnalyzeChatIntentPort;
import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.ClaimSlackEventPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.DeleteRefreshTokenPort;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.ExchangeSlackOAuthCodePort;
import com.aquilaoffice.application.port.out.ExtendWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.InitializeMemberProfilePort;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadMemberProfilePort;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.SaveChatMessagePort;
import com.aquilaoffice.application.port.out.SaveChatSessionPort;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SaveOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import com.aquilaoffice.application.port.out.SaveSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SendSlackReplyPort;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateMemberProfilePort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationSessionStatusPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationStepPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockPort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.ChangeWorkspaceMemberRoleService;
import com.aquilaoffice.application.service.CompleteSlackOAuthCallbackService;
import com.aquilaoffice.application.service.CreateGithubCredentialService;
import com.aquilaoffice.application.service.CreateOrchestrationSessionService;
import com.aquilaoffice.application.service.CreateSlackChannelMappingService;
import com.aquilaoffice.application.service.CreateTaskService;
import com.aquilaoffice.application.service.CreateWorkspaceAgentService;
import com.aquilaoffice.application.service.CreateWorkspaceGatewayBindingService;
import com.aquilaoffice.application.service.DeleteWorkspaceAgentService;
import com.aquilaoffice.application.service.DeleteWorkspaceService;
import com.aquilaoffice.application.service.DeleteGithubCredentialService;
import com.aquilaoffice.application.service.DeleteSlackIntegrationService;
import com.aquilaoffice.application.service.DispatchOrchestrationSessionService;
import com.aquilaoffice.application.service.ExtendWorkspaceInviteService;
import com.aquilaoffice.application.service.GetArtifactFileService;
import com.aquilaoffice.application.service.GetArtifactTreeService;
import com.aquilaoffice.application.service.GetOrchestrationPlanArtifactService;
import com.aquilaoffice.application.service.GetOrchestrationPlanFileService;
import com.aquilaoffice.application.service.GetWorkspaceAgentService;
import com.aquilaoffice.application.service.GetWorkspaceGatewayStatusService;
import com.aquilaoffice.application.service.GetWorkspaceDashboardSummaryService;
import com.aquilaoffice.application.service.GetTaskDetailService;
import com.aquilaoffice.application.service.GoogleLoginService;
import com.aquilaoffice.application.service.LeaveWorkspaceService;
import com.aquilaoffice.application.service.MemberProfileService;
import com.aquilaoffice.application.service.OpenWorkspacePresenceStreamService;
import com.aquilaoffice.application.service.ListTaskReportsService;
import com.aquilaoffice.application.service.ListWorkspaceAgentsService;
import com.aquilaoffice.application.service.ListGithubCredentialsService;
import com.aquilaoffice.application.service.ListChatSessionMessagesService;
import com.aquilaoffice.application.service.ListChatTaskMessagesService;
import com.aquilaoffice.application.service.ListSlackChannelMappingsService;
import com.aquilaoffice.application.service.ListTasksService;
import com.aquilaoffice.application.service.ListWorkspaceInvitesService;
import com.aquilaoffice.application.service.ListWorkspaceMemberTaskStatsService;
import com.aquilaoffice.application.service.ListWorkspaceMembersService;
import com.aquilaoffice.application.service.LogoutAuthService;
import com.aquilaoffice.application.service.ReceiveSlackEventService;
import com.aquilaoffice.application.service.RefreshAuthTokenService;
import com.aquilaoffice.application.service.RemoveWorkspaceMemberService;
import com.aquilaoffice.application.service.RevokeWorkspaceInviteService;
import com.aquilaoffice.application.service.RunTaskService;
import com.aquilaoffice.application.service.SendChatMessageService;
import com.aquilaoffice.application.service.TestWorkspaceGatewayConnectionService;
import com.aquilaoffice.application.service.UpdateGithubCredentialService;
import com.aquilaoffice.application.service.UpdateSlackIntegrationService;
import com.aquilaoffice.application.service.UpdateTaskStatusService;
import com.aquilaoffice.application.service.UpdateWorkspacePresencePositionService;
import com.aquilaoffice.application.service.UpdateWorkspaceService;
import com.aquilaoffice.application.service.WorkspacePresenceRegistry;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * application service 설정이 Spring 런타임에서 필요한 use case bean을 조립하는지 검증한다.
 */
@SpringBootTest(classes = ApplicationServiceConfig.class)
class ApplicationServiceConfigTest {

    @MockitoBean
    private LoadMemberPort loadMemberPort;

    @MockitoBean
    private LoadMemberProfilePort loadMemberProfilePort;

    @MockitoBean
    private UpdateMemberProfilePort updateMemberProfilePort;

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
    private ExtendWorkspaceInvitePort extendWorkspaceInvitePort;

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
    private LoadWorkspaceDashboardSummaryPort loadWorkspaceDashboardSummaryPort;

    @MockitoBean
    private SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort;

    @MockitoBean
    private AuthenticateWorkspaceGatewayPort authenticateWorkspaceGatewayPort;

    @MockitoBean
    private LoadSlackChannelMappingPort loadSlackChannelMappingPort;

    @MockitoBean
    private SaveSlackChannelMappingPort saveSlackChannelMappingPort;

    @MockitoBean
    private SendSlackReplyPort sendSlackReplyPort;

    @MockitoBean
    private ClaimSlackEventPort claimSlackEventPort;

    @MockitoBean
    private ExchangeSlackOAuthCodePort exchangeSlackOAuthCodePort;

    @MockitoBean
    private SaveWorkspaceAgentPort saveWorkspaceAgentPort;

    @MockitoBean
    private CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort;

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

    @MockitoBean
    private SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    @MockitoBean
    private SaveTaskExecutionPort saveTaskExecutionPort;

    @MockitoBean
    private UpdateTaskExecutionPort updateTaskExecutionPort;

    @MockitoBean
    private LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;

    @MockitoBean
    private LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort;

    @MockitoBean
    private LoadOrchestrationPlanArtifactPort loadOrchestrationPlanArtifactPort;

    @MockitoBean
    private SaveOrchestrationSessionPort saveOrchestrationSessionPort;

    @MockitoBean
    private LoadOrchestrationSessionPort loadOrchestrationSessionPort;

    @MockitoBean
    private UpdateOrchestrationStepPort updateOrchestrationStepPort;

    @MockitoBean
    private UpdateOrchestrationSessionStatusPort updateOrchestrationSessionStatusPort;

    @MockitoBean
    private RunWorkspaceAgentPort runWorkspaceAgentPort;

    @MockitoBean
    private WorkspaceExecutionLockPort workspaceExecutionLockPort;

    @MockitoBean
    private SaveChatSessionPort saveChatSessionPort;

    @MockitoBean
    private LoadChatSessionPort loadChatSessionPort;

    @MockitoBean
    private SaveChatMessagePort saveChatMessagePort;

    @MockitoBean
    private AnalyzeChatIntentPort analyzeChatIntentPort;

    @MockitoBean
    private LoadChatMessagesPort loadChatMessagesPort;

    @MockitoBean
    private VerifyGoogleIdTokenPort verifyGoogleIdTokenPort;

    @MockitoBean
    private SaveGoogleLoginMemberPort saveGoogleLoginMemberPort;

    @MockitoBean
    private IssueAuthTokenPort issueAuthTokenPort;

    @MockitoBean
    private SaveRefreshTokenPort saveRefreshTokenPort;

    @MockitoBean
    private InitializeMemberProfilePort initializeMemberProfilePort;

    @MockitoBean
    private ResolveRefreshTokenPort resolveRefreshTokenPort;

    @MockitoBean
    private RotateRefreshTokenPort rotateRefreshTokenPort;

    @MockitoBean
    private DeleteRefreshTokenPort deleteRefreshTokenPort;

    @Autowired
    private Clock applicationClock;

    @Autowired
    private AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase;

    @Autowired
    private ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;

    @Autowired
    private ListWorkspaceMemberTaskStatsUseCase listWorkspaceMemberTaskStatsUseCase;

    @Autowired
    private GetMemberProfileUseCase getMemberProfileUseCase;

    @Autowired
    private UpdateMemberProfileUseCase updateMemberProfileUseCase;

    @Autowired
    private GoogleLoginUseCase googleLoginUseCase;

    @Autowired
    private RefreshAuthTokenUseCase refreshAuthTokenUseCase;

    @Autowired
    private LogoutAuthUseCase logoutAuthUseCase;

    @Autowired
    private UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase;

    @Autowired
    private OpenWorkspacePresenceStreamUseCase openWorkspacePresenceStreamUseCase;

    @Autowired
    private CloseWorkspacePresenceStreamUseCase closeWorkspacePresenceStreamUseCase;

    @Autowired
    private WorkspacePresenceRegistry workspacePresenceRegistry;

    @Autowired
    private ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;

    @Autowired
    private RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;

    @Autowired
    private ExtendWorkspaceInviteUseCase extendWorkspaceInviteUseCase;

    @Autowired
    private UpdateWorkspaceUseCase updateWorkspaceUseCase;

    @Autowired
    private DeleteWorkspaceUseCase deleteWorkspaceUseCase;

    @Autowired
    private ChangeWorkspaceMemberRoleUseCase changeWorkspaceMemberRoleUseCase;

    @Autowired
    private RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;

    @Autowired
    private LeaveWorkspaceUseCase leaveWorkspaceUseCase;

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
    private GetWorkspaceDashboardSummaryUseCase getWorkspaceDashboardSummaryUseCase;

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

    @Autowired
    private RunTaskUseCase runTaskUseCase;

    @Autowired
    private CreateOrchestrationSessionUseCase createOrchestrationSessionUseCase;

    @Autowired
    private DispatchOrchestrationSessionUseCase dispatchOrchestrationSessionUseCase;

    @Autowired
    private ListTaskReportsUseCase listTaskReportsUseCase;

    @Autowired
    private GetArtifactTreeUseCase getArtifactTreeUseCase;

    @Autowired
    private GetArtifactFileUseCase getArtifactFileUseCase;

    @Autowired
    private GetOrchestrationPlanArtifactUseCase getOrchestrationPlanArtifactUseCase;

    @Autowired
    private GetOrchestrationPlanFileUseCase getOrchestrationPlanFileUseCase;

    @Autowired
    private SendChatMessageUseCase sendChatMessageUseCase;

    @Autowired
    private ListChatTaskMessagesUseCase listChatTaskMessagesUseCase;

    @Autowired
    private ListChatSessionMessagesUseCase listChatSessionMessagesUseCase;

    @Autowired
    private ReceiveSlackEventUseCase receiveSlackEventUseCase;

    @Autowired
    private CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase;

    @Autowired
    private ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase;

    @Autowired
    private UpdateSlackIntegrationUseCase updateSlackIntegrationUseCase;

    @Autowired
    private DeleteSlackIntegrationUseCase deleteSlackIntegrationUseCase;

    @Autowired
    private CompleteSlackOAuthCallbackUseCase completeSlackOAuthCallbackUseCase;

    @Autowired
    private SlackSignatureVerifier slackSignatureVerifier;

    @Test
    void registersAcceptWorkspaceInviteUseCase() {
        assertThat(acceptWorkspaceInviteUseCase).isInstanceOf(AcceptWorkspaceInviteService.class);
    }

    @Test
    void registersListWorkspaceMembersUseCase() {
        assertThat(listWorkspaceMembersUseCase).isInstanceOf(ListWorkspaceMembersService.class);
    }

    @Test
    void registersListWorkspaceMemberTaskStatsUseCase() {
        assertThat(listWorkspaceMemberTaskStatsUseCase).isInstanceOf(ListWorkspaceMemberTaskStatsService.class);
    }

    @Test
    void registersGetMemberProfileUseCase() {
        assertThat(getMemberProfileUseCase).isInstanceOf(MemberProfileService.class);
    }

    @Test
    void registersUpdateMemberProfileUseCase() {
        assertThat(updateMemberProfileUseCase).isInstanceOf(MemberProfileService.class);
    }

    @Test
    void registersGoogleLoginUseCase() {
        assertThat(googleLoginUseCase).isInstanceOf(GoogleLoginService.class);
        assertThat(ReflectionTestUtils.getField(googleLoginUseCase, "verifyGoogleIdTokenPort"))
                .isSameAs(verifyGoogleIdTokenPort);
        assertThat(ReflectionTestUtils.getField(googleLoginUseCase, "saveGoogleLoginMemberPort"))
                .isSameAs(saveGoogleLoginMemberPort);
        assertThat(ReflectionTestUtils.getField(googleLoginUseCase, "issueAuthTokenPort"))
                .isSameAs(issueAuthTokenPort);
        assertThat(ReflectionTestUtils.getField(googleLoginUseCase, "saveRefreshTokenPort"))
                .isSameAs(saveRefreshTokenPort);
        assertThat(ReflectionTestUtils.getField(googleLoginUseCase, "initializeMemberProfilePort"))
                .isSameAs(initializeMemberProfilePort);
    }

    @Test
    void registersRefreshAuthTokenUseCase() {
        assertThat(refreshAuthTokenUseCase).isInstanceOf(RefreshAuthTokenService.class);
        assertThat(ReflectionTestUtils.getField(refreshAuthTokenUseCase, "resolveRefreshTokenPort"))
                .isSameAs(resolveRefreshTokenPort);
        assertThat(ReflectionTestUtils.getField(refreshAuthTokenUseCase, "issueAuthTokenPort"))
                .isSameAs(issueAuthTokenPort);
        assertThat(ReflectionTestUtils.getField(refreshAuthTokenUseCase, "rotateRefreshTokenPort"))
                .isSameAs(rotateRefreshTokenPort);
        assertThat(ReflectionTestUtils.getField(refreshAuthTokenUseCase, "clock"))
                .isSameAs(applicationClock);
    }

    @Test
    void registersLogoutAuthUseCase() {
        assertThat(logoutAuthUseCase).isInstanceOf(LogoutAuthService.class);
        assertThat(ReflectionTestUtils.getField(logoutAuthUseCase, "resolveRefreshTokenPort"))
                .isSameAs(resolveRefreshTokenPort);
        assertThat(ReflectionTestUtils.getField(logoutAuthUseCase, "deleteRefreshTokenPort"))
                .isSameAs(deleteRefreshTokenPort);
    }

    @Test
    void registersWorkspacePresenceRegistry() {
        assertThat(workspacePresenceRegistry).isNotNull();
    }

    @Test
    void registersUpdateWorkspacePresencePositionUseCase() {
        assertThat(updateWorkspacePresencePositionUseCase)
                .isInstanceOf(UpdateWorkspacePresencePositionService.class);
        assertThat(ReflectionTestUtils.getField(updateWorkspacePresencePositionUseCase, "workspacePresenceRegistry"))
                .isSameAs(workspacePresenceRegistry);
    }

    @Test
    void registersWorkspacePresenceStreamUseCases() {
        assertThat(openWorkspacePresenceStreamUseCase).isInstanceOf(OpenWorkspacePresenceStreamService.class);
        assertThat(closeWorkspacePresenceStreamUseCase).isSameAs(openWorkspacePresenceStreamUseCase);
        assertThat(ReflectionTestUtils.getField(openWorkspacePresenceStreamUseCase, "workspacePresenceRegistry"))
                .isSameAs(workspacePresenceRegistry);
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
    void registersExtendWorkspaceInviteUseCase() {
        assertThat(extendWorkspaceInviteUseCase).isInstanceOf(ExtendWorkspaceInviteService.class);
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
    void registersLeaveWorkspaceUseCase() {
        assertThat(leaveWorkspaceUseCase).isInstanceOf(LeaveWorkspaceService.class);
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
    void registersGetWorkspaceDashboardSummaryUseCase() {
        assertThat(getWorkspaceDashboardSummaryUseCase).isInstanceOf(GetWorkspaceDashboardSummaryService.class);
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

    @Test
    void registersRunTaskUseCase() {
        assertThat(runTaskUseCase).isInstanceOf(RunTaskService.class);
    }

    @Test
    void registersCreateOrchestrationSessionUseCase() {
        assertThat(createOrchestrationSessionUseCase).isInstanceOf(CreateOrchestrationSessionService.class);
    }

    @Test
    void registersDispatchOrchestrationSessionUseCase() {
        assertThat(dispatchOrchestrationSessionUseCase).isInstanceOf(DispatchOrchestrationSessionService.class);
    }

    @Test
    void registersListTaskReportsUseCase() {
        assertThat(listTaskReportsUseCase).isInstanceOf(ListTaskReportsService.class);
    }

    @Test
    void registersGetArtifactTreeUseCase() {
        assertThat(getArtifactTreeUseCase).isInstanceOf(GetArtifactTreeService.class);
    }

    @Test
    void registersGetArtifactFileUseCase() {
        assertThat(getArtifactFileUseCase).isInstanceOf(GetArtifactFileService.class);
    }

    @Test
    void registersGetOrchestrationPlanArtifactUseCase() {
        assertThat(getOrchestrationPlanArtifactUseCase).isInstanceOf(GetOrchestrationPlanArtifactService.class);
    }

    @Test
    void registersGetOrchestrationPlanFileUseCase() {
        assertThat(getOrchestrationPlanFileUseCase).isInstanceOf(GetOrchestrationPlanFileService.class);
    }

    @Test
    void registersSendChatMessageUseCase() {
        assertThat(sendChatMessageUseCase).isInstanceOf(SendChatMessageService.class);
    }

    @Test
    void registersListChatTaskMessagesUseCase() {
        assertThat(listChatTaskMessagesUseCase).isInstanceOf(ListChatTaskMessagesService.class);
    }

    @Test
    void registersListChatSessionMessagesUseCase() {
        assertThat(listChatSessionMessagesUseCase).isInstanceOf(ListChatSessionMessagesService.class);
    }

    @Test
    void registersReceiveSlackEventUseCase() {
        assertThat(receiveSlackEventUseCase).isInstanceOf(ReceiveSlackEventService.class);
    }

    @Test
    void wiresReceiveSlackEventUseCaseWithChannelMappingPort() {
        assertThat(ReflectionTestUtils.getField(receiveSlackEventUseCase, "loadSlackChannelMappingPort"))
                .isSameAs(loadSlackChannelMappingPort);
        assertThat(ReflectionTestUtils.getField(receiveSlackEventUseCase, "sendSlackReplyPort"))
                .isSameAs(sendSlackReplyPort);
        assertThat(ReflectionTestUtils.getField(receiveSlackEventUseCase, "claimSlackEventPort"))
                .isSameAs(claimSlackEventPort);
    }

    @Test
    void registersCreateSlackChannelMappingUseCase() {
        assertThat(createSlackChannelMappingUseCase).isInstanceOf(CreateSlackChannelMappingService.class);
    }

    @Test
    void registersListSlackChannelMappingsUseCase() {
        assertThat(listSlackChannelMappingsUseCase).isInstanceOf(ListSlackChannelMappingsService.class);
    }

    @Test
    void registersUpdateSlackIntegrationUseCase() {
        assertThat(updateSlackIntegrationUseCase).isInstanceOf(UpdateSlackIntegrationService.class);
    }

    @Test
    void registersDeleteSlackIntegrationUseCase() {
        assertThat(deleteSlackIntegrationUseCase).isInstanceOf(DeleteSlackIntegrationService.class);
    }

    @Test
    void registersCompleteSlackOAuthCallbackUseCase() {
        assertThat(completeSlackOAuthCallbackUseCase).isInstanceOf(CompleteSlackOAuthCallbackService.class);
    }

    @Test
    void registersSlackSignatureVerifier() {
        assertThat(slackSignatureVerifier).isNotNull();
    }
}
