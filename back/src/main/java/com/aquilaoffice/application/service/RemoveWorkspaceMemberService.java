package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberCommand;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberUseCase;
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
 * 워크스페이스 멤버 제거 유스케이스를 수행하는 application service다.
 */
public class RemoveWorkspaceMemberService implements RemoveWorkspaceMemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceMemberPort loadWorkspaceMemberPort;
    private final RemoveWorkspaceMemberPort removeWorkspaceMemberPort;

    public RemoveWorkspaceMemberService(
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
    public void removeWorkspaceMember(RemoveWorkspaceMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());
        List<ListWorkspaceMembersResult> members =
                loadWorkspaceMemberPort.findActiveWorkspaceMembersByWorkspaceId(command.workspaceId());
        ListWorkspaceMembersResult targetMember = findTargetMember(members, command.memberId());
        validateLastAdminIsKept(members, targetMember);

        removeWorkspaceMemberPort.remove(new WorkspaceMemberRemoval(command.workspaceId(), command.memberId()));
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

    private ListWorkspaceMembersResult findTargetMember(List<ListWorkspaceMembersResult> members, Long targetMemberId) {
        return members.stream()
                .filter(member -> member.memberId().equals(targetMemberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("workspace member not found"));
    }

    private void validateLastAdminIsKept(
            List<ListWorkspaceMembersResult> members, ListWorkspaceMembersResult targetMember) {
        if (targetMember.role() != WorkspaceMemberRole.ADMIN) {
            return;
        }
        long adminCount = members.stream()
                .filter(member -> member.role() == WorkspaceMemberRole.ADMIN)
                .count();
        if (adminCount <= 1) {
            throw new IllegalStateException("last admin cannot be removed");
        }
    }
}
