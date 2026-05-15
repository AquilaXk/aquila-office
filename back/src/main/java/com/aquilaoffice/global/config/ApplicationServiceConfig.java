package com.aquilaoffice.global.config;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.ChangeWorkspaceMemberRoleService;
import com.aquilaoffice.application.service.CreateWorkspaceService;
import com.aquilaoffice.application.service.CreateWorkspaceInviteService;
import com.aquilaoffice.application.service.DeleteWorkspaceService;
import com.aquilaoffice.application.service.GetWorkspaceDetailService;
import com.aquilaoffice.application.service.GetWorkspaceInviteService;
import com.aquilaoffice.application.service.ListWorkspaceMembersService;
import com.aquilaoffice.application.service.ListWorkspaceInvitesService;
import com.aquilaoffice.application.service.ListWorkspacesService;
import com.aquilaoffice.application.service.RemoveWorkspaceMemberService;
import com.aquilaoffice.application.service.RevokeWorkspaceInviteService;
import com.aquilaoffice.application.service.UpdateWorkspaceService;
import java.time.Clock;
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
}
