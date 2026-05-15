package com.aquilaoffice.global.config;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.CreateWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.CreateWorkspaceService;
import com.aquilaoffice.application.service.CreateWorkspaceInviteService;
import com.aquilaoffice.application.service.GetWorkspaceDetailService;
import com.aquilaoffice.application.service.GetWorkspaceInviteService;
import com.aquilaoffice.application.service.ListWorkspacesService;
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
}
