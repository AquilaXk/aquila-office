package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspace;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 워크스페이스 생성 유스케이스를 수행하는 application service다.
 *
 * <p>Spring 의존성을 두지 않고 port와 순수 도메인 모델만 사용해 헥사고날 경계를 유지한다.</p>
 */
public class CreateWorkspaceService implements CreateWorkspaceUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveWorkspacePort saveWorkspacePort;
    private final Clock clock;

    public CreateWorkspaceService(LoadMemberPort loadMemberPort, SaveWorkspacePort saveWorkspacePort, Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.saveWorkspacePort = Objects.requireNonNull(saveWorkspacePort, "saveWorkspacePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public CreateWorkspaceResult createWorkspace(CreateWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member creator = loadMemberPort
                .findByGoogleSub(command.creatorGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("creator member not found"));
        Workspace workspace = Workspace.create(command.name(), command.description(), creator);
        // 생성자는 워크스페이스를 만든 즉시 ADMIN 멤버로 함께 저장되어야 한다.
        WorkspaceMember workspaceMember = WorkspaceMember.create(
                workspace, creator, WorkspaceMemberRole.ADMIN, LocalDateTime.now(clock));
        SavedWorkspace savedWorkspace = saveWorkspacePort.save(workspace, workspaceMember);

        return new CreateWorkspaceResult(
                savedWorkspace.workspaceId(),
                workspace.getName(),
                workspace.getDescription(),
                creator.getGoogleSub(),
                workspaceMember.getRole(),
                workspaceMember.getJoinedAt());
    }
}
