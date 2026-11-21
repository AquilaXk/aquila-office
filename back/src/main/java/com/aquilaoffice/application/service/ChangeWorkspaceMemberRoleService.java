package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleCommand;
import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceMemberRoleChange;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버 역할 변경 유스케이스를 수행하는 application service다.
 */
public class ChangeWorkspaceMemberRoleService implements ChangeWorkspaceMemberRoleUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final ChangeWorkspaceMemberRolePort changeWorkspaceMemberRolePort;

    public ChangeWorkspaceMemberRoleService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            ChangeWorkspaceMemberRolePort changeWorkspaceMemberRolePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
        this.changeWorkspaceMemberRolePort = Objects.requireNonNull(
                changeWorkspaceMemberRolePort, "changeWorkspaceMemberRolePort must not be null");
    }

    @Override
    public void changeWorkspaceMemberRole(ChangeWorkspaceMemberRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());
        List<ListWorkspaceMembersResult> members =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(command.workspaceId());
        validateRoleChangeTarget(members, command.memberId(), command.role());

        changeWorkspaceMemberRolePort.changeRole(
                new WorkspaceMemberRoleChange(command.workspaceId(), command.memberId(), command.role()));
    }

    private void requireAdminMembership(Long workspaceId, String requesterGoogleSub) {
        Member requester = loadMemberPort
                .findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        GetWorkspaceDetailResult workspaceDetail = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private void validateRoleChangeTarget(
            List<ListWorkspaceMembersResult> members,
            Long targetMemberId,
            WorkspaceMemberRole nextRole) {
        ListWorkspaceMembersResult targetMember = null;
        int adminCount = 0;
        for (ListWorkspaceMembersResult member : members) {
            if (member.role() == WorkspaceMemberRole.ADMIN) {
                adminCount++;
            }
            if (member.memberId().equals(targetMemberId)) {
                targetMember = member;
            }
        }
        if (targetMember == null) {
            throw new IllegalArgumentException("workspace member not found");
        }
        if (targetMember.role() == WorkspaceMemberRole.ADMIN
                && nextRole != WorkspaceMemberRole.ADMIN
                && adminCount <= 1) {
            throw new IllegalStateException("last admin cannot be changed");
        }
    }
}
