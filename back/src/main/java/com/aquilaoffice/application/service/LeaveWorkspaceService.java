package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.LeaveWorkspaceCommand;
import com.aquilaoffice.application.port.in.LeaveWorkspaceUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.WorkspaceMemberRemoval;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.List;
import java.util.Objects;

/**
 * 요청자 본인의 워크스페이스 멤버십을 제거하는 application service다.
 */
public class LeaveWorkspaceService implements LeaveWorkspaceUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final RemoveWorkspaceMemberPort removeWorkspaceMemberPort;

    public LeaveWorkspaceService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceMemberPort loadWorkspaceMemberPort,
            RemoveWorkspaceMemberPort removeWorkspaceMemberPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceMemberPort =
                Objects.requireNonNull(loadWorkspaceMemberPort, "loadWorkspaceMemberPort must not be null");
        this.removeWorkspaceMemberPort =
                Objects.requireNonNull(removeWorkspaceMemberPort, "removeWorkspaceMemberPort must not be null");
    }

    @Override
    public void leaveWorkspace(LeaveWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = requireJoinedWorkspace(command.workspaceId(), command.requesterGoogleSub());
        List<ListWorkspaceMembersResult> members =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(command.workspaceId());
        ListWorkspaceMembersResult requesterMember = findRequesterMember(members, requesterGoogleSub);
        validateLastAdminIsKept(members, requesterMember);

        removeWorkspaceMemberPort.remove(
                new WorkspaceMemberRemoval(command.workspaceId(), requesterMember.memberId()));
    }

    private String requireJoinedWorkspace(Long workspaceId, String requesterGoogleSub) {
        Member requester = loadMemberPort
                .findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requester.getGoogleSub())
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
        return requester.getGoogleSub();
    }

    private ListWorkspaceMembersResult findRequesterMember(
            List<ListWorkspaceMembersResult> members, String requesterGoogleSub) {
        return members.stream()
                .filter(member -> member.memberGoogleSub().equals(requesterGoogleSub))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("workspace member not found"));
    }

    private void validateLastAdminIsKept(
            List<ListWorkspaceMembersResult> members, ListWorkspaceMembersResult requesterMember) {
        if (requesterMember.role() != WorkspaceMemberRole.ADMIN) {
            return;
        }
        long adminCount = members.stream()
                .filter(member -> member.role() == WorkspaceMemberRole.ADMIN)
                .count();
        if (adminCount <= 1) {
            throw new IllegalStateException("last admin cannot leave workspace");
        }
    }
}
