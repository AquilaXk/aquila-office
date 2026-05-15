package com.aquilaoffice.global.config;

import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListWorkspacesUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.service.CreateWorkspaceService;
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
    public ListWorkspacesUseCase listWorkspacesUseCase(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort) {
        return new ListWorkspacesService(loadMemberPort, loadWorkspacePort);
    }
}
