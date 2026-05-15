package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersUseCase;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.out.AcceptWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.service.AcceptWorkspaceInviteService;
import com.aquilaoffice.application.service.ListWorkspaceInvitesService;
import com.aquilaoffice.application.service.ListWorkspaceMembersService;
import com.aquilaoffice.application.service.RevokeWorkspaceInviteService;
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
    private GenerateWorkspaceInviteTokenPort generateWorkspaceInviteTokenPort;

    @Autowired
    private AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase;

    @Autowired
    private ListWorkspaceMembersUseCase listWorkspaceMembersUseCase;

    @Autowired
    private ListWorkspaceInvitesUseCase listWorkspaceInvitesUseCase;

    @Autowired
    private RevokeWorkspaceInviteUseCase revokeWorkspaceInviteUseCase;

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
}
