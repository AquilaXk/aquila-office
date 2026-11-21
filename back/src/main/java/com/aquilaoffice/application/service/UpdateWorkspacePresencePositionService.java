package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import java.util.List;
import java.util.Objects;

/**
 * 요청자의 워크스페이스 멤버십을 확인한 뒤 presence 위치 상태를 갱신하는 application service다.
 */
public class UpdateWorkspacePresencePositionService implements UpdateWorkspacePresencePositionUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final WorkspacePresenceRegistry workspacePresenceRegistry;

    public UpdateWorkspacePresencePositionService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            WorkspacePresenceRegistry workspacePresenceRegistry) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
        this.workspacePresenceRegistry =
                Objects.requireNonNull(workspacePresenceRegistry, "workspacePresenceRegistry must not be null");
    }

    @Override
    public void updatePosition(UpdateWorkspacePresencePositionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadMemberPort
                .findByGoogleSub(command.memberGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(command.workspaceId(), requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));

        Long memberId = findRequesterMemberId(command.workspaceId(), requester);
        WorkspacePresenceMessage event =
                workspacePresenceRegistry.updatePosition(
                        command.workspaceId(),
                        memberId,
                        command.position(),
                        command.status());
        broadcast(command.workspaceId(), event);
    }

    private Long findRequesterMemberId(Long workspaceId, Member requester) {
        if (requester.getId() != null) {
            return requester.getId();
        }
        List<ListWorkspaceMembersResult> members =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(workspaceId);

        return members.stream()
                .filter(member -> member.memberGoogleSub().equals(requester.getGoogleSub()))
                .map(ListWorkspaceMembersResult::memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("workspace member not found"));
    }

    private void broadcast(Long workspaceId, WorkspacePresenceMessage message) {
        for (WorkspacePresenceRegistry.WorkspacePresenceConnection connection :
                workspacePresenceRegistry.connectionsInWorkspace(workspaceId)) {
            try {
                connection.sink().send(message);
            } catch (RuntimeException exception) {
                workspacePresenceRegistry.leave(connection.workspaceId(), connection.memberId(), connection.sink());
            }
        }
    }
}
